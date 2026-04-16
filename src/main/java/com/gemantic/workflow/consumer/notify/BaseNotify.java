package com.gemantic.workflow.consumer.notify;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.gemantic.gpt.client.NotifyLogClient;
import com.gemantic.gpt.model.NotifyLog;
import com.gemantic.gpt.support.notify.NotifyPlan;
import com.gemantic.gpt.util.NotifyUtil;
import com.gemantic.gpt.util.TaskUtil;
import com.gemantic.redisearch.client.RedisZsetClient;
import com.gemantic.redisearch.support.ZSetInfo;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.MsgUtil;
import com.gemantic.springcloud.utils.RandomUtil;
import com.gemantic.tools.utils.OkHttpUtil;
import com.google.common.collect.Lists;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.NoRepositoryBean;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@NoRepositoryBean
public class BaseNotify implements Runnable {

    protected Logger LOG = LoggerFactory.getLogger(getClass().getSimpleName());

    @Resource
    protected RedisZsetClient redisZsetClient;

    @Resource
    protected NotifyLogClient notifyLogClient;

    @Value("${spring.application.name:llm-workflow-service}")
    protected String serviceName = "llm-workflow-service";

    @Value("${notify.interval:2000}")
    protected Long notifyInterval;

    protected Date startDate;

    @Override
    public void run() {
        String id = StringUtils.EMPTY;
        while (Boolean.TRUE) {
            try {
                List<ZSetInfo> rangeWithScores = redisZsetClient.find(getZsetName(), "ASC", 1, 1).getData();
                if (CollectionUtils.isEmpty(rangeWithScores)) {
                    return;
                }
                List<String> notifyIds = rangeWithScores.stream().map(r -> r.getValue()).distinct().collect(Collectors.toList());
                if (CollectionUtils.isEmpty(notifyIds)) {
                    return;
                }
                id = notifyIds.get(0);
                if (StringUtils.isBlank(id)) {
                    return;
                }
//                LOG.info("{} 接收 notifyId={}", getZsetName(), id);
                NotifyLog notifyLog = notifyLogClient.getById(Long.valueOf(id)).getData();
                if (null == notifyLog) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{} 忽略 notifyId={} 已不存在", getZsetName(), id);
                    }
                    removeZset(notifyIds);
                    continue;
                }
                Long now = NotifyUtil.getPlanAt(DateTime.now().getMillis());
                if (notifyLog.getPlanAt() > now) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{} 通知时间未到 notifyId={} planAt={} now={}", getZsetName(), id, notifyLog.getStatus());
                    }
                    continue;
                }
                removeZset(notifyIds);
                if (notifyLog.getStatus().intValue() != 0) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{} 忽略 notifyId={} status={}", getZsetName(), id, notifyLog.getStatus());
                    }
                    continue;
                }
                if (notifyLog.getNotifyStatus().intValue() == 1) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{} 忽略 notifyId={} notifyStatus={}", getZsetName(), id, notifyLog.getNotifyStatus());
                    }
                    continue;
                }
                NotifyPlan notifyPlan = NotifyUtil.getNotifyPlan(notifyLog.getPlanAt(), notifyLog.getPlan());
                if (null == notifyPlan) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{} 忽略 notifyId={} planAt={} 不存在", getZsetName(), id, notifyLog.getNotifyStatus());
                    }
                    continue;
                }
                notifyPlan.setStatus(1);
                notifyPlan.setNotifyStatus(0);
                notifyPlan.setNotifyAt(0L);
                notifyPlan.setNotifyError(StringUtils.EMPTY);
                notifyPlan.setDuration(0L);
                notifyPlan.setNotifyStatus(0);

                notifyLog.setStatus(notifyPlan.getStatus());
                notifyLog.setNotifyStatus(notifyPlan.getNotifyStatus());
                notifyLog.setNotifyAt(notifyPlan.getNotifyAt());
                notifyLog.setNotifyError(notifyPlan.getNotifyError());
                notifyLog.setDuration(notifyPlan.getDuration());
                notifyLog.setTimeout(notifyPlan.getTimeout());
                notifyLogClient.save(Lists.newArrayList(notifyLog));
                runNotify(notifyLog);

                notifyPlan.setStatus(notifyLog.getStatus());
                notifyPlan.setNotifyStatus(notifyLog.getNotifyStatus());
                notifyPlan.setNotifyError(notifyLog.getNotifyError());
                notifyPlan.setNotifyAt(notifyLog.getNotifyAt());
                notifyPlan.setDuration(notifyLog.getDuration());

                notifyLog.setPlanAt(0L);
                notifyLog.setNotifiedCount(notifyLog.getPlan().stream().filter(n -> n.getStatus().intValue() == 2).collect(Collectors.toList()).size());
                NotifyPlan nextPlan = null;
                if (notifyPlan.getNotifyStatus().intValue() < 0) {
                    nextPlan = NotifyUtil.getNextNotifyPlan(notifyPlan, notifyLog.getPlan());
                }
                if (null != nextPlan) {
                    notifyLog.setPlanAt(nextPlan.getPlanAt());
                    notifyLog.setTimeout(nextPlan.getTimeout());
                    notifyLog.setStatus(0);
                }
                notifyLogClient.save(Lists.newArrayList(notifyLog));
                if (null != nextPlan) {
                    saveZset(Lists.newArrayList(Pair.of(id, nextPlan.getScore())));
                }
            } catch (Exception e) {
                LOG.error("{} notifyId={} ", id, e);
            }
        }

    }


    public void runNotify(NotifyLog notifyLog) {
        Long start = DateTime.now().getMillis();
        OkHttpClient client = OkHttpUtil.httpClient(15, notifyLog.getTimeout(), TimeUnit.SECONDS);
        try {
            String responseStr = OkHttpUtil.post(client, notifyLog.getUrl(), null, notifyLog.getData(), notifyLog.getHeaders(), null, null, Boolean.TRUE);
            LOG.warn("请求正常 notifyId={} url={} headers={} request={} resonse={}", notifyLog.getId(), notifyLog.getUrl(), notifyLog.getHeaders(), notifyLog.getData(), responseStr);
            notifyLog.setNotifyStatus(-1);
            notifyLog.setNotifyError(StringUtils.EMPTY);
            if (StringUtils.isBlank(responseStr)) {
                notifyLog.setNotifyError("通知调用响应结果为空");
            } else {
                Response<Void> response = null;
                try {
                    response = JSON.parseObject(responseStr, new TypeReference<Response<Void>>() {
                    });
                    if (MsgUtil.isValidMessage(response)) {
                        notifyLog.setNotifyStatus(1);
                    } else {
                        if (null != response.getMessage() && StringUtils.isNotBlank(response.getMessage().getMessage())) {
                            notifyLog.setNotifyError(response.getMessage().getMessage());
                        } else {
                            notifyLog.setNotifyError(String.format("通知调用响应失败原因无法解析:%s", responseStr));
                        }
                    }
                } catch (Exception e) {
                    notifyLog.setNotifyError(String.format("通知调用响应结果无法解析:%s", responseStr));
                }

            }

        } catch (Exception e) {
            notifyLog.setNotifyStatus(-1);
            notifyLog.setNotifyError(e.getMessage());
            LOG.error("请求失败 notifyId={} url={} headers={} request={} {}", notifyLog.getId(), notifyLog.getUrl(), notifyLog.getHeaders(), notifyLog.getData(), e);
        }
        notifyLog.setStatus(2);
        notifyLog.setNotifyAt(DateTime.now().getMillis());
        notifyLog.setDuration(notifyLog.getNotifyAt() - start);
    }

    public String getZsetName() {
        return TaskUtil.getQueueName(serviceName, getClass().getSimpleName());
    }

    public Boolean available() {
        return Boolean.TRUE;
    }

    public Long getIntervalMilliseconds() {
        return notifyInterval + RandomUtil.generateInRange(0,1000);
    }


    public Long removeZset(List<String> ids) throws Exception {
        if (CollectionUtils.isEmpty(ids)) {
            return 0L;
        }
        Long result = redisZsetClient.delete(getZsetName(), ids).getData();
        LOG.info("通知任务移除有序集合 zset_name={} id={} result={}", getZsetName(), ids, result);
        return result;
    }


    public Long saveZset(List<Pair<String, Double>> notifyIdScoreList) throws Exception {
        if (CollectionUtils.isEmpty(notifyIdScoreList)) {
            return 0L;
        }
        List<ZSetInfo> zSetInfos = Lists.newArrayList();
        for (Pair<String, Double> notifyIdScore : notifyIdScoreList) {
            ZSetInfo zSetInfo = new ZSetInfo();
            zSetInfo.setScore(notifyIdScore.getRight());
            zSetInfo.setValue(notifyIdScore.getLeft());
            zSetInfos.add(zSetInfo);
        }
        Long result = redisZsetClient.save(getZsetName(), zSetInfos).getData();
        if (LOG.isDebugEnabled()) {
            LOG.debug("通知任务保存有序集合 zset_name={} result={} data={}", getZsetName(), result, JSON.toJSONString(zSetInfos));
        }
        return result;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }
}

