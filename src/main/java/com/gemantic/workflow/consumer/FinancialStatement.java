package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.gemantic.dfs.support.DfsInfo;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.workflow.Template;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.google.common.collect.Lists;
import okhttp3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 财务报表 - 直接 HTTP 调用，不走内部 Feign 客户端
 */
@Component("financialStatement")
public class FinancialStatement extends BaseConsumer {

    @Value("${financial.statement.api.url:}")
    private String apiUrl;

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build();

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        LOG.info("执行财务三表组件");
        Template template = DataParametersUtil.getValueByTemplate("file", workflowRunResult.getInputs(), Template.class, null);
        String fileDisplayName = DataParametersUtil.getDisplayNameByTemplate("file", workflowRunResult.getInputs());
        Response<List<JSONObject>> errorResponse = Response.error(null);

        if (null == template) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件中%s为空", workflowRunResult.getNodeName(), fileDisplayName));
            return errorResponse;
        }
        String fileInputKey = template.getName();
        if (!workflowRunResult.getInputs().containsKey(fileInputKey)) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件中%s的\"%s\"不存在", workflowRunResult.getNodeName(), fileDisplayName, fileInputKey));
            return errorResponse;
        }
        List<DfsInfo> files = DataParametersUtil.getValuesByTemplate(fileInputKey, workflowRunResult.getInputs(), DfsInfo.class, Lists.newArrayList());
        if (CollectionUtils.isEmpty(files)) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件中%s的\"%s\"为空", workflowRunResult.getNodeName(), fileDisplayName, fileInputKey));
            return errorResponse;
        }
        List<Long> taskIds = files.stream()
            .filter(f -> MapUtils.isNotEmpty(f.getMeta()) && StringUtils.isNotBlank(MapUtils.getString(f.getMeta(), WorkflowJSONKey.taskId.name())))
            .map(f -> MapUtils.getString(f.getMeta(), WorkflowJSONKey.taskId.name()))
            .map(Long::valueOf)
            .distinct()
            .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(taskIds)) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件中%s的\"%s\"缺失taskId", workflowRunResult.getNodeName(), fileDisplayName, fileInputKey));
            return errorResponse;
        }

        if (StringUtils.isBlank(apiUrl)) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件未配置 financial.statement.api.url", workflowRunResult.getNodeName()));
            return errorResponse;
        }

        List<JSONObject> outputs = Lists.newArrayList();
        try {
            JSONArray data = callFinanceApi(taskIds);
            LOG.info("taskIds={}, 三表数据量：{}", taskIds, data != null ? data.size() : 0);
            JSONObject output = WorkflowUtil.createOutput(WorkflowJSONKey.output.name(), WorkflowJSONKey.dict.name(), data);
            outputs.add(output);
        } catch (Exception e) {
            LOG.error("组件={}({}) 查询结果报错 taskIds={}", workflowRunResult.getNodeName(), workflowRunResult.getId(), taskIds, e);
            if (workflowRunResult.getIgnoreError() == 0) {
                errorResponse.getMessage().setMessage(String.format("（%s）组件查询财务数据失败: %s", workflowRunResult.getNodeName(), e.getMessage()));
                return errorResponse;
            }
        }
        return Response.ok(outputs);
    }

    private JSONArray callFinanceApi(List<Long> taskIds) throws Exception {
        String body = JSON.toJSONString(taskIds);
        RequestBody requestBody = RequestBody.create(body, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build();

        try (okhttp3.Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (response.body() == null) return new JSONArray();
            String responseStr = response.body().string();
            if (StringUtils.isBlank(responseStr)) return new JSONArray();
            JSONObject result = JSONObject.parseObject(responseStr);
            if (result == null) return new JSONArray();
            // 兼容 {data: [...]} 和直接返回数组两种格式
            if (result.containsKey("data")) {
                Object data = result.get("data");
                if (data instanceof JSONArray) return (JSONArray) data;
                return new JSONArray();
            }
            return new JSONArray();
        }
    }
}
