package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.client.BaseChatClient;
import com.gemantic.gpt.client.BaseOpenChatClient;
import com.gemantic.gpt.client.OpenQdataDbClient;
import com.gemantic.gpt.client.QdataDbClient;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.workflow.FieldOptions;
import com.gemantic.gpt.util.DataParametersUtil;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ChatDB
 */
@Component("qDb")
public class QDb extends BaseQChat {

    @Resource
    private OpenQdataDbClient openQdataDbClient;

    @Value("${qdata.interval:1000}")
    private Long intervalMilliseconds;

    @Value("${CONSUMER_Q_DB_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize(){
        return MoreObjects.firstNonNull(consumerBatchSize,super.getConsumerBatchSize());
    }

    @Override
    protected JSONObject getSendInfo(WorkflowRunResult workflowRunResult, String searchText) throws Exception {
        List<FieldOptions> database = DataParametersUtil.getValuesByTemplate("database", workflowRunResult.getInputs(), FieldOptions.class, Lists.newArrayList());
        List<FieldOptions> table = DataParametersUtil.getValuesByTemplate("table", workflowRunResult.getInputs(), FieldOptions.class, Lists.newArrayList());
        JSONObject send = new JSONObject();
        if (CollectionUtils.isNotEmpty(database)) {
            send.put("database_id", database.stream().map(d -> d.getValue()).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(table)) {
            send.put("table_id", table.stream().map(d -> d.getValue()).collect(Collectors.toList()));
        }
        return send;
    }

    @Override
    protected BaseOpenChatClient getBaseChatClient() throws Exception {
        return this.openQdataDbClient;
    }

    @Override
    public Long getIntervalMilliseconds() {
        return intervalMilliseconds;
    }
}
