package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gemantic.dfs.support.DfsInfo;
import com.gemantic.gpt.client.AiModelClient;
import com.gemantic.gpt.constant.TraceSessionStatus;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.constant.WorkflowReRunType;
import com.gemantic.gpt.constant.WorkflowRunType;
import com.gemantic.gpt.model.AiModel;
import com.gemantic.gpt.model.TraceSession;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.qdata.FileInfo;
import com.gemantic.gpt.support.workflow.FieldOptions;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.StringUtil;
import com.gemantic.springcloud.utils.ULID;
import com.gemantic.workflow.support.LangChain4jChatService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;
import java.util.Map;

/**
 * 提示基类 — LLM 调用已迁移至 LangChain4j
 */
public abstract class BasePrompt extends BaseConsumer {

    @Resource
    protected AiModelClient aiModelClient;

    @Resource
    protected LangChain4jChatService langChain4jChatService;

    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        String originalPrompt = StringUtil.trim(DataParametersUtil.getValueByTemplate("prompt", workflowRunResult.getInputs(), String.class, StringUtils.EMPTY));
        String promptDisplayName = DataParametersUtil.getDisplayNameByTemplate("prompt", workflowRunResult.getInputs());
        FieldOptions uploadFile = DataParametersUtil.getValueByTemplate("upload_file", workflowRunResult.getInputs(), FieldOptions.class, null);
        workflowRunResult.setSessionId(StringUtils.EMPTY);
        if (StringUtils.isBlank(promptDisplayName)) {
            promptDisplayName = "prompt";
        }

        Response<List<JSONObject>> errorResponse = Response.error(null);
        String error = String.format("（%s）组件中%s为空，请添加%s后重试", workflowRunResult.getNodeName(), promptDisplayName, promptDisplayName);
        if (StringUtils.isBlank(originalPrompt)) {
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
        }

        List<String> parameterKeys = WorkflowUtil.getPromptParameterKeys(originalPrompt);
        JSONObject promptParameters = JSON.copy(workflowRunResult.getInputs(), DataParametersUtil.JSONWRITER_FEATURE);
        updateInputs4UploadFilesContent(parameterKeys, promptParameters);
        Object convertPrompt = convertPrompt(originalPrompt, promptParameters);
        if (null == convertPrompt
                || convertPrompt instanceof List && CollectionUtils.isEmpty((List) convertPrompt)
                || convertPrompt instanceof String && StringUtils.isBlank((String) convertPrompt)) {
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
        }

        List<String> prompts = Lists.newArrayList();
        if (convertPrompt instanceof List) {
            prompts = (List<String>) convertPrompt;
        } else {
            prompts.add((String) convertPrompt);
        }

        // 解析模型配置
        JSONObject llmModelParameter = DataParametersUtil.getParameterByTemplate("llm_model", workflowRunResult.getInputs());
        String llmModelDisplayName = DataParametersUtil.getDisplayNameByTemplate("llm_model", workflowRunResult.getInputs());
        Object llmModelValue = DataParametersUtil.getValueByParameter(llmModelParameter, null);
        error = String.format("（%s）组件中%s为空，请选择%s后重试", workflowRunResult.getNodeName(), llmModelDisplayName, llmModelDisplayName);
        if (null == llmModelValue) {
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
        }

        FieldOptions llmModel;
        if (llmModelValue instanceof String s) {
            if (s.contains("{") && s.contains("}")) {
                llmModel = JSON.parseObject(s, FieldOptions.class);
            } else {
                llmModel = new FieldOptions();
                llmModel.setLabel(s);
            }
        } else {
            llmModel = DataParametersUtil.getValueByParameter(llmModelParameter, FieldOptions.class, null);
        }
        if (null == llmModel || StringUtils.isBlank(llmModel.getLabel())) {
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
        }

        // 查找 AiModel
        error = String.format("（%s）组件中%s选择的\"%s\"不存在，请重新选择后重试", workflowRunResult.getNodeName(), llmModelDisplayName, llmModel.getLabel());
        AiModel aiModel;
        if (NumberUtils.isDigits(llmModel.getValue())) {
            aiModel = aiModelClient.getById(Long.valueOf(llmModel.getValue())).getData();
            if (null == aiModel) {
                errorResponse.getMessage().setMessage(error);
                return errorResponse;
            }
        } else {
            Map<String, String> aiModelParams = Maps.newHashMap();
            aiModelParams.put("name", llmModel.getLabel());
            List<AiModel> aiModels = aiModelClient.find(Lists.newArrayList("updateAt"), Lists.newArrayList("DESC"), 1, 1, aiModelParams).getData().getList();
            if (CollectionUtils.isEmpty(aiModels)) {
                error = String.format("（%s）组件中%s选择的\"%s\"在QConfig模型管理中不存在，请添加模型后重试", workflowRunResult.getNodeName(), llmModelDisplayName, llmModel.getLabel());
                errorResponse.getMessage().setMessage(error);
                return errorResponse;
            }
            aiModel = aiModels.getFirst();
        }

        if (!aiModel.getBase().getChannel().equals(workflowRunResult.getChannel())) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件中%s选择的\"%s\"无使用权限，请重新选择", workflowRunResult.getNodeName(), llmModelDisplayName, llmModel.getLabel()));
            return errorResponse;
        }
        if (aiModel.getStatus() == 0) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件中%s选择的\"%s\"已被停用，请重新选择", workflowRunResult.getNodeName(), llmModelDisplayName, llmModel.getLabel()));
            return errorResponse;
        }

        // 图片文件校验（保留原有逻辑）
        if (null != uploadFile && StringUtils.isNotBlank(uploadFile.getLabel()) && StringUtils.isNotBlank(uploadFile.getValue())) {
            List<DfsInfo> dfsInfos = DataParametersUtil.getValuesByTemplate(uploadFile.getValue(), workflowRunResult.getInputs(), DfsInfo.class, Lists.newArrayList());
            if (CollectionUtils.isEmpty(dfsInfos)) {
                errorResponse.getMessage().setMessage(String.format("（%s）组件无法运行，请在（开始输入）组件的\"%s\"上传文件", workflowRunResult.getNodeName(), uploadFile.getLabel()));
                return errorResponse;
            }
            if (!"1".equals(MapUtils.getString(aiModel.getData(), "isImage"))) {
                errorResponse.getMessage().setMessage(String.format("（%s）组件无法运行，选择的模型不支持图片", workflowRunResult.getNodeName()));
                return errorResponse;
            }
        }

        Float temperature = DataParametersUtil.getValueByTemplate("temperature", workflowRunResult.getInputs(), Float.class, null);
        if (temperature != null) {
            aiModel.setTemperature(temperature);
        }

        // 处理再次生成
        String sessionId = new ULID().nextULID();
        List<TraceSession> chatHistory = getChatHistory(workflowRunResult.getInputs());
        if (CollectionUtils.isNotEmpty(chatHistory)) {
            sessionId = chatHistory.get(chatHistory.size() - 1).getId();
        }
        WorkflowReRunType workflowReRunType = getWorkflowReRunType(chatHistory);
        if (WorkflowReRunType.再次生成.equals(workflowReRunType)) {
            String input = chatHistory.get(chatHistory.size() - 1).getInput();
            String lastOutput = null;
            for (int i = chatHistory.size() - 2; i >= 0; i--) {
                TraceSession ts = chatHistory.get(i);
                if (!TraceSessionStatus.CORRECT_OR_SUCCESS.getStatus().equalsIgnoreCase(ts.getStatus()) || StringUtils.isBlank(ts.getOutput())) continue;
                lastOutput = ts.getOutput();
                break;
            }
            if (StringUtils.isNotBlank(lastOutput) && StringUtils.isNotBlank(input)) {
                prompts = Lists.newArrayList(String.join("\n\n", lastOutput, input));
            }
        }

        boolean useCache = !WorkflowRunType.RERUN.name().equals(workflowRunResult.getWorkflowRunType());

        // 调用 LangChain4j 执行 LLM
        long totalTokens = 0L, inputTokens = 0L, outputTokens = 0L, tokenTime = 0L, firstTokenLatency = 0L;
        List<String> responses = Lists.newArrayList();
        List<JSONObject> llmOutputs = Lists.newArrayList();

        for (String prompt : prompts) {
            LangChain4jChatService.LlmCallResult result = langChain4jChatService.chat(aiModel, prompt, useCache);

            if (!result.isSuccess()) {
                error = String.format("（%s）组件调用模型服务失败: %s", workflowRunResult.getNodeName(), result.error());
                workflowRunResult.setTokenTime(tokenTime);
                workflowRunResult.setFirstTokenLatency(firstTokenLatency);
                workflowRunResult.setTotalTokens(totalTokens);
                workflowRunResult.setInputTokens(inputTokens);
                workflowRunResult.setOutputTokens(outputTokens);
                workflowRunResult.setOutputs(WorkflowUtil.mergeOutputs(workflowRunResult, Lists.newArrayList(buildLlmTokenOutput(llmOutputs))));
                errorResponse.getMessage().setMessage(error);
                return errorResponse;
            }

            totalTokens += result.totalTokens();
            inputTokens += result.inputTokens();
            outputTokens += result.outputTokens();
            firstTokenLatency += result.firstTokenLatency();
            tokenTime += result.tokenTime();

            // 构建 llm_output 兼容结构
            JSONObject tokenInfo = new JSONObject();
            JSONObject tokenUsage = new JSONObject();
            tokenUsage.put("total_tokens", result.totalTokens());
            tokenUsage.put("input_tokens", result.inputTokens());
            tokenUsage.put("output_tokens", result.outputTokens());
            tokenInfo.put("token_usage", tokenUsage);
            tokenInfo.put("first_token_latency", result.firstTokenLatency());
            tokenInfo.put("token_time", result.tokenTime());
            llmOutputs.add(tokenInfo);

            responses.add(StringUtils.defaultString(result.output()));
        }

        workflowRunResult.setSessionId(sessionId);
        workflowRunResult.setTokenTime(tokenTime);
        workflowRunResult.setFirstTokenLatency(firstTokenLatency);
        workflowRunResult.setTotalTokens(totalTokens);
        workflowRunResult.setInputTokens(inputTokens);
        workflowRunResult.setOutputTokens(outputTokens);

        if (CollectionUtils.isEmpty(responses) || responses.size() == 1 && StringUtils.isBlank(responses.getFirst())) {
            workflowRunResult.setOutputs(WorkflowUtil.mergeOutputs(workflowRunResult, Lists.newArrayList(buildLlmTokenOutput(llmOutputs))));
            errorResponse.getMessage().setMessage(String.format("（%s）组件输出结果为空", workflowRunResult.getNodeName()));
            return errorResponse;
        }

        List<JSONObject> outputs = Lists.newArrayList();
        JSONObject output;
        if (convertPrompt instanceof List) {
            output = WorkflowUtil.createOutput(WorkflowJSONKey.output.name(), WorkflowJSONKey.str.name(), responses);
        } else {
            output = WorkflowUtil.createOutput(WorkflowJSONKey.output.name(), WorkflowJSONKey.str.name(), responses.getFirst());
            outputs.addAll(WorkflowUtil.getTableLlmOutputs(responses.getFirst()));
        }
        outputs.add(output);
        outputs.add(buildLlmTokenOutput(llmOutputs));
        return Response.ok(outputs);
    }

    private JSONObject buildLlmTokenOutput(List<JSONObject> llmOutputs) {
        JSONObject out = WorkflowUtil.createOutput(WorkflowJSONKey.output_llm_token.name(), WorkflowJSONKey.dict.name(), llmOutputs);
        out.put(WorkflowJSONKey.display_name.name(), "输出：大模型参数");
        return out;
    }

    public abstract Object convertPrompt(String prompt, JSONObject inputs) throws Exception;
}


    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        String originalPrompt = StringUtil.trim(DataParametersUtil.getValueByTemplate("prompt", workflowRunResult.getInputs(), String.class, StringUtils.EMPTY));
        String prompt_display_name = DataParametersUtil.getDisplayNameByTemplate("prompt", workflowRunResult.getInputs());
        FieldOptions upload_file = DataParametersUtil.getValueByTemplate("upload_file",workflowRunResult.getInputs(),FieldOptions.class,null);
        workflowRunResult.setSessionId(StringUtils.EMPTY);
        if (StringUtils.isBlank(prompt_display_name)) {
            prompt_display_name = "prompt";
        }
        Response<List<JSONObject>> errorResponse = Response.error(null);
        String error = String.format("（%s）组件中%s为空，请添加%s后重试", workflowRunResult.getNodeName(), prompt_display_name, prompt_display_name);
        if (StringUtils.isBlank(originalPrompt)) {
//            throw new WorkflowException(error);
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
        }
        List<String> parameterKeys = WorkflowUtil.getPromptParameterKeys(originalPrompt);
        JSONObject promptParameters = JSON.copy(workflowRunResult.getInputs(),DataParametersUtil.JSONWRITER_FEATURE);
        updateInputs4UploadFilesContent(parameterKeys,promptParameters);
        Object convertPrompt = convertPrompt(originalPrompt, promptParameters);
        if (null == convertPrompt || convertPrompt instanceof List && CollectionUtils.isEmpty((List) convertPrompt) || convertPrompt instanceof String && StringUtils.isBlank((String) convertPrompt)) {
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
            //            throw new WorkflowException(error);
        }

        List<String> prompts = Lists.newArrayList();
        if (convertPrompt instanceof List) {
            prompts = (List<String>) convertPrompt;
        } else {
            prompts.add((String) convertPrompt);
        }
        JSONObject llm_model_parameter = DataParametersUtil.getParameterByTemplate("llm_model", workflowRunResult.getInputs());
        String llm_model_display_name = DataParametersUtil.getDisplayNameByTemplate("llm_model", workflowRunResult.getInputs());
        Object llm_model_value = DataParametersUtil.getValueByParameter(llm_model_parameter, null);
        error = String.format("（%s）组件中%s为空，请选择%s后重试", workflowRunResult.getNodeName(), llm_model_display_name, llm_model_display_name);
        if (null == llm_model_value) {
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
//            throw new WorkflowException(error);
        }
        FieldOptions llm_model = null;
        if (LOG.isDebugEnabled()) {
            LOG.debug("工作流组件={}({}) llm_model_value={}", workflowRunResult.getNodeName(), workflowRunResult.getId(), llm_model_value);
        }
        if (llm_model_value instanceof String) {
            if (llm_model_value.toString().contains("{") && llm_model_value.toString().contains("}")) {
                llm_model = JSON.parseObject(llm_model_value.toString(), FieldOptions.class);
            } else {
                llm_model = new FieldOptions();
                llm_model.setLabel(llm_model_value.toString());
            }
        } else {
            llm_model = DataParametersUtil.getValueByParameter(llm_model_parameter, FieldOptions.class, null);
        }
        if (null == llm_model || StringUtils.isBlank(llm_model.getLabel())) {
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
            //            throw new WorkflowException(error);
        }
        error = String.format("（%s）组件中%s选择的\"%s\"不存在，请重新选择后重试", workflowRunResult.getNodeName(), llm_model_display_name, llm_model.getLabel());
        AiModel aiModel = null;
        if (NumberUtils.isDigits(llm_model.getValue())) {
            aiModel = aiModelClient.getById(Long.valueOf(llm_model.getValue())).getData();
            if (null == aiModel) {
//                throw new WorkflowException(error);
                errorResponse.getMessage().setMessage(error);
                return errorResponse;
            }
        } else {
            Map<String, String> aiModelParams = Maps.newHashMap();
            aiModelParams.put("name", llm_model.getLabel());
            List<AiModel> aiModels = aiModelClient.find(Lists.newArrayList("updateAt"), Lists.newArrayList("DESC"), 1, 1, aiModelParams).getData().getList();
            if (CollectionUtils.isEmpty(aiModels)) {
                error = String.format("（%s）组件中%s选择的\"%s\"在QConfig模型管理中不存在，请添加模型后重试", workflowRunResult.getNodeName(), llm_model_display_name, llm_model.getLabel());
//                throw new WorkflowException(error);
                errorResponse.getMessage().setMessage(error);
                return errorResponse;
            }
            aiModel = aiModels.getFirst();
            if (LOG.isDebugEnabled()) {
                LOG.debug("工作流组件={}({}) 兼容老版本模型配置={} QConfig对应模型={}({})", workflowRunResult.getNodeName(), workflowRunResult.getId(), llm_model.getLabel(), aiModel.getName(), aiModel.getId());
            }
        }
        if (!aiModel.getBase().getChannel().equals(workflowRunResult.getChannel())) {
            error = String.format("（%s）组件中%s选择的\"%s\"无使用权限，请重新选择", workflowRunResult.getNodeName(), llm_model_display_name, llm_model.getLabel());
//            throw new WorkflowException(error);
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
        }
        if (aiModel.getStatus() == 0) {
            error = String.format("（%s）组件中%s选择的\"%s\"已被停用，请重新选择", workflowRunResult.getNodeName(), llm_model_display_name, llm_model.getLabel());
//            throw new WorkflowException(error);
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
        }
        List<FileInfo> files = Lists.newArrayList();
        if(null != upload_file && StringUtils.isNotBlank(upload_file.getLabel()) && StringUtils.isNotBlank(upload_file.getValue())){
            List<DfsInfo> dfsInfos = DataParametersUtil.getValuesByTemplate(upload_file.getValue(),workflowRunResult.getInputs(), DfsInfo.class,Lists.newArrayList());
            if(CollectionUtils.isEmpty(dfsInfos)){
                errorResponse.getMessage().setMessage(String.format("（%s）组件无法运行，请在（开始输入）组件的\"%s\"上传文件", workflowRunResult.getNodeName(),upload_file.getLabel()));
                return errorResponse;
            }
            if(!"1".equals(MapUtils.getString(aiModel.getData(),"isImage"))){
                errorResponse.getMessage().setMessage(String.format("（%s）组件无法运行，选择的模型不支持图片", workflowRunResult.getNodeName()));
                return errorResponse;
            }
            files = dfs2Files(dfsInfos);
        }
        Float temperature = DataParametersUtil.getValueByTemplate("temperature", workflowRunResult.getInputs(), Float.class, null);
        List<String> responses = Lists.newArrayList();
        String sessionId = new ULID().nextULID();
        List<TraceSession> chatHistory = getChatHistory(workflowRunResult.getInputs());
        if (CollectionUtils.isNotEmpty(chatHistory)) {
            sessionId = chatHistory.get(chatHistory.size() - 1).getId();
        }
        WorkflowReRunType workflowReRunType = getWorkflowReRunType(chatHistory);

//        System.out.println("workflowReRunType=" + workflowReRunType + " chatHistory=" + chatHistory.size());

        if (WorkflowReRunType.再次生成.equals(workflowReRunType)) {
            String input = chatHistory.get(chatHistory.size() - 1).getInput();
            String lastOutput = null;
            for (int i = chatHistory.size() - 2; i >= 0; i--) {
                TraceSession traceSession = chatHistory.get(i);
                if (!TraceSessionStatus.CORRECT_OR_SUCCESS.getStatus().equalsIgnoreCase(traceSession.getStatus()) || StringUtils.isBlank(traceSession.getOutput())) {
                    continue;
                }
                lastOutput = traceSession.getOutput();
                break;
            }
//            System.out.println("再次生成 input=" + input + " lastOutput=" + lastOutput);

            if (StringUtils.isNotBlank(lastOutput) && StringUtils.isNotBlank(input)) {
                prompts = Lists.newArrayList(String.join("\n\n", lastOutput, input));
            }
        }
        Long outputTokens = 0L;
        Long inputTokens = 0L;
        Long totalTokens = 0L;
        Long tokenTime = 0L;
        Long firstTokenLatency = 0L;
        List<JSONObject> llm_outputs = Lists.newArrayList();
        for (int i = 0; i < prompts.size(); i++) {
            String prompt = prompts.get(i);
            QLlmInfo request = new QLlmInfo();
            request.setAiModel(aiModel);
            request.setFiles(files);
            if (null != temperature) {
                request.getAiModel().setTemperature(temperature);
            }
            if (WorkflowRunType.RERUN.name().equals(workflowRunResult.getWorkflowRunType())) {
                request.setUse_cache(Boolean.FALSE);
            }
            request.setWorkflow_id(String.valueOf(workflowRunResult.getWorkflowId()));
            request.setWorkflow_node_id(workflowRunResult.getNodeId());
            request.setWorkflow_run_id(String.valueOf(workflowRunResult.getWorkflowRunId()));
            request.setWorkflow_run_result_id(String.valueOf(workflowRunResult.getId()));
            request.setSession_id(sessionId);
            request.setExecution_order(i + 1);
            request.setPrompt(prompt);

            Response<QLlmInfo> response = null;
            try {
                Long l1 = DateTime.now().getMillis();
                response = qLlmClient.qllm(request);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("工作流组件={}({}) request={} response={}", workflowRunResult.getNodeName(), workflowRunResult.getId(),new  String(JSON.toJSONBytes(request)), new String(JSON.toJSONBytes(response)));
                }
                if(null != response && null != response.getData() && MapUtils.isNotEmpty(response.getData().getLlm_output())){
                    JSONObject modelToken = response.getData().getLlm_output();
                    llm_outputs.add(modelToken);
                    if(null != modelToken.get("token_usage") && modelToken.get("token_usage") instanceof Map && null != modelToken.get("first_token_latency") && null !=modelToken.get("token_time")){
                        JSONObject token_usage = modelToken.getJSONObject("token_usage");
                        if(MapUtils.isNotEmpty(token_usage) && token_usage.containsKey("total_tokens") && token_usage.containsKey("output_tokens") && token_usage.containsKey("input_tokens")) {
                            totalTokens += token_usage.getLongValue("total_tokens",0L) <0L? 0L:token_usage.getLongValue("total_tokens",0L);
                            outputTokens += token_usage.getLongValue("output_tokens",0L) <0L? 0L:token_usage.getLongValue("output_tokens",0L);
                            inputTokens += token_usage.getLongValue("input_tokens",0L) <0L? 0L:token_usage.getLongValue("input_tokens",0L);
                        }
                        firstTokenLatency+= modelToken.getLongValue("first_token_latency",0L);
                        tokenTime+=modelToken.getLongValue("token_time",0L);
                    }
                }else {
                    LOG.warn("工作流组件={}({}) 模型服务返回失败信息 request={} response={}", workflowRunResult.getNodeName(), workflowRunResult.getId(), new String(JSON.toJSONBytes(request)), new String(JSON.toJSONBytes(response)));
                    llm_outputs.add(null);
                }
            } catch (Exception e) {
                LOG.error("工作流组件={}({}) 调用模型服务失败 request={} response={}", workflowRunResult.getNodeName(), workflowRunResult.getId(),new String(JSON.toJSONBytes(request)), new String(JSON.toJSONBytes(response)),e);
                error = String.format("（%s）组件调用模型服务失败", workflowRunResult.getNodeName());
                workflowRunResult.setTokenTime(tokenTime);
                workflowRunResult.setFirstTokenLatency(firstTokenLatency);
                workflowRunResult.setTotalTokens(totalTokens);
                workflowRunResult.setInputTokens(inputTokens);
                workflowRunResult.setOutputTokens(outputTokens);
                errorResponse.getMessage().setMessage(error);
                workflowRunResult.setOutputs(WorkflowUtil.mergeOutputs(workflowRunResult,Lists.newArrayList(getLlmOutput(llm_outputs))));
                return errorResponse;
                //                throw new WorkflowException(error);
            }
            if (!MsgUtil.isValidMessage(response) && null != response && null != response.getMessage()) {
                error = String.format("（%s）组件%s", workflowRunResult.getNodeName(), response.getMessage().getMessage());
                workflowRunResult.setTokenTime(tokenTime);
                workflowRunResult.setFirstTokenLatency(firstTokenLatency);
                workflowRunResult.setTotalTokens(totalTokens);
                workflowRunResult.setInputTokens(inputTokens);
                workflowRunResult.setOutputTokens(outputTokens);
                workflowRunResult.setOutputs(WorkflowUtil.mergeOutputs(workflowRunResult,Lists.newArrayList(getLlmOutput(llm_outputs))));
                errorResponse.getMessage().setMessage(error);
                return errorResponse;
//                throw new WorkflowException(error);
            }
            if (MsgUtil.isValidMessage(response) && null != response && null != response.getData() && StringUtils.isNotBlank(response.getData().getOutput()) && response.getData().getOutput().contains("api错误")) {
                error = String.format("（%s）组件%s", workflowRunResult.getNodeName(), response.getData().getOutput());
                workflowRunResult.setTokenTime(tokenTime);
                workflowRunResult.setFirstTokenLatency(firstTokenLatency);
                workflowRunResult.setTotalTokens(totalTokens);
                workflowRunResult.setInputTokens(inputTokens);
                workflowRunResult.setOutputTokens(outputTokens);
                workflowRunResult.setOutputs(WorkflowUtil.mergeOutputs(workflowRunResult,Lists.newArrayList(getLlmOutput(llm_outputs))));
                errorResponse.getMessage().setMessage(error);
                return errorResponse;
//                throw new WorkflowException(error);
            }
            if (null == response) {
                responses.add(StringUtils.EMPTY);
                continue;
            }
            if (null == response.getData()) {
                responses.add(StringUtils.EMPTY);
                continue;
            }
            String output = response.getData().getOutput();
            if (StringUtils.isBlank(output)) {
                responses.add(StringUtils.EMPTY);
                continue;
            }
            responses.add(output);
        }
        workflowRunResult.setSessionId(sessionId);
        workflowRunResult.setTokenTime(tokenTime);
        workflowRunResult.setFirstTokenLatency(firstTokenLatency);
        workflowRunResult.setTotalTokens(totalTokens);
        workflowRunResult.setInputTokens(inputTokens);
        workflowRunResult.setOutputTokens(outputTokens);
        if (CollectionUtils.isEmpty(responses) || responses.size() == 1 && StringUtils.isBlank(responses.getFirst())) {
            workflowRunResult.setOutputs(WorkflowUtil.mergeOutputs(workflowRunResult,Lists.newArrayList(getLlmOutput(llm_outputs))));
            error = String.format("（%s）组件输出结果为空", workflowRunResult.getNodeName());
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
        }
        List<JSONObject> outputs = Lists.newArrayList();
        JSONObject output = null;
        if (convertPrompt instanceof List) {
            output = WorkflowUtil.createOutput(WorkflowJSONKey.output.name(), WorkflowJSONKey.str.name(), responses);
        } else {
            output = WorkflowUtil.createOutput(WorkflowJSONKey.output.name(), WorkflowJSONKey.str.name(), responses.getFirst());
            outputs.addAll(WorkflowUtil.getTableLlmOutputs(responses.getFirst()));
        }
        outputs.add(output);
        outputs.add(getLlmOutput(llm_outputs));
        return Response.ok(outputs);
    }

    private JSONObject getLlmOutput(List<JSONObject> llm_outputs){
        JSONObject output_llm = WorkflowUtil.createOutput(WorkflowJSONKey.output_llm_token.name(),WorkflowJSONKey.dict.name(),llm_outputs);
        output_llm.put(WorkflowJSONKey.display_name.name(), "输出：大模型参数");
        return output_llm;
    }

    public abstract Object convertPrompt(String prompt, JSONObject inputs) throws Exception;


}
