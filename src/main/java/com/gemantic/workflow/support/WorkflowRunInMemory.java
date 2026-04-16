package com.gemantic.workflow.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.gemantic.gpt.client.WorkflowPlanClient;
import com.gemantic.gpt.constant.Position;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.constant.WorkflowNodeType;
import com.gemantic.gpt.constant.WorkflowRunStatus;
import com.gemantic.gpt.model.WorkflowPlan;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.workflow.Edge;
import com.gemantic.gpt.support.workflow.WorkflowException;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.model.ResponseMessage;
import com.gemantic.springcloud.user.UserInfo;
import com.gemantic.springcloud.utils.MsgUtil;
import com.gemantic.workflow.consumer.BaseConsumer;
import com.gemantic.workflow.repository.ConsumerRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Data
public class WorkflowRunInMemory implements Callable<Response<JSONObject>> {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowRunInMemory.class);
    private static final TypeReference<LinkedHashMap<String,WorkflowRunResult>> PLAN_TYPEREFERENCE = new TypeReference<LinkedHashMap<String, WorkflowRunResult>>() {};
    private JSONObject inputs;
    private WorkflowPlan workflowPlan;
    private String workflowRunId;
    private ConsumerRepository consumerRepository;
    private WorkflowPlanClient workflowPlanClient;
    private ExecutorService nodeExecutorService;
    private Boolean nodeConcurrent = Boolean.TRUE;
    private Integer runNodeCount = 0;

    private Integer runNodeSuccessCount = 0;

    private Integer runNodeErrorCount = 0;

    @Schema(description = "总token数")
    private final AtomicLong totalTokens = new AtomicLong(0L);

    @Schema(description = "输出token数")
    private final AtomicLong outputTokens = new AtomicLong(0L);

    @Schema(description = "输入token数")
    private final AtomicLong inputTokens = new AtomicLong(0L);

    @Schema(description = "首token耗时，单位：毫秒")
    private final AtomicLong firstTokenLatency = new AtomicLong(0L);

    @Schema(description = "输出token耗时，单位：毫秒")
    private final AtomicLong tokenTime = new AtomicLong(0L);

    private UserInfo userInfo;

    @Schema(description = "开始运行时间")
    private Long runAt = 0L;

    @Schema(description = "结束运行时间")
    private Long finishAt = 0L;

    @Override
    public Response<JSONObject> call() {
        this.runAt = DateTime.now().getMillis();
        try {
            Map<String, WorkflowRunResult> plan = PLAN_TYPEREFERENCE.to(workflowPlan.getPlan());
            this.runNodeCount += plan.size();

            // 初始化节点的 userInfo / appId / workflowId
            for (WorkflowRunResult r : plan.values()) {
                r.setChannel(userInfo.getChannel());
                r.setUserId(userInfo.getUserId());
                r.setUserGroupId(userInfo.getUserGroupId());
                r.setUserName(userInfo.getUserName());
                r.setWorkflowRunId(0L);
                r.setAppId(Long.valueOf(workflowPlan.getAppId()));
                r.setWorkflowId(Long.valueOf(workflowPlan.getId()));
                r.setOutputs(null);
                r.setErrorInfo(null);
            }

            // 用 LangGraph4j StateGraph 执行工作流
            WorkflowGraphBuilder graphBuilder = new WorkflowGraphBuilder(consumerRepository);
            var stateGraph = graphBuilder.buildGraph(workflowPlan, plan);
            var compiledGraph = stateGraph.compile();

            // 初始输入
            Map<String, Object> initData = new java.util.HashMap<>();
            initData.put(WorkflowState.RUN_RESULTS, plan);
            initData.put(WorkflowState.INPUTS, inputs);

            // 流式执行，消费所有节点输出
            var nodeOutputStream = compiledGraph.stream(initData);
            nodeOutputStream.forEach(output -> {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("LangGraph4j node output: {}", output);
                }
            });

            // 统计结果
            this.runNodeSuccessCount += (int) plan.values().stream()
                .filter(p -> "FINISHED".equalsIgnoreCase(p.getStatus()) || "NOT_RUN".equalsIgnoreCase(p.getStatus()))
                .count();
            this.runNodeErrorCount += (int) plan.values().stream()
                .filter(p -> "FAILED".equalsIgnoreCase(p.getStatus()))
                .count();

            // 累计 token 统计
            for (WorkflowRunResult r : plan.values()) {
                this.tokenTime.addAndGet(defaultLong(r.getTokenTime()));
                this.totalTokens.addAndGet(defaultLong(r.getTotalTokens()));
                this.inputTokens.addAndGet(defaultLong(r.getInputTokens()));
                this.outputTokens.addAndGet(defaultLong(r.getOutputTokens()));
                this.firstTokenLatency.addAndGet(defaultLong(r.getFirstTokenLatency()));
            }

            // 获取输出节点结果
            WorkflowRunResult endNode = plan.get(workflowPlan.getEndNodeIds().getFirst());
            JSONObject finalOutputs = endNode != null ? endNode.getOutputs() : null;
            if (finalOutputs == null && endNode != null) {
                finalOutputs = WorkflowUtil.mergeOutputs(endNode, Lists.newArrayList());
            }
            workflowPlan.setPlan(WorkflowUtil.getPlan(plan));

            if (endNode != null && "FAILED".equalsIgnoreCase(endNode.getStatus())) {
                Response<JSONObject> result = Response.error(finalOutputs);
                result.getMessage().setMessage(endNode.getErrorInfo());
                return result;
            }
            return Response.ok(finalOutputs);

        } catch (Exception e) {
            LOG.error("工作流内存运行失败 workflowId={}", workflowPlan.getId(), e);
            Response<JSONObject> result = Response.error(new JSONObject());
            result.getMessage().setMessage(e.getMessage());
            return result;
        } finally {
            this.finishAt = DateTime.now().getMillis();
        }
    }


    protected Response<Void> call(Map<String,WorkflowRunResult> plan,List<String> runNodeIds){
        if (CollectionUtils.isEmpty(runNodeIds)) {
            return Response.ok();
        }
        if (Boolean.TRUE.equals(nodeConcurrent) && nodeExecutorService != null && runNodeIds.size() > 1) {
            return callNodesConcurrently(plan, runNodeIds);
        }
        return callNodesSequentially(plan, runNodeIds);
    }

    protected Response<Void> callNodesSequentially(Map<String,WorkflowRunResult> plan, List<String> runNodeIds) {
        for(String runNodeId : runNodeIds){
            if(null == plan.get(runNodeId)){
                continue;
            }
            WorkflowRunResult workflowRunResult = plan.get(runNodeId);
            workflowRunResult.setAppId(Long.valueOf(workflowPlan.getAppId()));
            Response<Void> callNode = call(plan,workflowRunResult);
            if(!MsgUtil.isValidMessage(callNode)){
                return callNode;
            }
            List<String> tagetNodeIds = getTagetNodeIds(workflowRunResult);
            Response<Void> callNodes = call(plan,tagetNodeIds);
            if(!MsgUtil.isValidMessage(callNodes)){
                return callNodes;
            }
        }
        return Response.ok();
    }

    protected Response<Void> callNodesConcurrently(Map<String, WorkflowRunResult> plan, List<String> runNodeIds) {
        List<Future<NodeTaskResult>> futures = Lists.newArrayList();
        for (String runNodeId : runNodeIds) {
            WorkflowRunResult workflowRunResult = plan.get(runNodeId);
            if (workflowRunResult == null) {
                continue;
            }
            futures.add(nodeExecutorService.submit(() -> {
                workflowRunResult.setAppId(Long.valueOf(workflowPlan.getAppId()));
                Response<Void> callNode = call(plan, workflowRunResult);
                return new NodeTaskResult(workflowRunResult, callNode);
            }));
        }
        LinkedHashSet<String> targetNodeIds = new LinkedHashSet<>();
        for (Future<NodeTaskResult> future : futures) {
            NodeTaskResult taskResult;
            try {
                taskResult = future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Response<Void> response = Response.error();
                response.getMessage().setMessage("workflow node task interrupted");
                return response;
            } catch (ExecutionException e) {
                Response<Void> response = Response.error();
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                response.getMessage().setMessage(cause.getMessage());
                return response;
            }
            if(!MsgUtil.isValidMessage(taskResult.getResponse())){
                return taskResult.getResponse();
            }
            targetNodeIds.addAll(getTagetNodeIds(taskResult.getWorkflowRunResult()));
        }
        return call(plan, Lists.newArrayList(targetNodeIds));
    }

    protected Response<Void> call(Map<String,WorkflowRunResult> plan,WorkflowRunResult workflowRunResult){
        if(WorkflowUtil.WORKFLOW_RUN_STATUS_FINISHED.contains(workflowRunResult.getStatus())){
            return Response.ok();
        }
        BaseConsumer consumer = consumerRepository.getConsumer(workflowRunResult.getNodeType());
        String error = null;
        Response<Void> errorResponse = Response.error();
        if(null == consumer){
            workflowRunResult.setStatus(WorkflowRunStatus.FAILED.name());
            error = String.format("（%s）组件已废弃，请更换其他组件", workflowRunResult.getNodeName());
            workflowRunResult.setErrorInfo(error);
            workflowRunResult.setFinishAt(DateTime.now().getMillis());
            if(workflowRunResult.getIgnoreError() != 1){
                errorResponse.getMessage().setMessage(error);
                return errorResponse;
            }
            return Response.ok();
        }
        try {
            JSONObject inputs = getInputs(plan,workflowRunResult,consumer);
            if(null == inputs){
                return Response.ok();
            }
            workflowRunResult.setInputs(inputs);
            workflowRunResult.setRunAt(DateTime.now().getMillis());
            workflowRunResult.setStatus(WorkflowRunStatus.RUNNING.name());
            workflowRunResult.setChannel(userInfo.getChannel());
            workflowRunResult.setUserId(userInfo.getUserId());
            workflowRunResult.setUserGroupId(userInfo.getUserGroupId());
            workflowRunResult.setWorkflowRunId(0L);
            workflowRunResult.setAppId(Long.valueOf(workflowPlan.getAppId()));
            workflowRunResult.setWorkflowId(Long.valueOf(workflowPlan.getId()));
            workflowRunResult.setUserName(userInfo.getUserName());
            workflowRunResult.setOutputs(null);
            workflowRunResult.setErrorInfo(null);
            Response<Void> response = Response.ok();
            if(workflowRunResult.getNodeType().startsWith(WorkflowNodeType.sub_workflow.name())){
                Response<JSONObject> outputs = callSubWorkflow(workflowRunResult);
                if(null == outputs || null == outputs.getMessage()){
                    response.setMessage(ResponseMessage.error(200,-1,String.format("（%s）组件输出结果为空", workflowRunResult.getNodeName())));
                }else {
                    response.setMessage(response.getMessage());
                    workflowRunResult.setOutputs(outputs.getData());
                }
            }else {
                Response<List<JSONObject>> outputResultResponse = consumer.getOutputs(workflowRunResult);
                if (MsgUtil.isValidMessage(outputResultResponse)) {
                    JSONObject outputs = WorkflowUtil.mergeOutputs(workflowRunResult, outputResultResponse.getData());
                    workflowRunResult.setOutputs(outputs);
                }else if(null == outputResultResponse || null == outputResultResponse.getMessage()){
                    response.setMessage(ResponseMessage.error(200,-1,String.format("（%s）组件输出结果为空", workflowRunResult.getNodeName())));
                }else {
                    response.setMessage(outputResultResponse.getMessage());
                }
            }
            if(!MsgUtil.isValidMessage(response)){
                error = response.getMessage().getMessage();
                LOG.error("节点运行失败(内存运行模式) 节点={}({}) 工作流运行id={} 工作流id={} 失败原因={}"
                        , workflowRunResult.getNodeName(), workflowRunResult.getNodeId(), workflowRunId, workflowPlan.getId(), error);
            }
        }catch (Exception e){
            if(e instanceof WorkflowException){
                error = e.getMessage();
                LOG.error("节点运行失败(内存运行模式) 节点={}({}) 工作流运行id={} 工作流id={} 失败原因={}"
                        ,workflowRunResult.getNodeName(), workflowRunResult.getNodeId(),workflowRunId , workflowPlan.getId(), error);
            }else  {
                error = String.format("（%s）组件运行出错:%s", workflowRunResult.getNodeName(),e.getMessage());
                LOG.error("节点运行失败(内存运行模式) 节点={}({}) 工作流运行id={} 工作流id={} 失败原因={}"
                        ,workflowRunResult.getNodeName(), workflowRunResult.getNodeId(),workflowRunId , workflowPlan.getId(), error,e);
            }
        }
        workflowRunResult.setFinishAt(DateTime.now().getMillis());
        this.tokenTime.addAndGet(defaultLong(workflowRunResult.getTokenTime()));
        this.totalTokens.addAndGet(defaultLong(workflowRunResult.getTotalTokens()));
        this.inputTokens.addAndGet(defaultLong(workflowRunResult.getInputTokens()));
        this.outputTokens.addAndGet(defaultLong(workflowRunResult.getOutputTokens()));
        this.firstTokenLatency.addAndGet(defaultLong(workflowRunResult.getFirstTokenLatency()));
        if(StringUtils.isNotBlank(error)){
            workflowRunResult.setStatus(WorkflowRunStatus.FAILED.name());
            workflowRunResult.setErrorInfo(error);
            if(MapUtils.isEmpty(workflowRunResult.getOutputs()) && WorkflowUtil.isOutputNode(workflowRunResult.getNodeType())){
                JSONObject outputs = WorkflowUtil.mergeOutputs(workflowRunResult, Lists.newArrayList());
                workflowRunResult.setOutputs(outputs);
            }
            if(workflowRunResult.getIgnoreError() != 1){
                errorResponse.getMessage().setMessage(error);
                return errorResponse;
            }
            callError(plan, workflowRunResult);
        }else {
            workflowRunResult.setStatus(WorkflowRunStatus.FINISHED.name());
            LOG.warn("节点运行成功(内存运行模式) 节点={}({}) 工作流运行id={} 工作流id={}"
                    , workflowRunResult.getNodeName(), workflowRunResult.getNodeId(), workflowRunId, workflowPlan.getId());
            callFinished(plan, workflowRunResult);
        }
        return Response.ok();
    }

    protected List<String> getTagetNodeIds(WorkflowRunResult workflowRunResult) {
        return WorkflowUtil.getTagetNodeIds(workflowRunResult);
    }

    public Long getTotalTokens() {
        return totalTokens.get();
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens.set(defaultLong(totalTokens));
    }

    public Long getOutputTokens() {
        return outputTokens.get();
    }

    public void setOutputTokens(Long outputTokens) {
        this.outputTokens.set(defaultLong(outputTokens));
    }

    public Long getInputTokens() {
        return inputTokens.get();
    }

    public void setInputTokens(Long inputTokens) {
        this.inputTokens.set(defaultLong(inputTokens));
    }

    public Long getFirstTokenLatency() {
        return firstTokenLatency.get();
    }

    public void setFirstTokenLatency(Long firstTokenLatency) {
        this.firstTokenLatency.set(defaultLong(firstTokenLatency));
    }

    public Long getTokenTime() {
        return tokenTime.get();
    }

    public void setTokenTime(Long tokenTime) {
        this.tokenTime.set(defaultLong(tokenTime));
    }

    protected void callFinished(Map<String,WorkflowRunResult> plan,WorkflowRunResult workflowRunResult){
        if(WorkflowNodeType.if_else.name().equalsIgnoreCase(workflowRunResult.getNodeType())){
            List<JSONObject> if_else_template = DataParametersUtil.getValuesByTemplate(WorkflowJSONKey.if_else_template.name(),workflowRunResult.getInputs(), JSONObject.class,Lists.newArrayList());
            if(CollectionUtils.isEmpty(if_else_template)){
                workflowRunResult.setStatus(WorkflowRunStatus.FAILED.name());
                workflowRunResult.setErrorInfo(String.format("（%s）组件无法运行，请输入条件配置", workflowRunResult.getNodeName()));
                callError(plan, workflowRunResult);
                return;
            }
            JSONObject ifElseValue = DataParametersUtil.getValueByTemplate(WorkflowJSONKey.output.name(), workflowRunResult.getOutputs(), JSONObject.class,new JSONObject());
            String branchId = ifElseValue.getString(WorkflowJSONKey.branchId.name());
            if(StringUtils.isBlank(branchId)){
                workflowRunResult.setStatus(WorkflowRunStatus.FAILED.name());
                workflowRunResult.setErrorInfo(String.format("（%s）组件运行输出异常,branchId为空", workflowRunResult.getNodeName()));
                callError(plan, workflowRunResult);
                return;
            }
            List<Edge> tagetEdge = WorkflowUtil.getTagetEdge(workflowRunResult);
            List<String> runTargetNodeIds = tagetEdge.stream().filter(e->branchId.equalsIgnoreCase(e.getSourceHandle())).map(Edge::getTarget).distinct().toList();
            List<String> notRunTargetNodeIds = tagetEdge.stream().map(Edge::getTarget).filter(e->!runTargetNodeIds.contains(e)).distinct().collect(Collectors.toList());
            if(CollectionUtils.isNotEmpty(notRunTargetNodeIds)){
                List<String> flowNodeIds = WorkflowUtil.getFlowNodeIds(plan,notRunTargetNodeIds,Boolean.TRUE,workflowPlan.getEndNodeIds(),Boolean.FALSE);
                updateNotRunStatus(plan,flowNodeIds);
            }
        }
    }



    protected void callError(Map<String,WorkflowRunResult> plan,WorkflowRunResult workflowRunResult){
        if(WorkflowNodeType.if_else.name().equalsIgnoreCase(workflowRunResult.getNodeType())){
            List<String> flowNodeIds = WorkflowUtil.getFlowNodeIds(plan,Lists.newArrayList(workflowRunResult.getNodeId()),Boolean.FALSE,workflowPlan.getEndNodeIds(),Boolean.FALSE);
            updateNotRunStatus(plan,flowNodeIds);
        }
    }

    public void updateNotRunStatus(Map<String,WorkflowRunResult> plan,List<String> nodeIds){
        for(String nodeId : nodeIds){
            if(null == plan.get(nodeId)){
                continue;
            }
            WorkflowRunResult run = plan.get(nodeId);
            if(WorkflowUtil.WORKFLOW_RUN_STATUS_FINISHED.contains(run.getStatus())){
                continue;
            }
            if(WorkflowUtil.isOutputNode(run.getNodeType())){
                continue;
            }
            plan.get(nodeId).setStatus(WorkflowRunStatus.NOT_RUN.name());
        }
    }

    protected Response<JSONObject> callSubWorkflow(WorkflowRunResult workflowRunResult) throws Exception{
        JSONObject subWorkflow = DataParametersUtil.getValueByTemplate(WorkflowJSONKey.subWorkflow.name(), workflowRunResult.getInputs(), JSONObject.class, null);
        Response<JSONObject> errorResponse = Response.error(null);
        if(MapUtils.isEmpty(subWorkflow) || StringUtils.isBlank(subWorkflow.getString(WorkflowJSONKey.id.name()))){
            errorResponse.getMessage().setMessage(String.format("（%s）组件没有选择工作流", workflowRunResult.getNodeName()));
            return errorResponse;
        }
        List<WorkflowPlan> workflowPlans = workflowPlanClient.search(Lists.newArrayList(subWorkflow.getString(WorkflowJSONKey.id.name()))).getData().getList();
        if(CollectionUtils.isEmpty(workflowPlans)){
            errorResponse.getMessage().setMessage(String.format("（%s）组件选择的工作流\"%s\"不支持内存运行模式，输出组件必须为\"数据输出\"，工作流标签设置\"支持内存运行模式\"，重新发布后才能支持",workflowRunResult.getNodeName(),subWorkflow.getString(WorkflowJSONKey.name.name())));
            return errorResponse;
        }
        WorkflowPlan workflowPlan = workflowPlans.getFirst();
        workflowRunResult.setSubWorkflowId(Long.valueOf(workflowPlan.getId()));
        workflowRunResult.setAppId(Long.valueOf(workflowPlan.getAppId()));
        workflowRunResult.setSubWorkflowOutputType(workflowPlan.getOutputType());
        List<JSONObject> parameters_mapping = DataParametersUtil.getValuesByTemplate(WorkflowJSONKey.parameters_mapping.name(), workflowRunResult.getInputs(), JSONObject.class, Lists.newArrayList());
        List<JSONObject> standard_chart = DataParametersUtil.getValuesByTemplate(WorkflowJSONKey.standard_chart.name(), workflowRunResult.getInputs(), JSONObject.class, Lists.newArrayList());
        if (CollectionUtils.isEmpty(parameters_mapping) && MapUtils.isNotEmpty(workflowPlan.getInputs())) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件替换变量为空", workflowRunResult.getNodeName()));
            return errorResponse;
        }
        if(WorkflowNodeType.sub_workflow_list.name().equalsIgnoreCase(workflowRunResult.getNodeType()) && CollectionUtils.isEmpty(standard_chart)){
            errorResponse.getMessage().setMessage(String.format("（%s）组件列表数据为空", workflowRunResult.getNodeName()));
            return errorResponse;
        }
        List<JSONObject> subWorkflowInputsList = WorkflowUtil.getSubWorkflowInputs(workflowRunResult.getInputs(),standard_chart,parameters_mapping,workflowPlan.getInputs());
        List<WorkflowRunInMemory> subWorkflowRuns = subWorkflowInputsList.stream().map(subWorkflowInputs->{
            WorkflowRunInMemory workflowRunInMemory = new WorkflowRunInMemory();
            WorkflowPlan workflowPlanTmp = new WorkflowPlan();
            BeanUtils.copyProperties(workflowPlan,workflowPlanTmp);
            workflowPlanTmp.setPlan(JSON.copy(workflowPlanTmp.getPlan()));
            workflowPlanTmp.setInputs(JSON.copy(workflowPlanTmp.getInputs()));
            workflowRunInMemory.setWorkflowPlan(workflowPlanTmp);
            workflowRunInMemory.setWorkflowPlanClient(getWorkflowPlanClient());
            workflowRunInMemory.setConsumerRepository(getConsumerRepository());
            workflowRunInMemory.setWorkflowRunId(getWorkflowRunId());
            workflowRunInMemory.setInputs(subWorkflowInputs);
            workflowRunInMemory.setUserInfo(getUserInfo());
            workflowRunInMemory.setNodeExecutorService(getNodeExecutorService());
            workflowRunInMemory.setNodeConcurrent(getNodeConcurrent());
            return workflowRunInMemory;
        }).toList();
        List<SubWorkflowTaskResult> subWorkflowTaskResults = runSubWorkflowTasks(subWorkflowRuns);
        Map<String,WorkflowRunResult> subWorkflowListRun = Maps.newLinkedHashMap();
        List<Object> outputValue = Lists.newArrayList();
        for(SubWorkflowTaskResult taskResult : subWorkflowTaskResults){
            int i = taskResult.getIndex();
            WorkflowRunInMemory subWorkflowRun = taskResult.getSubWorkflowRun();
            Response<JSONObject> subWorkflowOutputs = taskResult.getResponse();
            this.runNodeCount+= subWorkflowRun.getRunNodeCount();
            this.runNodeSuccessCount+=subWorkflowRun.getRunNodeSuccessCount();
            this.runNodeErrorCount+= subWorkflowRun.getRunNodeErrorCount();
            workflowRunResult.setTotalTokens(workflowRunResult.getTotalTokens()+subWorkflowRun.getTotalTokens());
            workflowRunResult.setFirstTokenLatency(workflowRunResult.getFirstTokenLatency()+subWorkflowRun.getFirstTokenLatency());
            workflowRunResult.setInputTokens(workflowRunResult.getInputTokens()+subWorkflowRun.getInputTokens());
            workflowRunResult.setOutputTokens(workflowRunResult.getOutputTokens()+subWorkflowRun.getOutputTokens());
            workflowRunResult.setTokenTime(workflowRunResult.getTokenTime()+subWorkflowRun.getTokenTime());
            if(WorkflowNodeType.sub_workflow.name().equalsIgnoreCase(workflowRunResult.getNodeType())){
                workflowRunResult.setSubWorkflowRunInputText(WorkflowUtil.getInputText(subWorkflowRun.getInputs()));
                workflowRunResult.setSubWorkflowRunResult(PLAN_TYPEREFERENCE.to(subWorkflowRun.getWorkflowPlan().getPlan()));
                return subWorkflowOutputs;
            }
            WorkflowRunResult subResult = new WorkflowRunResult();
            if(MsgUtil.isValidMessage(subWorkflowOutputs)) {
                subResult.setStatus(WorkflowRunStatus.FINISHED.name());
            }else {
                subResult.setStatus(WorkflowRunStatus.FAILED.name());
                subResult.setErrorInfo(subWorkflowOutputs.getMessage().getMessage());
            }
            subResult.setPosition(Position.CHILD.name());
            subResult.setNodeId(workflowRunResult.getNodeId());
            subResult.setSubWorkflowRunIndex(i);
            subResult.setNodeType(workflowRunResult.getNodeType());
            subResult.setSubWorkflowId(workflowRunResult.getSubWorkflowId());
            subResult.setSubWorkflowRunInputText(WorkflowUtil.getInputText(subWorkflowRun.getInputs()));
            subResult.setSubWorkflowRunResult(PLAN_TYPEREFERENCE.to(subWorkflowRun.getWorkflowPlan().getPlan()));
            subResult.setTotalTokens(subWorkflowRun.getTotalTokens());
            subResult.setFirstTokenLatency(subWorkflowRun.getFirstTokenLatency());
            subResult.setInputTokens(subWorkflowRun.getInputTokens());
            subResult.setDegree(workflowRunResult.getDegree());
            subResult.setOutputTokens(subWorkflowRun.getOutputTokens());
            subResult.setTokenTime(subWorkflowRun.getTokenTime());
            subResult.setInputs(subWorkflowRun.getInputs());
            subResult.setOutputs(subWorkflowOutputs.getData());
            subResult.setRunAt(subWorkflowRun.getRunAt());
            subResult.setFinishAt(subWorkflowRun.getFinishAt());
            subWorkflowListRun.put(String.valueOf(subResult.getSubWorkflowRunIndex()),subResult);
            if (MapUtils.isEmpty(subResult.getOutputs()) || MapUtils.isEmpty(DataParametersUtil.getParameterByTemplate(WorkflowJSONKey.output.name(),subResult.getOutputs())) || !(DataParametersUtil.getValueByParameter(DataParametersUtil.getParameterByTemplate(WorkflowJSONKey.output.name(),subResult.getOutputs()),null) instanceof Map)) {
                outputValue.add(null);
                continue;
            }
            outputValue.add(DataParametersUtil.getValueByParameter(DataParametersUtil.getParameterByTemplate(WorkflowJSONKey.output.name(),subResult.getOutputs()),null));
        }
        JSONObject branchOutputs = WorkflowUtil.mergeOutputs(workflowRunResult, Lists.newArrayList(WorkflowUtil.createOutput(WorkflowJSONKey.output.name(), workflowRunResult.getSubWorkflowOutputType(), outputValue)));
        workflowRunResult.setSubWorkflowRunResult(subWorkflowListRun);
        return Response.ok(branchOutputs);

    }

    protected List<SubWorkflowTaskResult> runSubWorkflowTasks(List<WorkflowRunInMemory> subWorkflowRuns) throws Exception {
        if (CollectionUtils.isEmpty(subWorkflowRuns)) {
            return Lists.newArrayList();
        }
        if (Boolean.FALSE.equals(nodeConcurrent) || nodeExecutorService == null || subWorkflowRuns.size() <= 1) {
            List<SubWorkflowTaskResult> results = Lists.newArrayListWithCapacity(subWorkflowRuns.size());
            for (int i = 0; i < subWorkflowRuns.size(); i++) {
                results.add(new SubWorkflowTaskResult(i, subWorkflowRuns.get(i), subWorkflowRuns.get(i).call()));
            }
            return results;
        }
        List<Future<SubWorkflowTaskResult>> futures = Lists.newArrayListWithCapacity(subWorkflowRuns.size());
        for (int i = 0; i < subWorkflowRuns.size(); i++) {
            final int index = i;
            final WorkflowRunInMemory subWorkflowRun = subWorkflowRuns.get(i);
            futures.add(nodeExecutorService.submit(() ->
                    new SubWorkflowTaskResult(index, subWorkflowRun, subWorkflowRun.call())));
        }
        List<SubWorkflowTaskResult> results = Lists.newArrayListWithCapacity(subWorkflowRuns.size());
        for (Future<SubWorkflowTaskResult> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (ExecutionException e) {
                throw new RuntimeException("sub workflow task execution failed", e.getCause());
            }
        }
        results.sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));
        return results;
    }

    @Data
    public static class SubWorkflowTaskResult {
        private final Integer index;
        private final WorkflowRunInMemory subWorkflowRun;
        private final Response<JSONObject> response;
    }

    @Data
    public static class NodeTaskResult {
        private final WorkflowRunResult workflowRunResult;
        private final Response<Void> response;
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }



    public JSONObject getInputs(Map<String,WorkflowRunResult> plan,WorkflowRunResult workflowRunResult,BaseConsumer consumer) {
        List<Edge> edges = workflowRunResult.getData().getList(WorkflowJSONKey.edgesHandle.name(), Edge.class);
        JSONObject inputs = DataParametersUtil.mergeParameters(workflowRunResult.getInputs(),this.inputs);
        workflowRunResult.setInputs(inputs);
        if (CollectionUtils.isEmpty(edges)) {
            return workflowRunResult.getInputs();
        }
        List<Edge> sources = edges.stream().filter(e -> workflowRunResult.getNodeId().equals(e.getTarget()) && StringUtils.isNotBlank(e.getSource())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sources)) {
            return workflowRunResult.getInputs();
        }
        List<String> sourceNodeIds = WorkflowUtil.getSourceNodeIds(workflowRunResult);
        Map<String, WorkflowRunResult> sourceRunFinishedResult = Maps.newLinkedHashMap();
        for(String sourceNodeId : sourceNodeIds){
            if(!plan.containsKey(sourceNodeId)){
                continue;
            }
            WorkflowRunResult sourceRun = plan.get(sourceNodeId);
            if(WorkflowUtil.WORKFLOW_RUN_STATUS_FINISHED.contains(sourceRun.getStatus())){
                sourceRunFinishedResult.put(sourceNodeId,sourceRun);
            }
        }
        return consumer.getInputs(sources,sourceNodeIds,sourceRunFinishedResult,workflowRunResult,workflowRunId);
    }


}
