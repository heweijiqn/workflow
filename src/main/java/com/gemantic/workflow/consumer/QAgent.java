package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.client.BaseOpenChatClient;
import com.gemantic.gpt.client.OpenQagentClient;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.google.common.base.MoreObjects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;


@Component("qAgent")
public class QAgent extends BaseQChat {

    @Resource
    private OpenQagentClient openQagentClient;

    @Value("${qdata.interval:1000}")
    private Long intervalMilliseconds;

    @Value("${CONSUMER_Q_AGENT_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize(){
        return MoreObjects.firstNonNull(consumerBatchSize,super.getConsumerBatchSize());
    }


    @Override
    protected JSONObject getSendInfo(WorkflowRunResult workflowRunResult, String searchText) throws Exception {
        JSONObject result = new JSONObject();
        return result;
    }

    @Override
    protected BaseOpenChatClient getBaseChatClient() throws Exception {
        return this.openQagentClient;
    }

    @Override
    public Long getIntervalMilliseconds() {
        return intervalMilliseconds;
    }
}
