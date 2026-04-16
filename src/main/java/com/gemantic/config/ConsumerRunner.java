package com.gemantic.config;

import com.gemantic.springcloud.utils.SpringBeanUtil;
import com.gemantic.workflow.consumer.BaseConsumer;
import com.gemantic.workflow.consumer.notify.BaseNotify;
import com.gemantic.workflow.job.QueueTimeoutJob;
import com.gemantic.workflow.job.RunTimeoutJob;
import com.gemantic.workflow.job.SubWorkflowStatusJob;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.MapUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Component
public class ConsumerRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumerRunner.class);

    @Value("${consumer.enable:true}")
    private Boolean consumerEnable;

    @Resource
    private RunTimeoutJob runTimeoutJob;

    @Resource
    private SubWorkflowStatusJob subWorkflowStatusJob;

    @Resource
    private QueueTimeoutJob queueTimeoutJob;

    @Resource
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    public static final Map<String, ScheduledFuture<?>> SCHEDULED_FUTURES = Maps.newHashMap();

//    @Value("#{${consumer.count}}")
//    private Map<String, Integer> consumerCount;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!consumerEnable) {
            return;
        }
        Map<String, BaseConsumer> consumerMap = SpringBeanUtil.getBeansOfType(BaseConsumer.class);
        if (MapUtils.isEmpty(consumerMap)) {
            LOG.warn("没有消费者配置");
            return;
        }
//        LOG.warn("消费者启动个数={}", consumerCount);
        int i = 2;
        for (Map.Entry<String, BaseConsumer> consumerEntry : consumerMap.entrySet()) {
            if (!consumerEntry.getValue().available()) {
                continue;
            }
//            Integer threadPoolSize = MapUtils.getInteger(consumerCount, consumerEntry.getValue().getClass().getSimpleName(), 1);
//            for (int j = 0; j < threadPoolSize; j++) {
                BaseConsumer consumer = consumerEntry.getValue();
                Date startDate = new Date(DateTime.now().plusSeconds(i).getMillis());
                Long intervalMilliseconds = consumer.getIntervalMilliseconds();
                consumer.setStartDate(startDate);
                ScheduledFuture<?> scheduledFuture = threadPoolTaskScheduler.scheduleWithFixedDelay(consumer, startDate.toInstant(), Duration.ofMillis(intervalMilliseconds));
                SCHEDULED_FUTURES.put(consumerEntry.getKey(),scheduledFuture);
                LOG.warn("初始化组件消费者{}({}) 开始运行时间={} 间隔={}毫秒", consumer.getClass().getSimpleName(), consumer.getQueueName(), new DateTime(startDate.getTime()).toString("MM-dd HH:mm:ss"), intervalMilliseconds);
                i += 2;
//            }
        }

        Map<String, BaseNotify> notifyConsumerMap = SpringBeanUtil.getBeansOfType(BaseNotify.class);
        for (Map.Entry<String, BaseNotify> consumerEntry : notifyConsumerMap.entrySet()) {
            if (!consumerEntry.getValue().available()) {
                continue;
            }
            Date startDate = new Date(DateTime.now().plusSeconds(i).getMillis());
            BaseNotify consumer = consumerEntry.getValue();
            consumer.setStartDate(startDate);
            Long intervalMilliseconds = consumer.getIntervalMilliseconds();
            ScheduledFuture<?> scheduledFuture = threadPoolTaskScheduler.scheduleWithFixedDelay(consumer, startDate.toInstant(), Duration.ofMillis(intervalMilliseconds));
            SCHEDULED_FUTURES.put(consumerEntry.getKey(),scheduledFuture);
            LOG.warn("初始化通知消费者{}({}) 开始运行时间={} 间隔={}毫秒", consumer.getClass().getSimpleName(), consumer.getZsetName(), new DateTime(startDate.getTime()).toString("MM-dd HH:mm:ss"), intervalMilliseconds);
            i += 2;
        }

        Date startDate = new Date(DateTime.now().plusMinutes(1).getMillis());
        Long intervalMilliseconds = runTimeoutJob.getIntervalMilliseconds();
        runTimeoutJob.setStartDate(startDate);
        ScheduledFuture<?> scheduledFuture = threadPoolTaskScheduler.scheduleWithFixedDelay(runTimeoutJob, startDate.toInstant(), Duration.ofMillis(intervalMilliseconds));
        SCHEDULED_FUTURES.put(runTimeoutJob.getClass().getSimpleName(),scheduledFuture);
        LOG.warn("初始化运行超时任务 开始运行时间={} 间隔={}毫秒", new DateTime(startDate.getTime()).toString("MM-dd HH:mm:ss"), intervalMilliseconds);

//        startDate = new Date(DateTime.now().plusMinutes(1).getMillis());
//        intervalMilliseconds = queueTimeoutJob.getIntervalMilliseconds();
//        queueTimeoutJob.setStartDate(startDate);
//        ScheduledFuture<?> queueTimeoutFuture = threadPoolTaskScheduler.scheduleWithFixedDelay(queueTimeoutJob, startDate.toInstant(), Duration.ofMillis(intervalMilliseconds));
//        SCHEDULED_FUTURES.put(queueTimeoutJob.getClass().getSimpleName(),queueTimeoutFuture);
//        LOG.warn("初始化队列超时任务 开始运行时间={} 间隔={}毫秒", new DateTime(startDate.getTime()).toString("MM-dd HH:mm:ss"), intervalMilliseconds);

        Date subWorkflowStartDate = new Date(DateTime.now().plusMinutes(1).getMillis());
        intervalMilliseconds = subWorkflowStatusJob.getIntervalMilliseconds();
        subWorkflowStatusJob.setStartDate(subWorkflowStartDate);
        ScheduledFuture<?> subWorkflowStatusScheduledFuture =threadPoolTaskScheduler.scheduleWithFixedDelay(subWorkflowStatusJob, subWorkflowStartDate.toInstant(), Duration.ofMillis(intervalMilliseconds));
        SCHEDULED_FUTURES.put(subWorkflowStatusJob.getClass().getSimpleName(),subWorkflowStatusScheduledFuture);
        LOG.warn("初始化子工作流运行状态异常修复任务 开始运行时间={} 间隔={}毫秒", new DateTime(subWorkflowStartDate.getTime()).toString("MM-dd HH:mm:ss"), intervalMilliseconds);

    }




}
