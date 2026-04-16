package com.gemantic.workflow.repository;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.model.*;
import com.gemantic.gpt.support.notify.NotifyConfig;
import com.gemantic.gpt.support.workflow.*;
import com.gemantic.springcloud.model.Response;
import com.gemantic.workflow.consumer.BaseConsumer;

import java.util.List;
import java.util.Map;

public interface WorkflowRepository {

//    Response<Long> runWorkflow(String channel, String userGroupId, String userId, String userName, Workflow workflow, JSONObject inputs) throws Exception;

    Response<Long> runWorkflow(RunInputs runInputs) throws Exception;


    Response<OpenRunOutput> runWorkflowInMemory(OpenRunInput data) throws Exception;

    Response<WorkflowPlan> getPlan(WorkflowVersion workflowVersion) throws Exception;

    Response<Void> getPlan(Map<String,WorkflowRunResult> plan, Integer
            degree, String source, List<Edge> edges, Map<String, Node> runNodes, JSONObject
                                startParameterInputs) throws Exception;

    Response<Long> initRunWorkflow(RunInputs runInputs, List<WorkflowRunResult> firsetNodes) throws Exception;

    CopyRunResult getWorkflowRunResultCopy(Long workflowId, String outputType, String workflowInputMd5, String workflowDataMd5, Long copyTimeout) throws Exception;

    CopyRunResult getWorkflowRunResultCopyByRun(Long workflowId, String outputType, String workflowInputsMd5, String workflowDataMd5, Long copyTimeout) throws Exception;

    CopyRunResult getWorkflowRunResultCopyBySubworkflow(Long workflowId, String workflowInputsMd5, String workflowDataMd5, Long copyTimeout) throws Exception;

    Map<String, WorkflowRunResult> getWorkflowRunResult(String mode, Workflow data, JSONObject inputs, String type) throws Exception;

    Map<String, WorkflowRunResult> getWorkflowRunResult(String mode, String workflowName, WorkflowData data, JSONObject inputs, String type) throws Exception;

    void updateWorkflowRunResultLeafFinish(Long finishAt, WorkflowRunResult workflowRunResult) throws Exception;
    void updateWorkflowRunResultBranchFinish(Long finishAt, WorkflowRunResult parent) throws Exception;
    void saveWorkflowRunResult(WorkflowRun workflowRun, List<WorkflowRunResult> workflowRunResults) throws Exception;

    void updateWorkflowRunStatusByQueque(List<WorkflowRunResult> workflowRunResults, Boolean force) throws Exception;

    void updateWorkflowRunStatusByRunning(WorkflowRunResult workflowRunResult) throws Exception;

    void updateWorkflowRunStatusByFailure(WorkflowRunResult workflowRunResult, String errorInfo,Long finishAt) throws Exception;

    void updateWorkflowRunFinish(WorkflowRunResult workflowRunResult,Long finishAt) throws Exception;


    void pushWorkFlowRunResult(List<WorkflowRunResult> workflowRunResults, Boolean force) throws Exception;

    List<Long> pushTargetNodes(WorkflowRunResult workflowRunResult) throws Exception;


    List<WorkflowRunResult> getWorkflowRunResult(Long workflowRunId, Long parentId, List<String> nodeIds, List<String> status, List<String> position, String workflowRunType) throws Exception;

    Integer getWorkflowRunNodeCount(Long workflowRunId, String status, String workflowRunType) throws Exception;

    Response<Long> notify(WorkflowRun workflowRun);

    void initNotifyConfig(String channel, Long appId, NotifyConfig notifyConfig);



    void updateRunNodeCount(Long workflowRunId, String workflowRunType) throws Exception;


    WorkflowRunResult getSubWorkflowRunResult(String workflowRunType, String mode, Workflow subWorkflow, JSONObject subWorkflowInputs, WorkflowData subWorkflowData) throws Exception;

    WorkflowRunResult getWorkflowRunResultStatus(Long workflowRunResultId) throws Exception;

    void updateWorkflowRunResult(List<Long> workflowRunResultIds, Long now, String status, String error, Boolean force) throws Exception;

    WorkflowRun getWorkflowRunStatus(Long workflowRunId) throws Exception;

    void updateWorkflowRun(List<Long> workflowRunId, Long now, String status) throws Exception;

    void runNode(WorkflowRunResult workflowRunResult, BaseConsumer baseConsumer);

    void fillRunResults(String mode, Map<String, WorkflowRunResult> runResults, Integer
            degree, String source, List<Edge> edges, Map<String, Node> runNodes, JSONObject
                                        startParameterInputs, String workflowRunType) throws Exception;

    void deleteChildrenWorkflowRunResult(Long workflowId,Long workflowRunId, List<Long> parentIds, String workflowRunType) throws Exception;


}
