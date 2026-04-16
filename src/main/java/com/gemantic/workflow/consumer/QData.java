package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.dfs.support.DfsInfo;
import com.gemantic.gpt.client.BaseChatClient;
import com.gemantic.gpt.client.BaseOpenChatClient;
import com.gemantic.gpt.client.OpenQdataClient;
import com.gemantic.gpt.client.QdataClient;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.workflow.WorkflowException;
import com.gemantic.gpt.util.WorkflowUtil;
import com.google.common.base.MoreObjects;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;


@Component("qData")
public class QData extends BaseQChat {

    @Resource
    private OpenQdataClient openQdataClient;

    @Value("${qdata.interval:1000}")
    private Long intervalMilliseconds;

    @Value("${CONSUMER_Q_DATA_BATCH_SIZE:}")
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
        return this.openQdataClient;
    }

    @Override
    public Long getIntervalMilliseconds() {
        return intervalMilliseconds;
    }
}
