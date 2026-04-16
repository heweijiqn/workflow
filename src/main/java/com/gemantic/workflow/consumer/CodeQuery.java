package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.StringUtil;
import com.gemantic.utils.JsonUtils;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import dev.langchain4j.code.graal.GraalJsCodeExecutionEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.gemantic.gpt.util.WorkflowUtil.getNotEmptyInputsByParameterKeys;

/**
 * CodeQuery - 使用 LangChain4j GraalVM JS 沙盒执行脚本
 * 不再依赖内部 ApiRunClient
 *
 * 注意：原 java_template 字段现在接受 JavaScript 脚本
 * 脚本中可通过 inputs 对象访问上游输入变量，最终 return 结果
 */
@Slf4j
@Component("codeQuery")
public class CodeQuery extends BaseConsumer {

    private static final String ARRAY_TYPE = "array";

    // GraalJsCodeExecutionEngine 是线程安全的，单例复用
    private static final GraalJsCodeExecutionEngine CODE_ENGINE = new GraalJsCodeExecutionEngine();

    @Value("${CONSUMER_CODE_QUERY_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize() {
        return MoreObjects.firstNonNull(consumerBatchSize, super.getConsumerBatchSize());
    }

    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        JSONObject inputs = workflowRunResult.getInputs();

        String script = StringUtil.trim(DataParametersUtil.getValueByTemplate("java_template", inputs, String.class, StringUtils.EMPTY));
        String scriptDisplayName = DataParametersUtil.getDisplayNameByTemplate("java_template", inputs);
        if (StringUtils.isBlank(scriptDisplayName)) {
            scriptDisplayName = "脚本";
        }

        Response<List<JSONObject>> errorResponse = Response.error(null);
        if (StringUtils.isBlank(script)) {
            errorResponse.getMessage().setMessage(
                String.format("（%s）组件%s输入为空，请检查连接是否正确", workflowRunResult.getNodeName(), scriptDisplayName));
            return errorResponse;
        }

        // 收集上游输入变量，注入到脚本上下文
        List<String> parameterKeys = getIncludedParameterKeys(inputs);
        JSONObject inputValues = getNotEmptyInputsByParameterKeys(inputs, parameterKeys);

        // 构建注入脚本：把所有输入变量序列化为 JS 变量
        StringBuilder scriptWithContext = new StringBuilder();
        for (String key : inputValues.keySet()) {
            Object value = inputValues.getJSONObject(key).get("value");
            String valueJson = value == null ? "null" : new String(com.alibaba.fastjson2.JSON.toJSONBytes(value));
            scriptWithContext.append("var ").append(key).append(" = ").append(valueJson).append(";\n");
        }
        scriptWithContext.append(script);

        try {
            String result = CODE_ENGINE.execute(scriptWithContext.toString());
            log.info("（{}）组件脚本执行完成 result={}", workflowRunResult.getNodeName(), result);
            return bindFlowData(workflowRunResult, result);
        } catch (Exception e) {
            log.error("（{}）组件脚本执行失败", workflowRunResult.getNodeName(), e);
            errorResponse.getMessage().setMessage(
                String.format("（%s）组件脚本执行失败: %s", workflowRunResult.getNodeName(), e.getMessage()));
            return errorResponse;
        }
    }

    private Response<List<JSONObject>> bindFlowData(WorkflowRunResult workflowRunResult, String result) {
        Response<List<JSONObject>> errorResponse = Response.error(null);
        if (StringUtils.isBlank(result) || "[]".equals(result.trim())) {
            errorResponse.getMessage().setMessage(
                String.format("（%s）组件输出结果为空", workflowRunResult.getNodeName()));
            return errorResponse;
        }

        List<JSONObject> outputs = Lists.newArrayList();
        String trimmed = result.trim();

        try {
            if (trimmed.startsWith("[")) {
                JSONArray array = JsonUtils.parseArrayWithArm(trimmed);
                if (CollectionUtils.isEmpty(array)) {
                    errorResponse.getMessage().setMessage(
                        String.format("（%s）组件输出结果为空", workflowRunResult.getNodeName()));
                    return errorResponse;
                }
                outputs.add(WorkflowUtil.createOutput(WorkflowJSONKey.output_chart.name(), ARRAY_TYPE, array));
                outputs.add(WorkflowUtil.createOutput("output_json_schema", WorkflowJSONKey.dict.name(),
                    JsonUtils.generateJsonSchema(array, "")));
            } else if (trimmed.startsWith("{")) {
                JSONObject obj = JsonUtils.parseJsonObjectWithArm(trimmed);
                outputs.add(WorkflowUtil.createOutput(WorkflowJSONKey.output_chart.name(), WorkflowJSONKey.dict.name(), obj));
            } else {
                outputs.add(WorkflowUtil.createOutput(WorkflowJSONKey.output_chart.name(), WorkflowJSONKey.str.name(), trimmed));
            }
        } catch (Exception e) {
            log.warn("（{}）组件结果解析失败，作为字符串输出", workflowRunResult.getNodeName(), e);
            outputs.add(WorkflowUtil.createOutput(WorkflowJSONKey.output_chart.name(), WorkflowJSONKey.str.name(), trimmed));
        }

        return Response.ok(outputs);
    }

    public static List<String> getIncludedParameterKeys(JSONObject inputs) {
        List<String> keys = Lists.newArrayList();
        if (inputs == null) return keys;
        Set<String> excludeKeys = new HashSet<>();
        excludeKeys.add("ignore_error");
        excludeKeys.add("java_template");
        excludeKeys.add("output_standard_chart");
        for (String key : inputs.keySet()) {
            if (!excludeKeys.contains(key)) keys.add(key);
        }
        return keys;
    }
}
