package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.StringUtil;
import com.google.common.collect.Lists;
import dev.langchain4j.model.input.PromptTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文本输入输出 — 使用 LangChain4j PromptTemplate 渲染变量
 */
@Component("textInOut")
public class TextInOut extends BaseConsumer {

    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        JSONObject inputs = workflowRunResult.getInputs();
        String text = StringUtil.trim(DataParametersUtil.getValueByTemplate("text", inputs, String.class, StringUtils.EMPTY));
        String textDisplayName = DataParametersUtil.getDisplayNameByTemplate("text", inputs);
        if (StringUtils.isBlank(textDisplayName)) {
            textDisplayName = "text";
        }

        Response<List<JSONObject>> errorResponse = Response.error(null);
        if (StringUtils.isBlank(text)) {
            errorResponse.getMessage().setMessage(
                String.format("（%s）组件%s输入为空，请输入内容", workflowRunResult.getNodeName(), textDisplayName));
            return errorResponse;
        }

        // 用 LangChain4j PromptTemplate 渲染 {{变量}} 占位符
        String rendered = renderWithPromptTemplate(text, inputs);

        JSONObject output = WorkflowUtil.createOutput(WorkflowJSONKey.output.name(), WorkflowJSONKey.str.name(), rendered);
        return Response.ok(Lists.newArrayList(output));
    }

    /**
     * 用 LangChain4j PromptTemplate 渲染模板变量
     * LangChain4j 使用 {{var}} 语法，与工作流模板语法一致
     */
    private String renderWithPromptTemplate(String text, JSONObject inputs) {
        try {
            // 收集所有模板变量的值
            Map<String, Object> vars = new HashMap<>();
            List<String> parameterKeys = WorkflowUtil.getPromptParameterKeys(text);
            for (String key : parameterKeys) {
                String actualKey = key.replace("{{", "").replace("}}", "");
                Object value = DataParametersUtil.getValueByTemplate(actualKey, inputs, Object.class, null);
                if (value != null) {
                    vars.put(actualKey, value.toString());
                }
            }

            if (vars.isEmpty()) {
                // 没有变量，直接返回原文本（先用原有逻辑兜底）
                return WorkflowUtil.replacePrompt(inputs, text);
            }

            PromptTemplate template = PromptTemplate.from(text);
            return template.apply(vars).text();
        } catch (Exception e) {
            // PromptTemplate 解析失败时降级到原有逻辑
            LOG.warn("LangChain4j PromptTemplate 渲染失败，降级到原有逻辑: {}", e.getMessage());
            return WorkflowUtil.replacePrompt(inputs, text);
        }
    }

    @Override
    public Boolean available() {
        return Boolean.TRUE;
    }
}
