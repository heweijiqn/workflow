package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSON;
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
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ApiRequest - 直接从 inputs 读取 url/method/headers/body，用 OkHttp 发请求
 * 不再依赖内部 ApiRunClient
 */
@Slf4j
@Component("apiRequest")
public class ApiRequest extends BaseConsumer {

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    private static final String ARRAY_TYPE = "array";

    @Resource(name = "apiRequestExecutorService")
    private ExecutorService apiRequestExecutorService;

    @Override
    protected ExecutorService getExecutorService() {
        return apiRequestExecutorService != null ? apiRequestExecutorService : super.getExecutorService();
    }

    @Value("${CONSUMER_API_REQUEST_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize() {
        return MoreObjects.firstNonNull(consumerBatchSize, super.getConsumerBatchSize());
    }

    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        JSONObject inputs = workflowRunResult.getInputs();

        // 从 inputs 读取 HTTP 请求参数
        String url = StringUtil.trim(DataParametersUtil.getValueByTemplate("url", inputs, String.class, StringUtils.EMPTY));
        if (StringUtils.isBlank(url)) {
            // 兼容旧版 api_id 字段：尝试从 api_id.url 读取
            JSONObject apiId = DataParametersUtil.getValueByTemplate("api_id", inputs, JSONObject.class, null);
            if (apiId != null) {
                url = StringUtil.trim(apiId.getString("url"));
            }
        }

        Response<List<JSONObject>> errorResponse = Response.error(null);
        if (StringUtils.isBlank(url)) {
            errorResponse.getMessage().setMessage(
                String.format("（%s）组件url输入为空，请配置请求地址", workflowRunResult.getNodeName()));
            return errorResponse;
        }

        // 替换 url 中的模板变量
        url = WorkflowUtil.replacePrompt(inputs, url);

        String method = StringUtil.trim(DataParametersUtil.getValueByTemplate("method", inputs, String.class, "GET")).toUpperCase();
        JSONObject headersParam = DataParametersUtil.getValueByTemplate("headers", inputs, JSONObject.class, new JSONObject());
        String bodyStr = StringUtil.trim(DataParametersUtil.getValueByTemplate("body", inputs, String.class, StringUtils.EMPTY));
        if (StringUtils.isNotBlank(bodyStr)) {
            bodyStr = WorkflowUtil.replacePrompt(inputs, bodyStr);
        }

        // 替换 parameters_mapping 中的变量到 url query 或 body
        List<JSONObject> parametersMapping = DataParametersUtil.getValuesByTemplate(
            WorkflowJSONKey.parameters_mapping.name(), inputs, JSONObject.class, Lists.newArrayList());
        if (CollectionUtils.isNotEmpty(parametersMapping)) {
            StringBuilder queryBuilder = new StringBuilder();
            for (JSONObject mapping : parametersMapping) {
                String source = mapping.getString("source");
                String target = mapping.getString("target");
                if (StringUtils.isBlank(source) || StringUtils.isBlank(target)) continue;
                Object value = DataParametersUtil.getValueByTemplate(source, inputs, Object.class, null);
                if (value == null) continue;
                String valueStr = value instanceof String ? (String) value : JSON.toJSONString(value);
                if ("GET".equals(method)) {
                    if (queryBuilder.length() > 0) queryBuilder.append("&");
                    queryBuilder.append(target).append("=").append(valueStr);
                } else if (StringUtils.isNotBlank(bodyStr)) {
                    bodyStr = bodyStr.replace("{{" + target + "}}", valueStr);
                }
            }
            if (queryBuilder.length() > 0) {
                url = url + (url.contains("?") ? "&" : "?") + queryBuilder;
            }
        }

        try {
            String responseBody = executeRequest(url, method, headersParam, bodyStr);
            return bindFlowData(workflowRunResult, responseBody);
        } catch (Exception e) {
            log.error("（{}）组件HTTP请求失败 url={}", workflowRunResult.getNodeName(), url, e);
            errorResponse.getMessage().setMessage(
                String.format("（%s）组件HTTP请求失败: %s", workflowRunResult.getNodeName(), e.getMessage()));
            return errorResponse;
        }
    }

    private String executeRequest(String url, String method, JSONObject headers, String body) throws Exception {
        Request.Builder builder = new Request.Builder().url(url);

        // 设置 headers
        if (headers != null) {
            for (String key : headers.keySet()) {
                builder.addHeader(key, headers.getString(key));
            }
        }

        RequestBody requestBody = null;
        if (StringUtils.isNotBlank(body)) {
            String contentType = headers != null ? headers.getString("Content-Type") : null;
            MediaType mediaType = MediaType.parse(
                StringUtils.isNotBlank(contentType) ? contentType : "application/json; charset=utf-8");
            requestBody = RequestBody.create(body, mediaType);
        }

        switch (method) {
            case "POST" -> builder.post(requestBody != null ? requestBody : RequestBody.create(new byte[0]));
            case "PUT" -> builder.put(requestBody != null ? requestBody : RequestBody.create(new byte[0]));
            case "DELETE" -> builder.delete(requestBody);
            case "PATCH" -> builder.patch(requestBody != null ? requestBody : RequestBody.create(new byte[0]));
            default -> builder.get();
        }

        try (okhttp3.Response response = HTTP_CLIENT.newCall(builder.build()).execute()) {
            if (response.body() == null) return StringUtils.EMPTY;
            return response.body().string();
        }
    }

    private Response<List<JSONObject>> bindFlowData(WorkflowRunResult workflowRunResult, String responseBody) {
        Response<List<JSONObject>> errorResponse = Response.error(null);
        if (StringUtils.isBlank(responseBody)) {
            errorResponse.getMessage().setMessage(
                String.format("（%s）组件输出结果为空", workflowRunResult.getNodeName()));
            return errorResponse;
        }

        List<JSONObject> outputs = Lists.newArrayList();
        String trimmed = responseBody.trim();

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
                // 尝试从 returnStr 字段提取
                Object returnStr = obj.get("returnStr");
                if (returnStr instanceof String rs && StringUtils.isNotBlank(rs)) {
                    String rsTrimmed = rs.trim();
                    if (rsTrimmed.startsWith("[")) {
                        JSONArray arr = JsonUtils.parseArrayWithArm(rsTrimmed);
                        outputs.add(WorkflowUtil.createOutput(WorkflowJSONKey.output_chart.name(), ARRAY_TYPE, arr));
                        outputs.add(WorkflowUtil.createOutput("output_json_schema", WorkflowJSONKey.dict.name(),
                            JsonUtils.generateJsonSchema(arr, "")));
                    } else {
                        outputs.add(WorkflowUtil.createOutput(WorkflowJSONKey.output_chart.name(), WorkflowJSONKey.str.name(), rs));
                    }
                } else {
                    outputs.add(WorkflowUtil.createOutput(WorkflowJSONKey.output_chart.name(), WorkflowJSONKey.dict.name(), obj));
                }
            } else {
                outputs.add(WorkflowUtil.createOutput(WorkflowJSONKey.output_chart.name(), WorkflowJSONKey.str.name(), trimmed));
            }
        } catch (Exception e) {
            log.warn("（{}）组件响应解析失败，作为字符串输出", workflowRunResult.getNodeName(), e);
            outputs.add(WorkflowUtil.createOutput(WorkflowJSONKey.output_chart.name(), WorkflowJSONKey.str.name(), trimmed));
        }

        return Response.ok(outputs);
    }
}
