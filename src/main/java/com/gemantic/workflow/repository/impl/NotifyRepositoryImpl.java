package com.gemantic.workflow.repository.impl;

import com.gemantic.db.util.DBUtil;
import com.gemantic.gpt.client.NotifyLogClient;
import com.gemantic.gpt.model.NotifyLog;
import com.gemantic.gpt.support.notify.NotifyPlan;
import com.gemantic.gpt.util.NotifyUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.ConvertUtil;
import com.gemantic.workflow.consumer.notify.CommonNotify;
import com.gemantic.workflow.repository.NotifyRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class NotifyRepositoryImpl implements NotifyRepository {

    private static final Logger LOG = LoggerFactory.getLogger(NotifyRepositoryImpl.class);

    @Resource
    private NotifyLogClient notifyLogClient;

    @Resource
    private CommonNotify commonNotify;

    @Override
    public Response<List<NotifyLog>> push(List<NotifyLog> data) throws Exception {
        Response<List<NotifyLog>> error = Response.error(Lists.newArrayList());
        data = data.stream().filter(e -> StringUtils.isNotBlank(e.getChannel())
                && StringUtils.isNotBlank(e.getUrl())
                && StringUtils.isNotBlank(e.getRefererId())
                && StringUtils.isNotBlank(e.getMethod())
                && null != e.getAppId()
                && StringUtils.isNotBlank(e.getType()) && MapUtils.isNotEmpty(e.getData())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(data)) {
            error.getMessage().setMessage("channel,url,method,appId,type,data,refererId不能为空");
            return error;
        }
        Map<String, List<NotifyLog>> notifyMap = data.stream().collect(Collectors.groupingBy(k -> String.join("_", k.getChannel(), String.valueOf(k.getAppId()), k.getType())));
        List<NotifyLog> result = Lists.newArrayList();
        for (Map.Entry<String, List<NotifyLog>> entry : notifyMap.entrySet()) {
            List<NotifyLog> notifyLogs = entry.getValue();
            String channel = notifyLogs.get(0).getChannel();
            String appId = String.valueOf(notifyLogs.get(0).getAppId());
            String type = String.valueOf(notifyLogs.get(0).getType());
            notifyLogs.forEach(NotifyLog::initNotifyKey);
            notifyLogs = notifyLogs.stream().collect(Collectors.toMap(NotifyLog::getNotifyKey, Function.identity(), (k1, k2) -> k2)).values().stream().collect(Collectors.toList());
            List<List<NotifyLog>> batchNotifyLog = ConvertUtil.convert2BatchList(notifyLogs, 30);
            for (List<NotifyLog> batch : batchNotifyLog) {
                Map<String, String> params = Maps.newHashMap();
                params.put("channel", channel);
                params.put("appId", appId);
                params.put("type", type);
                params.put("notifyKey", batch.stream().map(NotifyLog::getNotifyKey).collect(Collectors.joining(",")));
                notifyLogClient.delete(params);
                for (NotifyLog notifyLog : batch) {
                    List<NotifyPlan> plans = NotifyUtil.getNotifyPlan(notifyLog.getPlan());
                    notifyLog.setPlan(plans);
                    notifyLog.setNotifyZset(commonNotify.getZsetName());
                    notifyLog.setPlanAt(plans.getFirst().getPlanAt());
                    notifyLog.setTimeout(plans.getFirst().getTimeout());
                }
                List<Long> ids = notifyLogClient.save(batch).getData();
                DBUtil.fillModelId(ids, batch);
                result.addAll(batch);
                List<Pair<String, Double>> notifyIdScoreList = batch.stream().map(b -> Pair.of(String.valueOf(b.getId()), b.getPlan().get(0).getScore())).collect(Collectors.toList());
                commonNotify.saveZset(notifyIdScoreList);
            }
        }
        return Response.ok(result);
    }
}
