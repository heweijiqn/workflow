package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gemantic.dfs.support.DfsInfo;
import com.gemantic.es.utils.EsClientUtil;
import com.gemantic.gpt.client.*;
import com.gemantic.gpt.constant.*;
import com.gemantic.gpt.model.Application;
import com.gemantic.gpt.model.ChatHistory;
import com.gemantic.gpt.model.TraceSession;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.qdata.FileInfo;
import com.gemantic.gpt.support.workflow.FieldOptions;
import com.gemantic.gpt.util.ChatUtil;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.search.constant.CommonFields;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.user.UserInfo;
import com.gemantic.springcloud.utils.StringUtil;
import com.gemantic.springcloud.utils.TextProcessUtil;
import com.gemantic.springcloud.utils.ULID;
import com.gemantic.tools.utils.OkHttpUtil;
import com.gemantic.workflow.client.ExtractTaskClient;
import com.gemantic.workflow.model.ExtractTask;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class BaseQChat extends BaseConsumer {
    @Resource
    protected ApplicationClient applicationClient;

    @Resource
    protected TraceSessionClient traceSessionClient;

    @Resource
    protected TraceSessionEsClient traceSessionEsClient;



    @Resource
    private ChatHistoryClient chatHistoryClient;

    @Resource
    private ExtractTaskClient extractTaskClient;

    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        JSONObject inputs = workflowRunResult.getInputs();
        String search_text = StringUtil.trim(DataParametersUtil.getValueByTemplate("search_text", inputs, String.class, StringUtils.EMPTY));
        String search_text_display_name = DataParametersUtil.getDisplayNameByTemplate("search_text", inputs);
        if (StringUtils.isBlank(search_text_display_name)) {
            search_text_display_name = "search_text";
        }
        String search_text_error = String.format("（%s）组件%s输入为空，请检查连线是否正确", workflowRunResult.getNodeName(), search_text_display_name);
        Response<List<JSONObject>> errorResponse = Response.error(null);
        if (StringUtils.isBlank(search_text)) {
            errorResponse.getMessage().setMessage(search_text_error);
            return errorResponse;
//            throw new WorkflowException(search_text_error);
        }
        search_text = WorkflowUtil.replacePrompt(inputs, search_text);
        if (StringUtils.isBlank(StringUtil.trimAllBlank(search_text))) {
            errorResponse.getMessage().setMessage(search_text_error);
            return errorResponse;
//            throw new WorkflowException(search_text_error);
        }
        String application_display_name = DataParametersUtil.getDisplayNameByTemplate("application", inputs);
        FieldOptions application_option = DataParametersUtil.getValueByTemplate("application", inputs, FieldOptions.class, null);
        if (null == application_option || StringUtils.isBlank(application_option.getValue()) || !NumberUtils.isDigits(application_option.getValue())) {
            String application_error = String.format("（%s）组件%s为空", workflowRunResult.getNodeName(), application_display_name);
            errorResponse.getMessage().setMessage(application_error);
            return errorResponse;
            //            throw new WorkflowException(application_error);
        }
        Application application = applicationClient.getById(Long.valueOf(application_option.getValue())).getData();
        if (null == application) {
            String application_error = String.format("（%s）组件%s,\"%s(%s)\"应用不存在,请重新选择", workflowRunResult.getNodeName(), application_display_name, application_option.getLabel(), application_option.getValue());
            errorResponse.getMessage().setMessage(application_error);
            return errorResponse;
//            throw new WorkflowException(application_error);
        }
        UserInfo userInfo = workflowRunResult.getUserInfo();
        if (null == userInfo) {
            userInfo = new UserInfo();
            userInfo.setUserId(workflowRunResult.getUserId());
            userInfo.setUserName(workflowRunResult.getUserName());
            userInfo.setUserGroupId(workflowRunResult.getUserGroupId());
            userInfo.setChannel(workflowRunResult.getChannel());
            workflowRunResult.setUserInfo(userInfo);
        }
        if (!application.getChannel().equals(workflowRunResult.getUserInfo().getChannel())) {
            String application_error = String.format("（%s）组件%s,\"%s(%s)\"应用无使用权限,请重新选择", workflowRunResult.getNodeName(), application_display_name, application_option.getLabel(), application_option.getValue());
            errorResponse.getMessage().setMessage(application_error);
            return errorResponse;
//            throw new WorkflowException(application_error);
        }

        if (application.getStatus() == 0) {
            String application_error = String.format("（%s）组件%s,\"%s(%s)\"应用已被禁用,请重新选择", workflowRunResult.getNodeName(), application_display_name, application_option.getLabel(), application_option.getValue());
            errorResponse.getMessage().setMessage(application_error);
            return errorResponse;
//            throw new WorkflowException(application_error);
        }
        if (null == application.getLastReleaseVersionId() || application.getLastReleaseVersionId() <= 0L) {
            String application_error = String.format("（%s）组件%s,\"%s(%s)\"应用未发布,请重新选择", workflowRunResult.getNodeName(), application_display_name, application_option.getLabel(), application_option.getValue());
            errorResponse.getMessage().setMessage(application_error);
            return errorResponse;
//            throw new WorkflowException(application_error);
        }
        Boolean use_upload_file = DataParametersUtil.getValueByTemplate("use_upload_file", workflowRunResult.getInputs(), Boolean.class, Boolean.FALSE);
        Map<String, List<DfsInfo>> dfsInfos = WorkflowUtil.getDfsInfoByTemplate(workflowRunResult.getInputs());
        if (use_upload_file && MapUtils.isEmpty(dfsInfos)) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件无法运行，请在（开始输入）组件上传文件", workflowRunResult.getNodeName()));
            return errorResponse;
//            throw new WorkflowException(String.format("（%s）组件无法运行，请在（开始输入）组件上传文件", workflowRunResult.getNodeName()));
        }
//        if(use_upload_file){
//            FieldOptions upload_file = DataParametersUtil.getValueByTemplate("upload_file",workflowRunResult.getInputs(),FieldOptions.class,null);
//            if(null != upload_file && StringUtils.isNotBlank(upload_file.getValue()) && dfsInfos.containsKey(upload_file.getValue())){
//                List<DfsInfo> filters = dfsInfos.get(upload_file.getValue());
//                dfsInfos = Maps.newHashMap();
//                dfsInfos.put(upload_file.getValue(),filters);
//            }
//        }
        String session_id = new ULID().nextULID();
        List<TraceSession> chatHistory = getChatHistory(workflowRunResult.getInputs());
        if (CollectionUtils.isNotEmpty(chatHistory)) {
            session_id = chatHistory.getLast().getId();
        }
        workflowRunResult.setSessionId(session_id);
        WorkflowReRunType workflowReRunType = getWorkflowReRunType(chatHistory);
        JSONObject sendInfo = getSendInfo(workflowRunResult, search_text);
        JSONObject sendInputs = new JSONObject();
        if (WorkflowReRunType.再次生成.equals(workflowReRunType) && StringUtils.isNotBlank(chatHistory.getLast().getParent_id())) {
            search_text = chatHistory.getLast().getInput();
            List<TraceSession> history = Lists.newArrayList();
            for (int i = chatHistory.size() - 2; i >= 0; i--) {
                TraceSession traceSession = chatHistory.get(i);
                if (!TraceSessionStatus.CORRECT_OR_SUCCESS.getStatus().equals(traceSession.getStatus()) || StringUtils.isBlank(traceSession.getOutput())) {
                    continue;
                }
                history.add(traceSession);
                if (traceSession.getTags().contains(WorkflowReRunType.重新生成.name())) {
                    break;
                }
            }
            if (CollectionUtils.isNotEmpty(history)) {
                history.sort(Comparator.comparing(TraceSession::getStart_time));
                sendInputs.put("聊天记录", history.stream().map(h -> {
                    JSONObject r = new JSONObject();
                    r.put(WorkflowJSONKey.id.name(), h.getId());
                    r.put(WorkflowJSONKey.type.name(), h.getTags().getFirst());
                    r.put("detail", chatHistory2Json(h, Boolean.FALSE));
                    return r;
                }).collect(Collectors.toList()));
            }
        }
        if (use_upload_file && MapUtils.isNotEmpty(dfsInfos)) {
            FieldOptions upload_file = DataParametersUtil.getValueByTemplate("upload_file",workflowRunResult.getInputs(),FieldOptions.class,null);
            if(null != upload_file && StringUtils.isNotBlank(upload_file.getValue())){
                List<DfsInfo> filters = dfsInfos.get(upload_file.getValue());
                if(CollectionUtils.isEmpty(filters)){
                    errorResponse.getMessage().setMessage(String.format("（%s）组件无法运行，请在（开始输入）组件的\"%s\"上传文件", workflowRunResult.getNodeName(),upload_file.getValue()));
                    return errorResponse;
                }
                dfsInfos = Maps.newHashMap();
                dfsInfos.put(upload_file.getValue(),filters);
            }
            List<DfsInfo> dfsList = dfsInfos.values().stream().flatMap(s -> s.stream()).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(dfsList)) {
                errorResponse.getMessage().setMessage(String.format("（%s）组件无法运行，请在（开始输入）组件上传文件", workflowRunResult.getNodeName()));
                return errorResponse;
//                throw new WorkflowException(String.format("（%s）组件无法运行，请在（开始输入）组件上传文件", workflowRunResult.getNodeName()));
            }
            if(ApplicationTypeCode.QAgent.name().equalsIgnoreCase(getClass().getSimpleName())){
                List<FileInfo> fileInfos = dfs2Files(dfsList);
                sendInputs.put("文件",fileInfos);
            }else {
                sendInfo.put("doc_id", dfsList.stream().map(d -> d.getMeta().getString(WorkflowJSONKey.docId.name())).filter(d -> StringUtils.isNotBlank(d)).distinct().collect(Collectors.toList()));
                List<String> doc_task_id = dfsList.stream().map(d -> d.getMeta().getString(WorkflowJSONKey.taskId.name())).filter(d -> NumberUtils.isDigits(d)).distinct().collect(Collectors.toList());
                sendInfo.put("doc_task_id", doc_task_id);
                if (CollectionUtils.isNotEmpty(doc_task_id)) {
                    ExtractTask task = extractTaskClient.getById(Long.valueOf(doc_task_id.getFirst())).getData();
                    if(null != task){
                        sendInfo.put("doc_app_id", task.getApplicationId());
                    }else {
                        sendInfo.put("doc_app_id", workflowRunResult.getAppId().toString());
                    }
                } else {
                    sendInfo.put("doc_app_id", "0");
                }
            }
        }
        if (WorkflowRunType.RERUN.name().equals(workflowRunResult.getWorkflowRunType())) {
            sendInfo.put("use_cache", Boolean.FALSE);
        }
        sendInfo.put("app_id", String.valueOf(application.getId()));
        sendInfo.put("session_id", session_id);
        sendInfo.put("group_id", session_id);
        sendInfo.put("question", search_text);
        sendInfo.put("client",ApplicationTypeCode.QFlow.name());
        if (MapUtils.isNotEmpty(sendInputs)) {
            sendInfo.put("inputs", sendInputs);
        }
        sendInfo.put("reference_workflow_id", workflowRunResult.getWorkflowId().toString());
        sendInfo.put("reference_workflow_run_id", workflowRunResult.getWorkflowRunId().toString());
        sendInfo.put("clean_chat_memory", Boolean.TRUE);
        OkHttpClient client = OkHttpUtil.httpClient(1, 15, TimeUnit.MINUTES, Boolean.TRUE);
        getBaseChatClient().call(client, userInfo, sendInfo);
        Map<String,String> chatParam = Maps.newHashMap();
        chatParam.put("session_id",session_id);
        List<JSONObject> chatList = chatHistoryClient.search(Lists.newArrayList("createAt"),Lists.newArrayList("ASC"),1, CommonFields.PAGE_MAX_SIZE,chatParam).getData().getList().stream().map(ChatHistory::getDetail).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(chatList)) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件运行失败，调用接口无数据", workflowRunResult.getNodeName()));
            return errorResponse;
        }
        List<JSONObject> outputs = Lists.newArrayList();
        String error = getNodeError(workflowRunResult, getBaseChatClient().getError(chatList));
        if (StringUtils.isNotBlank(error)) {
            workflowRunResult.setOutputs(WorkflowUtil.mergeOutputs(workflowRunResult, outputs));
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
//            throw new WorkflowException(error);
        }
        chatList.forEach(ChatUtil::decompressChatDetail);
        JSONObject output_chat_source = WorkflowUtil.createOutput(WorkflowJSONKey.output_chat_source.name(), WorkflowJSONKey.dict.name(), chatList);
        outputs.add(output_chat_source);
        error = getNodeError(workflowRunResult, getBaseChatClient().getError(chatList));
        if (StringUtils.isNotBlank(error)) {
            workflowRunResult.setOutputs(WorkflowUtil.mergeOutputs(workflowRunResult, outputs));
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
//            throw new WorkflowException(error);
        }
        String output_content_str = null;
        List<TraceSession> traceSessions = esClientUtil.isEsClient() ? traceSessionEsClient.getObjects(Lists.newArrayList(session_id)).getData() : traceSessionClient.getObjects(Lists.newArrayList(session_id)).getData();
        if (CollectionUtils.isNotEmpty(traceSessions)) {
            output_content_str = traceSessions.getFirst().getOutput();
        }
        List<JSONObject> ai_value = chatList.stream().filter(s -> LlmMessageType.ai.name().equalsIgnoreCase(s.getString("sender"))).collect(Collectors.toList());
        if (StringUtils.isBlank(output_content_str)) {
            List<JSONObject> output_content_value = ai_value.stream().filter(s -> "stop".equalsIgnoreCase(s.getString("action")) || "Final Answer".equalsIgnoreCase(s.getString("action"))).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(output_content_value)) {
                output_content_str = output_content_value.getLast().getString("data");
            }
        }
        if (StringUtils.isBlank(output_content_str)) {
            error = "总结答案无内容";
            LOG.error("workflowRunResultId={} nodeName={} error={}", workflowRunResult.getId(), workflowRunResult.getNodeName(), error);
            error = String.format("（%s）组件输出结果为空", workflowRunResult.getNodeName());
            workflowRunResult.setOutputs(WorkflowUtil.mergeOutputs(workflowRunResult, outputs));
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
//            throw new WorkflowException(error);
        }
        JSONObject output_table = WorkflowUtil.getTableOutputs(ai_value.stream().filter(s -> "结构化数据查询结果".equalsIgnoreCase(s.getString("result_type")) && StringUtils.isNotBlank(s.getString("data"))).map(s -> s.getString("data")).collect(Collectors.toList()));
        outputs.add(output_table);
        outputs.addAll(WorkflowUtil.getTableLlmOutputs(output_content_str));
        JSONObject output_content = WorkflowUtil.createOutput(WorkflowJSONKey.output_content.name(), WorkflowJSONKey.str.name(), output_content_str);
        outputs.add(output_content);
        return Response.ok(outputs);
    }

    protected String getNodeError(WorkflowRunResult workflowRunResult, String error) {
        if (StringUtils.isBlank(error)) {
            return StringUtils.EMPTY;
        }
        if (error.contains("连接超时")) {
            error = String.format("（%s）组件连接超时", workflowRunResult.getNodeName());
        } else if (error.contains("工具调用出错")) {
            error = String.format("（%s）工具调用出错", workflowRunResult.getNodeName());
        } else if (error.contains("重连模型仍超时")) {
            error = String.format("（%s）重连模型仍超时", workflowRunResult.getNodeName());
        } else if (error.contains("模型加载出错")) {
            error = String.format("（%s）模型加载出错", workflowRunResult.getNodeName());
        } else {
            error = String.format("（%s）组件请求失败，%s", workflowRunResult.getNodeName(), error);
        }
        return error;
    }


    @Override
    public String getOutputTextParameterName() {
        return WorkflowJSONKey.output_content.name();
    }

    protected abstract JSONObject getSendInfo(WorkflowRunResult workflowRunResult, String searchText) throws Exception;

    protected abstract BaseOpenChatClient getBaseChatClient() throws Exception;

}
