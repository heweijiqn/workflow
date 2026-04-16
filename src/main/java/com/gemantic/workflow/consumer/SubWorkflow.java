package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.gemantic.gpt.constant.*;
import com.gemantic.gpt.model.TraceSession;
import com.gemantic.gpt.model.Workflow;
import com.gemantic.gpt.model.WorkflowRun;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.workflow.ReRunResult;
import com.gemantic.gpt.support.workflow.WorkflowData;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.MsgUtil;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 调用工作流
 */
@Component("subWorkflow")
public class SubWorkflow extends SubWorkflowBase {

    @Resource
    @Qualifier("subWorkflowExecutorService")
    private ExecutorService executorService;

    @Override
    protected ExecutorService getExecutorService() {
        return executorService != null ? executorService : super.getExecutorService();
    }

    @Value("${CONSUMER_SUB_WORKFLOW_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize(){
        return MoreObjects.firstNonNull(consumerBatchSize,super.getConsumerBatchSize());
    }


    @Override
    protected Response<Void> saveWorkflowResult(WorkflowRunResult workflowRunResult, Workflow workflow, List<JSONObject> parameters_mapping, JSONObject inputs) throws Exception {
        JSONObject subWorkflowInputs = WorkflowUtil.getSubWorkflowInputs(inputs, null, parameters_mapping, workflow.getInputs()).getFirst();
        workflow.setInputs(subWorkflowInputs);
        workflowRunResult.setSubWorkflowInputsMd5(WorkflowUtil.getInputsMd5(subWorkflowInputs));
        Map<String, WorkflowRunResult> workflowRunResultMap = workflowRepository.getWorkflowRunResult(workflowRunResult.getMode(), workflow, subWorkflowInputs, workflowRunResult.getWorkflowRunType());
        WorkflowRun run = null;
        if (WorkflowRunType.DEBUG.name().equalsIgnoreCase(workflowRunResult.getWorkflowRunType())) {
            run = new WorkflowRun();
            run.setWorkflowId(workflowRunResult.getWorkflowId());
            run.setId(0L);
            run.setUserGroupId(workflowRunResult.getUserGroupId());
            run.setUserId(workflowRunResult.getUserId());
            run.setUserName(workflowRunResult.getUserName());
            run.setAppId(workflowRunResult.getAppId());
            run.setChannel(workflowRunResult.getChannel());
            run.setUserInfo(workflowRunResult.getUserInfo());
        } else {
            run = workflowRunClient.getById(workflowRunResult.getWorkflowRunId()).getData();
            if (null == run) {
                return Response.error(null, "运行记录已不存在");
            }
        }
        run.setType(workflowRunResult.getWorkflowRunType());
        run.setMode(workflowRunResult.getMode());
        List<WorkflowRunResult> children = workflowRunResultMap.values().stream().toList();
        children.forEach(c -> {
            c.setParentId(workflowRunResult.getId());
            c.setParentNodeId(workflowRunResult.getNodeId());
            c.setParentNodeName(workflowRunResult.getNodeName());
        });
        workflowRepository.saveWorkflowRunResult(run, children);
        return Response.ok();
    }


    @Override
    public TraceSession reRun(WorkflowRunResult workflowRunResult, ReRunResult data) throws Exception {
        TraceSession traceSession = new TraceSession();
        traceSession.setStart_time(DateTime.now().getMillis());
        traceSession.setTags(Lists.newArrayList(WorkflowReRunType.重新生成.name()));
        JSONObject inputs = MoreObjects.firstNonNull(workflowRunResult.getInputs(), new JSONObject());
        traceSession.setInputs(inputs);
        WorkflowRunResult workflowFrom = workflowRunResult;
        JSONObject subWorkflowInputs = workflowRunResult.getInputs();
        if(WorkflowNodeType.sub_workflow_list.name().equalsIgnoreCase(workflowRunResult.getNodeType())){
            // 优化：直接查询完整的父节点信息，避免先 find 再 getById 的两次查询
            WorkflowRunResult parentNode = workflowRunResultClient.getById(workflowRunResult.getParentId()).getData();
            if (null == parentNode || null == parentNode.getParentId() || parentNode.getParentId() <= 0L){
                traceSession.setEnd_time(DateTime.now().getMillis());
                traceSession.setStatus(TraceSessionStatus.FAILED.getStatus());
                traceSession.setError("循环调用工作流运行节点不存在");
                return traceSession;
            }
            // 优化：直接查询完整的 workflowFrom 节点信息
            workflowFrom = workflowRunResultClient.getById(parentNode.getParentId()).getData();
            if(null == workflowFrom){
                traceSession.setEnd_time(DateTime.now().getMillis());
                traceSession.setStatus(TraceSessionStatus.FAILED.getStatus());
                traceSession.setError(String.format("循环调用工作流运行节点ID\"%s\"不存在", parentNode.getParentId()));
                return traceSession;
            }
       }
       Response<Workflow> workflowResponse = getWorkflow(workflowFrom);
       if(!MsgUtil.isValidMessage(workflowResponse)){
           traceSession.setEnd_time(DateTime.now().getMillis());
           traceSession.setStatus(TraceSessionStatus.FAILED.getStatus());
           traceSession.setError(workflowResponse.getMessage().getMessage());
           return traceSession;
       }
        Workflow workflow = workflowResponse.getData();
        if(WorkflowNodeType.sub_workflow.name().equalsIgnoreCase(workflowRunResult.getNodeType())){
            subWorkflowInputs = workflow.getInputs();
        }
        workflowRepository.deleteChildrenWorkflowRunResult(workflowRunResult.getWorkflowId(),workflowRunResult.getWorkflowRunId(),Lists.newArrayList(workflowRunResult.getId()),workflowRunResult.getWorkflowRunType());
        Map<String, WorkflowRunResult> workflowRunResultMap = workflowRepository.getWorkflowRunResult(workflowRunResult.getMode(), workflow, subWorkflowInputs, workflowRunResult.getWorkflowRunType());
        List<WorkflowRunResult> children = workflowRunResultMap.values().stream().toList();
        for(WorkflowRunResult c : children){
            c.setParentId(workflowRunResult.getId());
            c.setParentNodeId(workflowRunResult.getNodeId());
            c.setParentNodeName(workflowRunResult.getNodeName());
        }
        WorkflowRun workflowRun = workflowRunClient.getById(workflowRunResult.getWorkflowRunId()).getData();
        workflowRun.setMode(WorkflowRunMode.REALTIME.name());
        workflowRun.setType(WorkflowRunType.RERUN.name());
        workflowRepository.saveWorkflowRunResult(workflowRun, children);
        Map<String, String> subParam = Maps.newHashMap();
        subParam.put("parentId", String.valueOf(workflowRunResult.getId()));
        subParam.put("workflowRunType", workflowRunResult.getWorkflowRunType());
        subParam.put("position", String.join(",", Position.ROOT.name(), Position.LEAF.name()));
        List<WorkflowRunResult> firstNodes = workflowRunResultClient.find(1, Integer.MAX_VALUE, subParam).getData().getList();
        if (firstNodes.size() > 1) {
            firstNodes = firstNodes.stream().filter(f -> Position.ROOT.name().equals(f.getPosition())).collect(Collectors.toList());
        }
        workflowRepository.pushWorkFlowRunResult(firstNodes,Boolean.FALSE);
        traceSession.setEnd_time(DateTime.now().getMillis());
        workflowRunResult.setInputs(inputs);
        workflowRunResult = workflowRunResultClient.getById(workflowRunResult.getId()).getData();
        if(WorkflowRunStatus.FINISHED.name().equalsIgnoreCase(workflowRunResult.getStatus())){
            traceSession.setStatus(TraceSessionStatus.CORRECT_OR_SUCCESS.getStatus());
        }else if(WorkflowRunStatus.FAILED.name().equalsIgnoreCase(workflowRunResult.getStatus())) {
            traceSession.setStatus(TraceSessionStatus.FAILED.getStatus());
        }
        traceSession.setInput(workflowRunResult.getSubWorkflowRunInputText());
        traceSession.setOutputs(workflowRunResult.getOutputs());
        return traceSession;
    }


}
