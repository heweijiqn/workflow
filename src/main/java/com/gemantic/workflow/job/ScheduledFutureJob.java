package com.gemantic.workflow.job;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.config.ConsumerRunner;
import com.gemantic.springcloud.utils.SpringBeanUtil;
import com.gemantic.workflow.consumer.BaseConsumer;
import com.gemantic.workflow.consumer.notify.BaseNotify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class ScheduledFutureJob {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledFutureJob.class);

    @Resource
    private RunTimeoutJob runTimeoutJob;

    @Resource
    private SubWorkflowStatusJob subWorkflowStatusJob;

    @Resource
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Resource
    private QueueTimeoutJob queueTimeoutJob;

    @Scheduled(cron = "0 0/5 * * * ?")
    public List<String> scan(){
        if(MapUtils.isEmpty(ConsumerRunner.SCHEDULED_FUTURES)){
            return Lists.newArrayList();
        }
        List<String> consumerKeys = new ArrayList<>(ConsumerRunner.SCHEDULED_FUTURES.keySet());
        return create(consumerKeys,Boolean.FALSE);
    }

    public List<String> create(List<String> consumerKeys,Boolean cancelBeforeCreate){
        if(CollectionUtils.isEmpty(consumerKeys)){
            return Lists.newArrayList();
        }
        List<String> result = Lists.newArrayList();
        Map<String, BaseConsumer> consumerMap = SpringBeanUtil.getBeansOfType(BaseConsumer.class);
        Map<String, BaseNotify> notifyMap = SpringBeanUtil.getBeansOfType(BaseNotify.class);

        int i = 2;
        for(String consumerKey: consumerKeys){
            if(cancelBeforeCreate){
                cancel(Lists.newArrayList(consumerKey),Boolean.FALSE);
            }
            ScheduledFuture<?> oldScheduledFuture = ConsumerRunner.SCHEDULED_FUTURES.get(consumerKey);
            if(null != oldScheduledFuture && !oldScheduledFuture.isDone() && !oldScheduledFuture.isCancelled()){
                continue;
            }
            if(consumerKey.equalsIgnoreCase(subWorkflowStatusJob.getClass().getSimpleName())){
                Date startDate = new Date(DateTime.now().plusMinutes(i).getMillis());
                Long intervalMilliseconds = subWorkflowStatusJob.getIntervalMilliseconds();
                subWorkflowStatusJob.setStartDate(startDate);
                ScheduledFuture<?> scheduledFuture = threadPoolTaskScheduler.scheduleWithFixedDelay(subWorkflowStatusJob, startDate.toInstant(), Duration.ofMillis(intervalMilliseconds));
                ConsumerRunner.SCHEDULED_FUTURES.put(subWorkflowStatusJob.getClass().getSimpleName(),scheduledFuture);
                result.add(consumerKey);
                LOG.warn("创建子工作流运行状态异常修复任务 开始运行时间={} 间隔={}毫秒", new DateTime(startDate.getTime()).toString("MM-dd HH:mm:ss"), intervalMilliseconds);
            }else if(consumerKey.equalsIgnoreCase(runTimeoutJob.getClass().getSimpleName())){
                Date startDate = new Date(DateTime.now().plusMinutes(i).getMillis());
                Long intervalMilliseconds = runTimeoutJob.getIntervalMilliseconds();
                runTimeoutJob.setStartDate(startDate);
                ScheduledFuture<?> scheduledFuture = threadPoolTaskScheduler.scheduleWithFixedDelay(runTimeoutJob, startDate.toInstant(), Duration.ofMillis(intervalMilliseconds));
                ConsumerRunner.SCHEDULED_FUTURES.put(runTimeoutJob.getClass().getSimpleName(),scheduledFuture);
                result.add(consumerKey);
                LOG.warn("创建运行超时任务 开始运行时间={} 间隔={}毫秒", new DateTime(startDate.getTime()).toString("MM-dd HH:mm:ss"), intervalMilliseconds);
            }else if(consumerKey.equalsIgnoreCase(queueTimeoutJob.getClass().getSimpleName())){
                Date startDate = new Date(DateTime.now().plusMinutes(i).getMillis());
                Long intervalMilliseconds = queueTimeoutJob.getIntervalMilliseconds();
                queueTimeoutJob.setStartDate(startDate);
                ScheduledFuture<?> scheduledFuture = threadPoolTaskScheduler.scheduleWithFixedDelay(queueTimeoutJob, startDate.toInstant(), Duration.ofMillis(intervalMilliseconds));
                ConsumerRunner.SCHEDULED_FUTURES.put(queueTimeoutJob.getClass().getSimpleName(),scheduledFuture);
                result.add(consumerKey);
                LOG.warn("创建队列超时任务 开始运行时间={} 间隔={}毫秒", new DateTime(startDate.getTime()).toString("MM-dd HH:mm:ss"), intervalMilliseconds);
            }else if(null != notifyMap.get(consumerKey)){
                BaseNotify notify = notifyMap.get(consumerKey);
                if (null == notify) {
                    continue;
                }
                Date startDate = new Date(DateTime.now().plusSeconds(i).getMillis());
                Long intervalMilliseconds = notify.getIntervalMilliseconds();
                notify.setStartDate(startDate);
                ScheduledFuture<?> scheduledFuture = threadPoolTaskScheduler.scheduleWithFixedDelay(notify, startDate, intervalMilliseconds);
                ConsumerRunner.SCHEDULED_FUTURES.put(consumerKey, scheduledFuture);
                result.add(consumerKey);
                LOG.warn("创建消费者{}({}) 开始运行时间={} 间隔={}毫秒", notify.getClass().getSimpleName(), notify.getZsetName(), new DateTime(startDate.getTime()).toString("MM-dd HH:mm:ss"), intervalMilliseconds);

            }else {
                BaseConsumer consumer = consumerMap.get(consumerKey);
                if (null == consumer) {
                    continue;
                }
                Date startDate = new Date(DateTime.now().plusSeconds(i).getMillis());
                Long intervalMilliseconds = consumer.getIntervalMilliseconds();
                consumer.setStartDate(startDate);
                ScheduledFuture<?> scheduledFuture = threadPoolTaskScheduler.scheduleWithFixedDelay(consumer, startDate.toInstant(), Duration.ofMillis(intervalMilliseconds));
                ConsumerRunner.SCHEDULED_FUTURES.put(consumerKey, scheduledFuture);
                result.add(consumerKey);
                LOG.warn("创建消费者{}({}) 开始运行时间={} 间隔={}毫秒", consumer.getClass().getSimpleName(), consumer.getQueueName(), new DateTime(startDate.getTime()).toString("MM-dd HH:mm:ss"), intervalMilliseconds);
            }
            i += 2;
        }
        return result;
    }


    public Map<String,JSONObject> cancel(List<String> consumerKeys,Boolean remove){
        if(CollectionUtils.isEmpty(consumerKeys)){
            return Maps.newHashMap();
        }
        Map<String,JSONObject> result = Maps.newHashMap();
        for(String consumerKey: consumerKeys){
            ScheduledFuture<?> oldScheduledFuture = ConsumerRunner.SCHEDULED_FUTURES.get(consumerKey);
            if(null == oldScheduledFuture){
                result.put(consumerKey,null);
                continue;
            }
            JSONObject status = new JSONObject();
            status.put("isCancelled",oldScheduledFuture.isCancelled());
            status.put("isDone",oldScheduledFuture.isDone());
            if(oldScheduledFuture.isCancelled() || oldScheduledFuture.isDone()){
                result.put(consumerKey,status);
                if(remove){
                    ConsumerRunner.SCHEDULED_FUTURES.remove(consumerKey);
                }
                continue;
            }
            oldScheduledFuture.cancel(Boolean.TRUE);
            LOG.info("停止定时任务{}",consumerKey);
            status.put("isCancelled",oldScheduledFuture.isCancelled());
            status.put("isDone",oldScheduledFuture.isDone());
            result.put(consumerKey,status);
            if(remove){
                ConsumerRunner.SCHEDULED_FUTURES.remove(consumerKey);
            }
        }
        return result;
    }


    public Map<String, JSONObject> getScheduledFutureDoneStatus(){
        if(MapUtils.isEmpty(ConsumerRunner.SCHEDULED_FUTURES)){
            return Maps.newHashMap();
        }
        Map<String,JSONObject> doneStatus = Maps.newHashMap();
        for(Map.Entry<String, ScheduledFuture<?>> scheduledFutureEntry : ConsumerRunner.SCHEDULED_FUTURES.entrySet()){
            JSONObject status = new JSONObject();
            status.put("isDone",scheduledFutureEntry.getValue().isDone());
            status.put("isCancelled",scheduledFutureEntry.getValue().isCancelled());
            doneStatus.put(scheduledFutureEntry.getKey(),status);
        }
        return doneStatus;
    }


}
