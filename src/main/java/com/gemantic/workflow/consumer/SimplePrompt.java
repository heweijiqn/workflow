package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.util.WorkflowUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 普通提示
 */
@Component("simplePrompt")
public class SimplePrompt extends BasePrompt {

    @Value("${simplePrompt.interval:1000}")
    private Long intervalMilliseconds;




    @Override
    public Object convertPrompt(String prompt, JSONObject inputs) throws Exception {
        return WorkflowUtil.replacePrompt(inputs, prompt);
    }

    @Override
    public Long getIntervalMilliseconds() {
        return intervalMilliseconds;
    }
}
