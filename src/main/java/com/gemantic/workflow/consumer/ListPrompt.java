package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.util.WorkflowUtil;
import com.google.common.base.MoreObjects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 列表循环提示
 */
@Component("listPrompt")
public class ListPrompt extends BasePrompt {

    @Value("${CONSUMER_LIST_PROMPT_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize(){
        return MoreObjects.firstNonNull(consumerBatchSize,super.getConsumerBatchSize());
    }
    @Override
    public Object convertPrompt(String prompt, JSONObject inputs) throws Exception {
        return WorkflowUtil.replacePrompts(inputs, prompt);
    }



}
