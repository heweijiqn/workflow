package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.client.ApplicationConfigClient;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.model.ApplicationConfig;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.workflow.WorkflowException;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.StringUtil;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.bing.BingWebSearchEngine;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 搜索引擎 - 使用 LangChain4j BingWebSearchEngine 实现
 */
@Component("internetSearch")
public class InternetSearch extends BaseConsumer {

    @Resource
    private ApplicationConfigClient applicationConfigClient;

    private final Map<String, WebSearchEngine> engineCache = new ConcurrentHashMap<>();

    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        JSONObject inputs = workflowRunResult.getInputs();
        String searchText = StringUtil.trim(DataParametersUtil.getValueByTemplate("search_text", inputs, String.class, StringUtils.EMPTY));
        String searchTextDisplayName = DataParametersUtil.getDisplayNameByTemplate("search_text", inputs);
        if (StringUtils.isBlank(searchTextDisplayName)) {
            searchTextDisplayName = "search_text";
        }

        Response<List<JSONObject>> errorResponse = Response.error(null);
        if (StringUtils.isBlank(searchText)) {
            errorResponse.getMessage().setMessage(String.format("(%s)组件%s输入为空", workflowRunResult.getNodeName(), searchTextDisplayName));
            return errorResponse;
        }
        searchText = WorkflowUtil.replacePrompt(inputs, searchText);
        if (StringUtils.isBlank(StringUtil.trimAllBlank(searchText))) {
            errorResponse.getMessage().setMessage(String.format("(%s)组件%s输入为空", workflowRunResult.getNodeName(), searchTextDisplayName));
            return errorResponse;
        }

        Integer count = DataParametersUtil.getValueByTemplate("count", inputs, Integer.class, 5);
        List<JSONObject> searchResult;
        try {
            searchResult = doSearch(workflowRunResult, searchText, count);
        } catch (WorkflowException e) {
            errorResponse.getMessage().setMessage(e.getMessage());
            return errorResponse;
        } catch (Exception e) {
            errorResponse.getMessage().setMessage(String.format("(%s)组件BING搜索请求失败", workflowRunResult.getNodeName()));
            return errorResponse;
        }

        if (CollectionUtils.isEmpty(searchResult)) {
            errorResponse.getMessage().setMessage(String.format("(%s)组件BING搜索无结果", workflowRunResult.getNodeName()));
            return errorResponse;
        }

        List<JSONObject> outputs = Lists.newArrayList();
        JSONObject output = WorkflowUtil.createOutput(WorkflowJSONKey.output.name(), WorkflowJSONKey.dict.name(), null);
        JSONObject outputTitle = WorkflowUtil.createOutput(WorkflowJSONKey.output_title.name(), WorkflowJSONKey.str.name(), Lists.newArrayList());
        JSONObject outputContent = WorkflowUtil.createOutput(WorkflowJSONKey.output_content.name(), WorkflowJSONKey.str.name(), Lists.newArrayList());
        JSONObject outputUrl = WorkflowUtil.createOutput(WorkflowJSONKey.output_url.name(), WorkflowJSONKey.str.name(), Lists.newArrayList());
        outputs.add(output);
        outputs.add(outputTitle);
        outputs.add(outputContent);
        outputs.add(outputUrl);

        Boolean combineResultInText = DataParametersUtil.getValueByTemplate("combine_result_in_text", inputs, Boolean.class, Boolean.FALSE);
        if (Boolean.TRUE.equals(combineResultInText)) {
            output.put(WorkflowJSONKey.multiline.name(), Boolean.FALSE);
            outputTitle.put(WorkflowJSONKey.multiline.name(), Boolean.FALSE);
            outputContent.put(WorkflowJSONKey.multiline.name(), Boolean.FALSE);
            outputUrl.put(WorkflowJSONKey.multiline.name(), Boolean.FALSE);
            output.put(WorkflowJSONKey.type.name(), WorkflowJSONKey.str.name());
            StringBuilder sb = new StringBuilder(), titleSb = new StringBuilder(),
                contentSb = new StringBuilder(), urlSb = new StringBuilder();
            for (JSONObject item : searchResult) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(item.getString("标题")).append("\n").append(item.getString("摘要")).append("\n").append(item.getString("网址"));
                if (titleSb.length() > 0) titleSb.append("\n");
                titleSb.append(item.getString("标题"));
                if (contentSb.length() > 0) contentSb.append("\n");
                contentSb.append(item.getString("摘要"));
                if (urlSb.length() > 0) urlSb.append("\n");
                urlSb.append(item.getString("网址"));
            }
            output.put(WorkflowJSONKey.value.name(), sb.toString());
            outputTitle.put(WorkflowJSONKey.value.name(), titleSb.toString());
            outputContent.put(WorkflowJSONKey.value.name(), contentSb.toString());
            outputUrl.put(WorkflowJSONKey.value.name(), urlSb.toString());
        } else {
            output.put(WorkflowJSONKey.value.name(), searchResult);
            outputTitle.put(WorkflowJSONKey.value.name(), searchResult.stream().map(s -> MoreObjects.firstNonNull(s.getString("标题"), StringUtils.EMPTY)).collect(Collectors.toList()));
            outputContent.put(WorkflowJSONKey.value.name(), searchResult.stream().map(s -> MoreObjects.firstNonNull(s.getString("摘要"), StringUtils.EMPTY)).collect(Collectors.toList()));
            outputUrl.put(WorkflowJSONKey.value.name(), searchResult.stream().map(s -> MoreObjects.firstNonNull(s.getString("网址"), StringUtils.EMPTY)).collect(Collectors.toList()));
        }
        return Response.ok(outputs);
    }

    private List<JSONObject> doSearch(WorkflowRunResult workflowRunResult, String searchText, Integer count) throws Exception {
        Map<String, String> appconfigParam = Maps.newHashMap();
        appconfigParam.put("appId", workflowRunResult.getAppId().toString());
        appconfigParam.put("productConfig.code", "bing_access");
        List<ApplicationConfig> applicationConfigs = applicationConfigClient.find(1, 1, appconfigParam).getData().getList();
        if (CollectionUtils.isEmpty(applicationConfigs)) {
            throw new WorkflowException(String.format("(%s)组件BING搜索的访问账号地址没有在QConfig系统配置", workflowRunResult.getNodeName()));
        }
        JSONObject config = applicationConfigs.getFirst().getData().getJSONObject(WorkflowJSONKey.template.name());
        String appKey = DataParametersUtil.getValueByTemplate("appKey", config, String.class, null);
        if (StringUtils.isBlank(appKey)) {
            throw new WorkflowException(String.format("(%s)组件BING搜索的访问账号在产品/应用模块中配置为空", workflowRunResult.getNodeName()));
        }

        WebSearchEngine engine = engineCache.computeIfAbsent(appKey,
            key -> BingWebSearchEngine.builder().apiKey(key).build());

        WebSearchRequest request = WebSearchRequest.builder()
            .searchTerms(searchText)
            .maxResults(count)
            .language("zh-CN")
            .build();

        WebSearchResults results = engine.search(request);
        if (results == null || results.results() == null || CollectionUtils.isEmpty(results.results().organicResults())) {
            LOG.warn("BING搜索无返回结果 searchText={}", searchText);
            return Lists.newArrayList();
        }

        return results.results().organicResults().stream().map(r -> {
            JSONObject item = new JSONObject();
            item.put("标题", r.title());
            item.put("摘要", r.snippet() != null ? r.snippet() : StringUtils.EMPTY);
            item.put("网址", r.url() != null ? r.url().toString() : StringUtils.EMPTY);
            return item;
        }).collect(Collectors.toList());
    }
}
