package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.client.BaseChatClient;
import com.gemantic.gpt.client.BaseOpenChatClient;
import com.gemantic.gpt.client.OpenQdataDocClient;
import com.gemantic.gpt.client.QdataDocClient;
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


@Component("qDoc")
public class QDoc extends BaseQChat {

    @Resource
    private OpenQdataDocClient openQdataDocClient;

    @Value("${qdata.interval:1000}")
    private Long intervalMilliseconds;

    @Value("${CONSUMER_Q_DOC_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize(){
        return MoreObjects.firstNonNull(consumerBatchSize,super.getConsumerBatchSize());
    }

    @Override
    protected JSONObject getSendInfo(WorkflowRunResult workflowRunResult, String searchText) throws Exception {
        List<FieldOptions> directories = DataParametersUtil.getValuesByTemplate("directories", workflowRunResult.getInputs(), FieldOptions.class, Lists.newArrayList());
        JSONObject send = new JSONObject();
        if (CollectionUtils.isNotEmpty(directories)) {
            send.put("doc_directory_id", directories.stream().map(d -> d.getValue()).collect(Collectors.toList()));
        }
        return send;
    }

    @Override
    protected BaseOpenChatClient getBaseChatClient() throws Exception {
        return this.openQdataDocClient;
    }

    @Override
    public Long getIntervalMilliseconds() {
        return intervalMilliseconds;
    }
}
