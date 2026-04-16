package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.StringUtil;
import com.google.common.collect.Lists;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文本分割 — 基于 LangChain4j DocumentSplitter 实现
 */
@Component("textSplitters")
public class TextSplitters extends BaseConsumer {

    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        String text = StringUtil.trim(DataParametersUtil.getValueByTemplate("text", workflowRunResult.getInputs(), String.class, StringUtils.EMPTY));
        List<String> parameterKeys = WorkflowUtil.getPromptParameterKeys(text);
        JSONObject promptParameters = JSON.copy(workflowRunResult.getInputs(), DataParametersUtil.JSONWRITER_FEATURE);
        updateInputs4UploadFilesContent(parameterKeys, promptParameters);
        text = WorkflowUtil.replacePrompt(promptParameters, text);
        if (StringUtils.isBlank(text)) {
            return null;
        }

        String splitMethod = DataParametersUtil.getValueByTemplate("split_method", workflowRunResult.getInputs(), String.class, "general");
        List<String> texts;

        if ("delimiter".equalsIgnoreCase(splitMethod)) {
            // 按分隔符分割，LangChain4j 没有直接对应，保留原逻辑
            String delimiter = DataParametersUtil.getValueByTemplate("delimiter", workflowRunResult.getInputs(), String.class, "\n");
            texts = Arrays.stream(StringUtils.splitByWholeSeparator(text, delimiter))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        } else {
            // 通用分割 — 使用 LangChain4j DocumentSplitter
            Integer chunkLength = DataParametersUtil.getValueByTemplate("chunk_length", workflowRunResult.getInputs(), Integer.class, 500);
            Integer chunkOverlap = DataParametersUtil.getValueByTemplate("chunk_overlap", workflowRunResult.getInputs(), Integer.class, 30);

            DocumentSplitter splitter = new DocumentByCharacterSplitter(chunkLength, chunkOverlap);
            Document document = Document.from(text);
            List<TextSegment> segments = splitter.split(document);
            texts = segments.stream()
                .map(TextSegment::text)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        }

        JSONObject output = WorkflowUtil.createOutput("output", "str", texts);
        return Response.ok(Lists.newArrayList(output));
    }

    @Override
    public Boolean available() {
        return Boolean.TRUE;
    }
}
