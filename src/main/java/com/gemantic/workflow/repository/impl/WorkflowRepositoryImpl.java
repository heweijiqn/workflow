package com.gemantic.workflow.repository.impl;

import com.alibaba.fastjson2.*;
import com.gemantic.api.client.UserClient;
import com.gemantic.db.model.BaseModel;
import com.gemantic.db.support.DBQuery;
import com.gemantic.db.support.DBQueryItem;
import com.gemantic.db.support.DBUpdate;
import com.gemantic.db.util.DBUtil;
import com.gemantic.dfs.support.DfsInfo;
import com.gemantic.es.utils.EsClientUtil;
import com.gemantic.gpt.client.*;
import com.gemantic.gpt.constant.*;
import com.gemantic.gpt.model.*;
import com.gemantic.gpt.support.FileUpload;
import com.gemantic.gpt.support.notify.NotifyConfig;
import com.gemantic.gpt.support.notify.NotifyPlan;
import com.gemantic.gpt.support.user.SysMqPriorityVo;
import com.gemantic.gpt.support.workflow.*;
import com.gemantic.gpt.util.*;
import com.gemantic.redisearch.client.RedisQueueClient;
import com.gemantic.redisearch.client.RedisZsetClient;
import com.gemantic.redisearch.constants.QueueOperate;
import com.gemantic.redisearch.support.ZSetInfo;
import com.gemantic.search.constant.CommonFields;
import com.gemantic.springcloud.constant.DBOperation;
import com.gemantic.springcloud.model.PageResponse;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.ConvertUtil;
import com.gemantic.springcloud.utils.MsgUtil;
import com.gemantic.springcloud.utils.TextProcessUtil;
import com.gemantic.workflow.consumer.BaseConsumer;
import com.gemantic.workflow.repository.ConsumerRepository;
import com.gemantic.workflow.repository.NotifyRepository;
import com.gemantic.workflow.repository.WorkflowRepository;
import com.gemantic.workflow.support.EdgeIndex;
import com.gemantic.workflow.support.NodeProcessContext;
import com.gemantic.workflow.support.TaskResponse;
import com.gemantic.workflow.support.WorkflowRunInMemory;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.hutool.core.thread.ThreadUtil.sleep;
import static java.util.Comparator.comparing;

@Repository
public class WorkflowRepositoryImpl implements WorkflowRepository {


    private static final Logger LOG = LoggerFactory.getLogger(WorkflowRepositoryImpl.class);
    @Resource
    private WorkflowRunClient workflowRunClient;

    @Resource
    private WorkflowRunResultClient workflowRunResultClient;

    @Resource
    private WorkflowClient workflowClient;

    @Resource
    private RedisQueueClient redisQueueClient;

    @Resource
    private RedisZsetClient redisZsetClient;

    @Resource
    private ChatgptFileApiClient chatgptFileApiClient;

//    @Resource
//    private ChatToolsClient chatToolsClient;

    @Resource
    protected EsClientUtil esClientUtil;

    @Resource
    private NotifyRepository notifyRepository;

    @Resource
    private ApplicationConfigClient applicationConfigClient;

    @Value("${consumer.mode:QUEUE}")
    private String consumerMode;

    @Resource
    private ConsumerRepository consumerRepository;

    @Resource
    private WorkflowVersionClient workflowVersionClient;

    @Resource
    private UserClient userClient;

    @Resource
    private WorkflowPlanClient workflowPlanClient;

    @Resource
    @Qualifier("nodeExecutorService")
    private ExecutorService nodeExecutorService;

    @Resource
    private WorkflowLogsClient workflowLogsClient;

    @Resource
    private WorkflowLogsEsClient workflowLogsEsClient;

    @Resource
    private WorkflowRunVersionClient workflowRunVersionClient;

    @Value("${workflow.run.version}")
    private Boolean workflowRunVersion;

    @Value("${SAVE_BATCH_SIZE:50}")
    private Integer saveBatchSize;

    @Override
    public Response<Long> runWorkflow(RunInputs runInputs) throws Exception {
        List<WorkflowRunResult> firstNodes = Lists.newArrayList();
        Response<Long> initResult = initRunWorkflow(runInputs, firstNodes);
        pushWorkFlowRunResult(firstNodes, Boolean.FALSE);
        return initResult;
    }


    @Override
    public Response<OpenRunOutput> runWorkflowInMemory(OpenRunInput data) throws Exception {
        List<WorkflowPlan> workflowPlans = workflowPlanClient.search(Lists.newArrayList(data.getWorkflowId().toString())).getData().getList();
        Response<OpenRunOutput> error = Response.error(null);
        if(CollectionUtils.isEmpty(workflowPlans)){
            error.getMessage().setMessage("该工作流不支持内存运行模式，输出组件必须为\"数据输出\"，工作流标签设置\"支持内存运行模式\"，再发布工作流后才能支持");
            return error;
        }
        WorkflowPlan workflowPlan = workflowPlans.getFirst();
        if(!data.getUserInfo().getChannel().equals(workflowPlan.getChannel())){
            error.getMessage().setMessage("账号无权限运行该工作流");
            return error;
        }
        JSONObject inputs = WorkflowUtil.getWorkflowInputs(data.getInputs(), workflowPlan.getInputs());
        WorkflowLogs workflowLogs = new WorkflowLogs();
        workflowLogs.setUserGroupId(data.getUserInfo().getUserGroupId());
        workflowLogs.setUserId(data.getUserInfo().getUserId());
        workflowLogs.setChannel(workflowPlan.getChannel());
        workflowLogs.setWorkflowId(workflowPlan.getId());
        workflowLogs.setVersion(workflowPlan.getVersion());
        workflowLogs.setVersionId(workflowPlan.getVersionId());
        workflowLogs.setRunAt(DateTime.now().getMillis());
        workflowLogs.setUserName(data.getUserInfo().getUserName());
        if(StringUtils.isBlank(data.getName())){
           workflowLogs.setName(workflowPlan.getName());
        }else {
            workflowLogs.setName(data.getName());
        }
        workflowLogs.setRunKey(data.getRunKey());
        workflowLogs.initId();
        workflowLogs.setCreateAt(DateTime.now().getMillis());
        workflowLogs.setStatus(WorkflowRunStatus.RUNNING.name());
        workflowLogs.setRunNodeCount(workflowPlan.getPlan().size());
        workflowLogs.setInputText(WorkflowUtil.getInputText(inputs));
        workflowLogs.setInputs(inputs);
        WorkflowRunInMemory workflowRunInMemory = new WorkflowRunInMemory();
        workflowRunInMemory.setWorkflowPlan(workflowPlan);
        workflowRunInMemory.setWorkflowRunId(workflowLogs.getId());
        workflowRunInMemory.setWorkflowPlanClient(workflowPlanClient);
        workflowRunInMemory.setConsumerRepository(consumerRepository);
        workflowRunInMemory.setInputs(inputs);
        workflowRunInMemory.setUserInfo(data.getUserInfo());
        workflowRunInMemory.setNodeExecutorService(nodeExecutorService);
        Response<JSONObject> call = workflowRunInMemory.call();
        workflowLogs.setRunNodeCount(workflowRunInMemory.getRunNodeCount());
        workflowLogs.setRunNodeSuccessCount(workflowRunInMemory.getRunNodeSuccessCount());
        workflowLogs.setRunNodeErrorCount(workflowRunInMemory.getRunNodeErrorCount());
        JSONObject logsData = new JSONObject();
        logsData.put("plan", TextProcessUtil.compressObject(workflowPlan.getPlan()));
        workflowLogs.setData(logsData);
        workflowLogs.setFinishAt(DateTime.now().getMillis());
        workflowLogs.setOutputs(call.getData());
        if(MsgUtil.isValidMessage(call)){
            workflowLogs.setStatus(WorkflowRunStatus.FINISHED.name());
            workflowLogs.setError(null);
        }else {
            workflowLogs.setError(call.getMessage().getMessage());
            workflowLogs.setStatus(WorkflowRunStatus.FAILED.name());
        }
        workflowLogs.setInputTokens(workflowRunInMemory.getInputTokens());
        workflowLogs.setTokenTime(workflowRunInMemory.getTokenTime());
        workflowLogs.setOutputTokens(workflowRunInMemory.getOutputTokens());
        workflowLogs.setTotalTokens(workflowRunInMemory.getTotalTokens());
        workflowLogs.setFirstTokenLatency(workflowRunInMemory.getFirstTokenLatency());
        if(esClientUtil.isEsClient()){
            workflowLogsEsClient.save(Lists.newArrayList(workflowLogs));
        }else {
            workflowLogsClient.save(Lists.newArrayList(workflowLogs));
        }
        OpenRunOutput openRunOutput = new OpenRunOutput();
        openRunOutput.setRunAt(workflowLogs.getRunAt());
        openRunOutput.setOutputType(workflowPlan.getOutputType());
        openRunOutput.setOutputs(WorkflowUtil.getJsonOutputs(call.getData()));
        openRunOutput.setWorkflowLogsId(workflowLogs.getId());
        if(NumberUtils.isDigits(workflowPlan.getVersionId())) {
            openRunOutput.setWorkflowVersionId(Long.valueOf(workflowPlan.getVersionId()));
        }
        openRunOutput.setFinishAt(workflowLogs.getFinishAt());
        openRunOutput.setName(data.getName());
        openRunOutput.setStatus(workflowLogs.getStatus());
        openRunOutput.setRunNodeErrorCount(workflowLogs.getRunNodeErrorCount());
        openRunOutput.setRunNodeCount(workflowLogs.getRunNodeCount());
        openRunOutput.setRunNodeSuccessCount(workflowLogs.getRunNodeSuccessCount());
        openRunOutput.setInputText(workflowLogs.getInputText());
        openRunOutput.setUserInfo(data.getUserInfo());
        return Response.ok(openRunOutput);
    }

    @Override
    public Response<WorkflowPlan> getPlan(WorkflowVersion workflowVersion) {
        WorkflowData workflowData = WorkflowUtil.getWorkflowData(workflowVersion.getData());
        List<Node> nodes = workflowData.getNodesHandle();
        List<Edge> edges = MoreObjects.firstNonNull(workflowData.getEdgesHandle(), Lists.newArrayList());
        Response<WorkflowPlan> error = Response.error(null);
        if (CollectionUtils.isEmpty(nodes)) {
            error.getMessage().setMessage(String.format("\"%s\"工作流无法运行,没有配置组件", workflowVersion.getWorkflowName()));
            return error;
        }
        List<Node> startParameterNodes = nodes.stream().filter(n -> WorkflowNodeType.start_parameters.name().equals(n.getType())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(startParameterNodes)) {
            error.getMessage().setMessage(String.format("\"%s\"工作流无法运行,缺少\"开始输入\"组件", workflowVersion.getWorkflowName()));
            return error;
        }
        if (startParameterNodes.size() > 1) {
            error.getMessage().setMessage(String.format("\"%s\"工作流无法运行,存在%d个\"开始输入\"组件，请修改工作流,只保留1个\"开始输入\"组件", workflowVersion.getWorkflowName(), startParameterNodes.size()));
            return error;
        }
        Map<String, Node> runNodes = nodes.stream().filter(n -> n.getHasInputs() == 1
                && !WorkflowNodeType.start_parameters.name().equals(n.getType())).collect(Collectors.toMap(Node::getId, Function.identity(), (k1, k2) -> k1));
        if (MapUtils.isEmpty(runNodes)) {
            error.getMessage().setMessage(String.format("\"%s\"工作流无法运行,缺少可运行的组件", workflowVersion.getWorkflowName()));
            return error;
        }
        List<Node> outputNodes = nodes.stream().filter(n -> WorkflowNodeType.content_output.name().equalsIgnoreCase(n.getType()) || WorkflowNodeType.json_output.name().equalsIgnoreCase(n.getType()) || WorkflowNodeType.doc_output.name().equalsIgnoreCase(n.getType())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(outputNodes)) {
            error.getMessage().setMessage(String.format("\"%s\"工作流无法运行,缺少\"输出\"组件", workflowVersion.getWorkflowName()));
            return error;
        }
        if (outputNodes.size() >= 2) {
            error.getMessage().setMessage(String.format("\"%s\"工作流无法运行,\"输出\"组件有%d个", workflowVersion.getWorkflowName(), outputNodes.size()));
            return error;
        }
        if (CollectionUtils.isEmpty(edges) && runNodes.size() >= 2) {
            error.getMessage().setMessage(String.format("\"%s\"工作流无法运行,组件之间没有连线", workflowVersion.getWorkflowName()));
            return error;
        }
        List<String> firstNodeIds = new ArrayList<>(runNodes.keySet());
        List<String> lastNodeIds = null;
        if (CollectionUtils.isNotEmpty(edges)) {
            List<Edge> errorEdges = edges.stream().filter(e -> StringUtils.isBlank(e.getTarget())
                            || null == runNodes.get(e.getTarget())
                            || StringUtils.isBlank(e.getSource())
                            || null == runNodes.get(e.getSource()))
                    .filter(e -> StringUtils.isNotBlank(e.getSourceHandle()) && !e.getSourceHandle().startsWith("ifelse")
                            || StringUtils.isNotBlank(e.getTargetHandle()) && !e.getTargetHandle().startsWith("ifelse")).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(errorEdges)) {
                error.getMessage().setMessage(String.format("\"%s\"工作流无法运行,部分组件之间连线错误", workflowVersion.getWorkflowName()));
                return error;
            }
            errorEdges = edges.stream().filter(e ->
                    StringUtils.isBlank(e.getTargetHandle())
                            || StringUtils.isBlank(e.getSourceHandle())
                            || null == runNodes.get(e.getTarget()).getData().getTemplate().get(e.getTargetHandle())
                            || null == runNodes.get(e.getSource()).getData().getTemplate().get(e.getSourceHandle())
            ).collect(Collectors.toList());

            errorEdges = errorEdges.stream()
                    .filter(e -> StringUtils.isNotBlank(e.getSourceHandle()) && !e.getSourceHandle().startsWith("ifelse")
                            || StringUtils.isNotBlank(e.getTargetHandle()) && !e.getTargetHandle().startsWith("ifelse")).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(errorEdges)) {
                StringBuilder errorInfo = new StringBuilder();
                for (Edge edge : errorEdges) {
                    Node source = runNodes.get(edge.getSource());
                    Node target = runNodes.get(edge.getTarget());
                    if (null != source && (null == source.getData() || null == source.getData().getTemplate() || null == source.getData().getTemplate().get(edge.getSourceHandle()))) {
                        String name = StringUtils.isNotBlank(source.getName()) ? source.getName() : source.getTypeName();
                        if (!errorInfo.isEmpty()) {
                            errorInfo.append(";");
                        }
//                        LOG.info("source => {}", source);
                        errorInfo.append(String.format("（%s）组件缺失\"%s\"", name, edge.getSourceHandle()));
                    }
                    if (null != target && (null == target.getData() || null == target.getData().getTemplate() || null == target.getData().getTemplate().get(edge.getTargetHandle()))) {
                        String name = StringUtils.isNotBlank(target.getName()) ? target.getName() : target.getTypeName();
                        if (!errorInfo.isEmpty()) {
                            errorInfo.append(";");
                        }
//                        LOG.info("target => {}",  target);
                        errorInfo.append(String.format("（%s）组件缺失\"%s\"", name, edge.getTargetHandle()));
                    }
                }
                if (errorInfo.length() <= 0) {
                    errorInfo.append("部分组件之间变量连线错误");
                }
                error.getMessage().setMessage(String.format("\"%s\"工作流无法运行:%s", workflowVersion.getWorkflowName(), errorInfo));
                return error;
            }
            List<String> sourceList = edges.stream().map(Edge::getSource).distinct().collect(Collectors.toList());
            List<String> targetList = edges.stream().map(Edge::getTarget).distinct().collect(Collectors.toList());
            firstNodeIds = new ArrayList<>(CollectionUtils.removeAll(firstNodeIds, targetList));
            firstNodeIds = new ArrayList<>(CollectionUtils.intersection(firstNodeIds, sourceList));
            if (CollectionUtils.isEmpty(firstNodeIds)) {
                error.getMessage().setMessage(String.format("\"%s\"工作流无法运行,没有开始运行的组件", workflowVersion.getWorkflowName()));
                return error;
            }
            lastNodeIds = new ArrayList<>(CollectionUtils.removeAll(targetList, sourceList));
            if (CollectionUtils.isEmpty(lastNodeIds)) {
                error.getMessage().setMessage(String.format("\"%s\"工作流无法运行,没有输出结果的组件", workflowVersion.getWorkflowName()));
                return error;
            }
            if (lastNodeIds.size() > 1) {
                error.getMessage().setMessage(String.format("\"%s\"工作流无法运行,输出结果的组件有%d个,只能配置一个输出结果组件", workflowVersion.getWorkflowName(), lastNodeIds.size()));
                return error;
            }
            if (!WorkflowUtil.isOutputNode(runNodes.get(lastNodeIds.getFirst()).getType())) {
                error.getMessage().setMessage(String.format("\"%s\"工作流无法运行,输出结果的组件不是输出分类下的组件", workflowVersion.getWorkflowName()));
                return error;
            }
        } else {
            if (runNodes.size() >= 2) {
                error.getMessage().setMessage(String.format("\"%s\"工作流无法运行,组件之间没有连线", workflowVersion.getWorkflowName()));
                return error;
            }
            //只有一个输出组件
            firstNodeIds = Lists.newArrayList(outputNodes.getFirst().getId());
            lastNodeIds = firstNodeIds;
        }
        Map<String, WorkflowRunResult> runResults = Maps.newLinkedHashMap();
        JSONObject runInputs = DataParametersUtil.mergeParameters(startParameterNodes.getFirst().getData().getTemplate(), workflowVersion.getInputs());
        for (String firstNodeId : firstNodeIds) {
            try {
                Response<Void> response =  getPlan(runResults, 1, firstNodeId, edges, runNodes, runInputs);
                if(!MsgUtil.isValidMessage(response)){
                    error.setMessage(response.getMessage());
                    LOG.error("工作流={}({}) 版本={}({}) 生成执行计划失败={}",workflowVersion.getWorkflowName(),workflowVersion.getWorkflowId(),workflowVersion.getName(),workflowVersion.getId(),response.getMessage().getMessage());
                    return error;
                }
            } catch (Exception e){
                LOG.error("工作流={}({}) 版本={}({}) 生成执行计划失败",workflowVersion.getWorkflowName(),workflowVersion.getWorkflowId(),workflowVersion.getName(),workflowVersion.getId(),e);
                error.getMessage().setMessage(e.getMessage());
                return error;
            }
        }
        if (runResults.size() == 1) {
            runResults.forEach((k, v) -> v.setPosition(Position.LEAF.name()));
        }
        WorkflowPlan workflowPlan = new WorkflowPlan();
        workflowPlan.setPlan(WorkflowUtil.getPlan(runResults));
        workflowPlan.setAppId(null == workflowVersion.getAppId() ? "0" :workflowVersion.getAppId().toString());
        workflowPlan.setChannel(workflowVersion.getChannel());
        workflowPlan.setName(workflowVersion.getWorkflowName());
        workflowPlan.setVersionId(null == workflowVersion.getId() ? "0" : workflowVersion.getId().toString());
        workflowPlan.setVersion(workflowVersion.getName());
        workflowPlan.setInputs(workflowVersion.getInputs());
        workflowPlan.setOutputType(workflowVersion.getOutputType());
        workflowPlan.setBeginNodeIds(firstNodeIds);
        workflowPlan.setEndNodeIds(lastNodeIds);
        workflowPlan.setId(workflowVersion.getWorkflowId().toString());
        return Response.ok(workflowPlan);
    }

    @Override
    public Response<Void> getPlan(Map<String,WorkflowRunResult> plan, Integer degree, String source, List<Edge> edges, Map<String, Node> runNodes, JSONObject startParameterInputs) throws Exception {
        Response<Void> error = Response.error();
        if(null == plan){
            error.getMessage().setMessage("plan不能为空");
            return error;
        }
        EdgeIndex edgeIndex = new EdgeIndex(edges);
        fillRunResultsWithIndex(WorkflowRunMode.MEMORY.name(), plan, degree, source, edgeIndex, runNodes, startParameterInputs, WorkflowRunType.RUN.name());
        return Response.ok();
    }

    @Override
    public Response<Long> initRunWorkflow(RunInputs runInputs, List<WorkflowRunResult> firstNodes) throws Exception {
        Response<Long> error = Response.error(null);
        if (null != runInputs.getWorkflowRunId() && runInputs.getWorkflowRunId() > 0L) {
            WorkflowRun workflowRun = workflowRunClient.getById(runInputs.getWorkflowRunId()).getData();
            if(null == workflowRun){
                error.getMessage().setMessage("工作流运行记录不存在");
                return error;
            }
            if(!WorkflowRunStatus.FAILED.name().equals(workflowRun.getStatus()) && !WorkflowRunStatus.FINISHED.name().equals(workflowRun.getStatus())){
                error.getMessage().setMessage("工作流未运行结束，不允许重新运行");
                return error;
            }
            Map<String, String> deleteResultParams = Maps.newHashMap();
            deleteResultParams.put("workflowRunId", String.valueOf(workflowRun.getId()));
            workflowRunResultClient.delete(deleteResultParams);
            WorkflowData data = WorkflowUtil.getWorkflowData(workflowRun.getData());
            Map<String, WorkflowRunResult> runResults = getWorkflowRunResult(workflowRun.getMode(), workflowRun.getName(), data, workflowRun.getInputs(), workflowRun.getType());
            List<WorkflowRunResult> saveResults = new ArrayList<>(runResults.values());
            saveWorkflowRunResult(workflowRun, saveResults);
            Integer nodeCount = getWorkflowRunNodeCount(workflowRun.getId(), null, workflowRun.getType());
            List<String> updateKeys = Lists.newArrayList("status","runNodeErrorCount","runNodeCount","runNodeSuccessCount","runAt","finishAt","updateAt");
            List<String> updateValues = Lists.newArrayList(WorkflowRunStatus.NOT_STARTED.name(),"0",nodeCount.toString(),"0","0","0",String.valueOf(DateTime.now().getMillis()));
            workflowRunClient.update(Lists.newArrayList(workflowRun.getId()),updateKeys,updateValues);
            if (saveResults.size() == 1) {
                firstNodes.add(saveResults.getFirst());
            } else {
                firstNodes.addAll(saveResults.stream().filter(s -> Position.ROOT.name().equalsIgnoreCase(s.getPosition())).toList());
            }
            return Response.ok(workflowRun.getId());
        }
        if (null == runInputs.getWorkflowId() || runInputs.getWorkflowId() <= 0L) {
            error.getMessage().setMessage("工作流id必填");
            return error;
        }
        if (null == runInputs.getUserInfo()) {
            error.getMessage().setMessage("userInfo必填");
            return error;
        }
        if (StringUtils.isBlank(runInputs.getUserInfo().getUserGroupId()) || StringUtils.isBlank(runInputs.getUserInfo().getUserId())) {
            error.getMessage().setMessage("userGroupId和userId必填");
            return error;
        }
        Workflow dbWorkflow = workflowClient.getById(runInputs.getWorkflowId()).getData();
        if (null == dbWorkflow) {
            error.getMessage().setMessage("工作流不存在或已被删除");
            return error;
        }
        dbWorkflow.setUserInfo(runInputs.getUserInfo());
        Response<Void> allowUse = UserUtil.allowWrite(String.format("运行%s(%d)", Workflow.class, dbWorkflow.getId()), dbWorkflow);
        if (!MsgUtil.isValidMessage(allowUse)) {
            error.setMessage(allowUse.getMessage());
            return error;
        }
        JSONObject metadata = runInputs.getMetadata();
        Workflow data = new Workflow();
        data.setId(runInputs.getWorkflowId());
        data.setName(dbWorkflow.getName());
        data.setAppId(dbWorkflow.getAppId());
        if (StringUtils.isBlank(dbWorkflow.getWorkflowDataMd5())) {
            data.setWorkflowDataMd5(WorkflowUtil.getWorkflowDataMd5(dbWorkflow.getData()));
        } else {
            data.setWorkflowDataMd5(dbWorkflow.getWorkflowDataMd5());
        }
        JSONObject flowData = runInputs.getData();
        if (MapUtils.isEmpty(flowData)) {
            if(null != runInputs.getWorkflowVersionId() && runInputs.getWorkflowVersionId() > 0L){
                WorkflowVersion workflowVersion = workflowVersionClient.getById(runInputs.getWorkflowVersionId()).getData();
                if(null == workflowVersion){
                    error.getMessage().setMessage(String.format("\"%s\"工作流版本不存在，无法运行", data.getName()));
                    return error;
                }
                flowData = workflowVersion.getData();
            }else {
                flowData = dbWorkflow.getData();
            }
        }
        flowData = getFlowData(flowData, runInputs.getRunNodeIdBegin(), runInputs.getRunNodeIdEnd());
        data.setData(flowData);
        if (MapUtils.isEmpty(flowData)) {
            error.getMessage().setMessage(String.format("\"%s\"工作流为空，无法运行", data.getName()));
            return error;
        }
        JSONObject inputs = MoreObjects.firstNonNull(runInputs.getInputs(), new JSONObject());
        try {
            WorkflowRun workflowRun = new WorkflowRun();
            if (!WorkflowRunMode.COPY.name().equalsIgnoreCase(runInputs.getMode())) {
                workflowRun.setMode(runInputs.getMode());
            }
            if (StringUtils.isNotBlank(runInputs.getName())) {
                workflowRun.setName(runInputs.getName());
            } else {
                workflowRun.setName(data.getName());
            }
            if(null != runInputs.getWorkflowVersionId() && runInputs.getWorkflowVersionId() > 0L){
                workflowRun.setVersionId(runInputs.getWorkflowVersionId());
            }
            workflowRun.setUserInfo(runInputs.getUserInfo());
            workflowRun.setData(data.getData());
            workflowRun.setMetadata(metadata);
            if (null != runInputs.getNotifyConfig()) {
                workflowRun.setNotifyConfig(runInputs.getNotifyConfig());
            } else if (null == workflowRun.getNotifyConfig()) {
                workflowRun.setNotifyConfig(dbWorkflow.getNotifyConfig());
            }
            workflowRun.setWorkflowId(data.getId());
            workflowRun.setRunKey(runInputs.getRunKey());
            workflowRun.setRunner(runInputs.getRunner());
            workflowRun.setAppId(data.getAppId());
            workflowRun.setInputs(inputs);
            workflowRun.setCopyTimeout(dbWorkflow.getCopyTimeout());
            workflowRun.setUserId(runInputs.getUserInfo().getUserId());
            workflowRun.setUserGroupId(runInputs.getUserInfo().getUserGroupId());
            workflowRun.setUserName(runInputs.getUserInfo().getUserName());
            workflowRun.setChannel(runInputs.getUserInfo().getChannel());
            workflowRun.setType(runInputs.getType());
            workflowRun.setOutputType(dbWorkflow.getOutputType());
            workflowRun.setUserInfo(runInputs.getUserInfo());
            // 从工作流复制标签到工作流运行（冗余存储）
            workflowRun.setTags(dbWorkflow.getTags());
            //如果政策运行不报错
            Map<String, WorkflowRunResult> runResults = getWorkflowRunResult(runInputs.getMode(), data, inputs, runInputs.getType());
            List<WorkflowRunResult> saveResults = new ArrayList<>(runResults.values());
            workflowRun.setDegree(saveResults.stream().mapToInt(WorkflowRunResult::getDegree).max().getAsInt());
            List<Long> id = workflowRunClient.save(Lists.newArrayList(workflowRun)).getData();
            Long runId = id.getFirst();
            workflowRun.setId(runId);
            List<WorkflowRunResult> uploadFiles = getRunUploadFilesNode(runInputs.getMode(), workflowRun, inputs);
            if (CollectionUtils.isNotEmpty(uploadFiles)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("文件上传 {}", uploadFiles.stream().map(WorkflowRunResult::getNodeName).collect(Collectors.toList()));
                }
                runResults = getWorkflowRunResult(runInputs.getMode(), data, inputs, runInputs.getType());
                saveResults = new ArrayList<>(runResults.values());
                workflowRun.setStatus(WorkflowRunStatus.RUNNING.name());
                workflowRun.setRunAt(DateTime.now().getMillis());
            } else {
                if (saveResults.size() == 1) {
                    firstNodes.add(saveResults.getFirst());
                } else {
                    firstNodes.addAll(saveResults.stream().filter(s -> Position.ROOT.name().equalsIgnoreCase(s.getPosition())).toList());
                }
            }
            if (!WorkflowRunType.DEBUG.name().equals(runInputs.getType()) && CollectionUtils.isEmpty(runInputs.getRunNodeIdBegin()) && CollectionUtils.isEmpty(runInputs.getRunNodeIdEnd())) {
                workflowRun.setWorkflowDataMd5(dbWorkflow.getWorkflowDataMd5());
            } else {
                workflowRun.setWorkflowDataMd5(WorkflowUtil.getWorkflowDataMd5(flowData));
            }
            workflowRun.setInputs(inputs);
            workflowRun.setWorkflowInputsMd5(WorkflowUtil.getInputsMd5(workflowRun.getInputs()));
            CopyRunResult copyResult = null;
            if ((null == runInputs.getWorkflowRunId() || runInputs.getWorkflowRunId() <= 0) && WorkflowRunMode.COPY.name().equalsIgnoreCase(runInputs.getMode())) {
                copyResult = getWorkflowRunResultCopy(workflowRun.getWorkflowId(), workflowRun.getOutputType(), workflowRun.getWorkflowInputsMd5(), workflowRun.getWorkflowDataMd5(), dbWorkflow.getCopyTimeout());
            }
            if (null != copyResult) {
                saveResults = copyResult.getToData();
                if (null != copyResult.getFromWorkflowRun()) {
                    workflowRun.setCopyWorkflowRunId(copyResult.getFromWorkflowRun().getId());
                    workflowRun.setCopyUpdateAt(copyResult.getFromWorkflowRun().getFinishAt());
                    workflowRun.setOutputs(copyResult.getFromWorkflowRun().getOutputs());
                } else {
                    workflowRun.setCopyWorkflowRunId(copyResult.getFromWorkflowRunResult().getWorkflowRunId());
                    workflowRun.setCopyWorkflowRunResultId(copyResult.getFromWorkflowRunResult().getId());
                    workflowRun.setCopyUpdateAt(copyResult.getFromWorkflowRunResult().getFinishAt());
                    workflowRun.setOutputs(copyResult.getFromWorkflowRunResult().getOutputs());
                }
                workflowRun.setCopyTimeout(dbWorkflow.getCopyTimeout());
                workflowRun.setStatus(WorkflowRunStatus.FINISHED.name());
                workflowRun.setRunAt(DateTime.now().getMillis());
                workflowRun.setFinishAt(workflowRun.getRunAt());
                workflowRun.setMode(WorkflowRunMode.COPY.name());
                if (CollectionUtils.isNotEmpty(uploadFiles)) {
                    workflowRunResultClient.delete(uploadFiles.stream().map(BaseModel::getId).collect(Collectors.toList()));
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("工作流{}({}) 复制已有的运行结果 runId={} copyTimeout={} copyUpdateAt={} copyRunId={} copyRunResultId={}", dbWorkflow.getName(), dbWorkflow.getId(), workflowRun.getId(), dbWorkflow.getCopyTimeout(), workflowRun.getCopyUpdateAt(), workflowRun.getCopyWorkflowRunId(), workflowRun.getCopyWorkflowRunResultId());
                    }
                }
                Response<Long> response = notify(workflowRun);
                workflowRun.setNotifyLogId(response.getData());
            }
            workflowRun.setInputText(WorkflowUtil.getInputText(workflowRun.getInputs()));
            saveWorkflowRunResult(workflowRun, saveResults);
            Integer nodeCount = getWorkflowRunNodeCount(runId, null, workflowRun.getType());
            Integer errorCount = getWorkflowRunNodeCount(runId, WorkflowRunStatus.FAILED.name(), workflowRun.getType());
            Integer successCount = getWorkflowRunNodeCount(runId, WorkflowRunStatus.FINISHED.name(), workflowRun.getType());
            workflowRun.setRunNodeCount(nodeCount);
            workflowRun.setRunNodeErrorCount(errorCount);
            workflowRun.setRunNodeSuccessCount(successCount);
            workflowRunClient.save(Lists.newArrayList(workflowRun));
            if (null != copyResult) {
                firstNodes.clear();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("工作流{}({}) 复制已有的运行结果 runId={} copyTimeout={} copyUpdateAt={} copyRunId={} copyRunResultId={}", dbWorkflow.getName(), dbWorkflow.getId(), workflowRun.getId(), dbWorkflow.getCopyTimeout(), workflowRun.getCopyUpdateAt(), workflowRun.getCopyWorkflowRunId(), workflowRun.getCopyWorkflowRunResultId());
                }
            }
            return Response.ok(runId);
        } catch (WorkflowException e) {
            error.getMessage().setMessage(e.getMessage());
            return error;
        }
    }

    @Override
    public CopyRunResult getWorkflowRunResultCopy(Long workflowId, String outputType, String workflowInputsMd5, String workflowDataMd5, Long copyTimeout) throws Exception {
        if (null == workflowId || workflowId <= 0L || StringUtils.isBlank(workflowDataMd5) || StringUtils.isBlank(workflowInputsMd5) || null == copyTimeout || copyTimeout == 0L) {
            return null;
        }
        CopyRunResult runs = getWorkflowRunResultCopyByRun(workflowId, outputType, workflowInputsMd5, workflowDataMd5, copyTimeout);
        CopyRunResult subworkflow = getWorkflowRunResultCopyBySubworkflow(workflowId, workflowInputsMd5, workflowDataMd5, copyTimeout);
        if (null == runs) {
            return subworkflow;
        }
        if (null == subworkflow) {
            return runs;
        }
        if (subworkflow.getFromWorkflowRunResult().getFinishAt() > runs.getFromWorkflowRun().getFinishAt()) {
            return subworkflow;
        }
        return runs;
    }

    @Override
    public CopyRunResult getWorkflowRunResultCopyByRun(Long workflowId, String outputType, String workflowInputsMd5, String workflowDataMd5, Long copyTimeout) throws Exception {
        if (null == workflowId || workflowId <= 0L || StringUtils.isBlank(workflowDataMd5) || StringUtils.isBlank(workflowInputsMd5) || null == copyTimeout) {
            return null;
        }
        Map<String, String> params = Maps.newHashMap();
        params.put("workflowId", String.valueOf(workflowId));
        params.put("workflowDataMd5", workflowDataMd5);
        params.put("workflowInputsMd5", workflowInputsMd5);
        params.put("outputType", outputType);
        if (copyTimeout >= 0L) {
            params.put("finishAt", String.join(StringUtils.EMPTY, CommonFields.DEFALUT_RANGE_VALUE, String.valueOf(DateTime.now().getMillis() - copyTimeout)));
        }
        params.put("copyWorkflowRunId", "0");
        params.put("copyWorkflowRunResultId", "0");
        params.put("status", WorkflowRunStatus.FINISHED.name());
        List<WorkflowRun> workflowRunList = workflowRunClient.find(Lists.newArrayList("finishAt"), Lists.newArrayList("DESC"), 1, 1, params).getData().getList();
        if (CollectionUtils.isEmpty(workflowRunList)) {
            return null;
        }
        WorkflowRun workflowRun = workflowRunList.getFirst();
        Map<String, String> paramsResult = Maps.newHashMap();
        paramsResult.put("workflowRunId", String.valueOf(workflowRun.getId()));
        List<WorkflowRunResult> workflowRunResults = WorkflowUtil.convertTree(workflowRunResultClient.find(1, Integer.MAX_VALUE, paramsResult).getData().getList());
        if (CollectionUtils.isEmpty(workflowRunResults)) {
            return null;
        }
        resetWorkflowRunResultCopyInfo(workflowRunResults, workflowRun.getFinishAt(), copyTimeout);
        CopyRunResult result = new CopyRunResult();
        result.setFromWorkflowRun(workflowRun);
        result.setToData(workflowRunResults);
        return result;
    }

    @Override
    public CopyRunResult getWorkflowRunResultCopyBySubworkflow(Long workflowId, String workflowInputsMd5, String workflowDataMd5, Long copyTimeout) throws Exception {
        if (null == workflowId || workflowId <= 0L || StringUtils.isBlank(workflowDataMd5) || StringUtils.isBlank(workflowInputsMd5) || null == copyTimeout) {
            return null;
        }
        Map<String, String> params = Maps.newHashMap();
        params.put("subWorkflowId", String.valueOf(workflowId));
        params.put("subWorkflowDataMd5", workflowDataMd5);
        params.put("subWorkflowInputsMd5", workflowInputsMd5);
        if (copyTimeout >= 0L) {
            params.put("finishAt", String.join(StringUtils.EMPTY, CommonFields.DEFALUT_RANGE_VALUE, String.valueOf(DateTime.now().getMillis() - copyTimeout)));
        }
        params.put("copyWorkflowRunId", "0");
        params.put("copyWorkflowRunResultId", "0");
        params.put("status", WorkflowRunStatus.FINISHED.name());
        List<WorkflowRunResult> workflowRunResultList = workflowRunResultClient.find(Lists.newArrayList("finishAt"), Lists.newArrayList("DESC"), 1, 1, params).getData().getList();
        if (CollectionUtils.isEmpty(workflowRunResultList)) {
            return null;
        }
        WorkflowRunResult workflowRunResult = workflowRunResultList.getFirst();

        List<WorkflowRunResult> workflowRunResults = workflowRunResultClient.getChildren(workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowRunType()).getData();
        if (CollectionUtils.isEmpty(workflowRunResults)) {
            return null;
        }
        workflowRunResults.forEach(w -> {
            if (workflowRunResult.getId().equals(w.getParentId())) {
                w.setParentId(null);
            }
        });
        workflowRunResults = WorkflowUtil.convertTree(workflowRunResults);
        resetWorkflowRunResultCopyInfo(workflowRunResults, workflowRunResult.getFinishAt(), copyTimeout);
        CopyRunResult result = new CopyRunResult();
        result.setFromWorkflowRunResult(workflowRunResult);
        result.setToData(workflowRunResults);
        return result;
    }

    private void resetWorkflowRunResultCopyInfo(Collection<WorkflowRunResult> workflowRunResults, Long copyUpdateAt, Long copyTimeout) {
        if (CollectionUtils.isEmpty(workflowRunResults)) {
            return;
        }
        for (WorkflowRunResult workflowRunResult : workflowRunResults) {
            workflowRunResult.setCopyWorkflowRunId(workflowRunResult.getWorkflowRunId());
            workflowRunResult.setCopyWorkflowRunResultId(workflowRunResult.getId());
            workflowRunResult.setCopyUpdateAt(copyUpdateAt);
            workflowRunResult.setCopyTimeout(copyTimeout);
            workflowRunResult.setMode(WorkflowRunMode.COPY.name());
            workflowRunResult.setId(null);
            workflowRunResult.setParentId(null);
            if (MapUtils.isNotEmpty(workflowRunResult.getSubWorkflowRunResult())) {
                resetWorkflowRunResultCopyInfo(workflowRunResult.getSubWorkflowRunResult().values(), copyUpdateAt, copyTimeout);
            }
        }
    }


    private void saveWorkFlowRunResults(List<WorkflowRunResult> workflowRunResults) throws Exception {
        if (CollectionUtils.isEmpty(workflowRunResults)) {
            return;
        }
        Long workflowId = workflowRunResults.getFirst().getWorkflowId();
        List<String> replaceDbNodes = workflowRunResults.stream().filter(w -> WorkflowRunType.DEBUG.name().equalsIgnoreCase(w.getWorkflowRunType()) && (null == w.getParentId() || w.getParentId() <= 0L)).map(WorkflowRunResult::getNodeId).distinct().collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(replaceDbNodes)) {
            Map<String, String> dbParam = Maps.newHashMap();
            dbParam.put("workflowId", String.valueOf(workflowId));
            dbParam.put("workflowRunType", WorkflowRunType.DEBUG.name());
            dbParam.put("nodeId", String.join(",", replaceDbNodes));
            Map<String, WorkflowRunResult> dbWorkflowRunResults = workflowRunResultClient.find(1, Integer.MAX_VALUE, dbParam).getData().getList().stream().collect(Collectors.toMap(WorkflowRunResult::getNodeId, Function.identity(), (k1, k2) -> k1));
            for (WorkflowRunResult workflowRunResult : workflowRunResults) {
                if (null != workflowRunResult.getParentId() && workflowRunResult.getParentId() > 0L) {
                    continue;
                }
                WorkflowRunResult dbWorkflowRunResult = dbWorkflowRunResults.get(workflowRunResult.getNodeId());
                if (null == dbWorkflowRunResult) {
                    continue;
                }
                workflowRunResult.setId(dbWorkflowRunResult.getId());
            }
        }
        List<Long> ids = workflowRunResultClient.save(workflowRunResults).getData();
        DBUtil.fillModelId(ids, workflowRunResults);
    }

    private JSONObject getFlowData(JSONObject flowData, List<String> runNodeIdBegin, List<String> runNodeIdEnd) {
        if (MapUtils.isEmpty(flowData) || null == flowData.get(WorkflowJSONKey.nodesHandle.name())
                || !(flowData.get(WorkflowJSONKey.nodesHandle.name()) instanceof List)) {
            return new JSONObject();
        }
        if (CollectionUtils.isEmpty(runNodeIdBegin) && CollectionUtils.isEmpty(runNodeIdEnd)) {
            return flowData;
        }
        List<JSONObject> nodesHandle = flowData.getList(WorkflowJSONKey.nodesHandle.name(), JSONObject.class);
        List<JSONObject> edgesHandle = Lists.newArrayList();
        if (null != flowData.get(WorkflowJSONKey.edgesHandle.name()) && (flowData.get(WorkflowJSONKey.edgesHandle.name()) instanceof List)) {
            edgesHandle = flowData.getList(WorkflowJSONKey.edgesHandle.name(), JSONObject.class);
        }
        List<JSONObject> targetNodesHandle = Lists.newArrayList();
        List<JSONObject> targetEdgesHandle = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(runNodeIdBegin)) {
            for (String nodeId : runNodeIdBegin) {
                fillFlowData(nodesHandle, edgesHandle, targetNodesHandle, targetEdgesHandle, nodeId, runNodeIdEnd, Boolean.FALSE);
            }
        } else {
            for (String nodeId : runNodeIdEnd) {
                fillFlowData(nodesHandle, edgesHandle, targetNodesHandle, targetEdgesHandle, nodeId, runNodeIdBegin, Boolean.TRUE);
            }
        }
        if (CollectionUtils.isEmpty(targetNodesHandle) && CollectionUtils.isEmpty(targetEdgesHandle)) {
            return new JSONObject();
        }
        JSONObject runData = JSON.copy(flowData, DataParametersUtil.JSONWRITER_FEATURE);
        runData.put(WorkflowJSONKey.nodesHandle.name(), targetNodesHandle);
        runData.put(WorkflowJSONKey.nodes.name(), targetNodesHandle);
        runData.put(WorkflowJSONKey.edgesHandle.name(), targetEdgesHandle);
        runData.put(WorkflowJSONKey.edges.name(), targetEdgesHandle);
        return runData;
    }


    private void fillFlowData(List<JSONObject> sourceNodesHandle, List<JSONObject> sourceEdgesHandle, List<JSONObject> targetNodesHandle, List<JSONObject> targetEdgesHandle, String nodeId, List<String> stopNodeIds, Boolean runBefore) {
        if (StringUtils.isBlank(nodeId)) {
            return;
        }
        Optional<JSONObject> nodeOptional = sourceNodesHandle.stream().filter(n -> nodeId.equalsIgnoreCase(n.getString(WorkflowJSONKey.id.name()))).findFirst();
        if (nodeOptional.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("no exists debug nodeId={}", nodeId);
            }
            return;
        }
        JSONObject node = nodeOptional.get();
        targetNodesHandle.add(node);
        if (CollectionUtils.isNotEmpty(stopNodeIds) && stopNodeIds.contains(nodeId)) {
            return;
        }
        List<JSONObject> nodeEdgesHandle = null;
        if (runBefore) {
            nodeEdgesHandle = sourceEdgesHandle.stream().filter(s -> nodeId.equals(s.getString(WorkflowJSONKey.target.name()))).collect(Collectors.toList());
        } else {
            nodeEdgesHandle = sourceEdgesHandle.stream().filter(s -> nodeId.equals(s.getString(WorkflowJSONKey.source.name()))).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(nodeEdgesHandle)) {
            return;
        }
        sourceEdgesHandle.addAll(nodeEdgesHandle);
        List<String> relationNodes = nodeEdgesHandle.stream().map(s -> runBefore ? s.getString(WorkflowJSONKey.source.name()) : s.getString(WorkflowJSONKey.target.name())).filter(StringUtils::isNotBlank).distinct().toList();
        for (String relationNode : relationNodes) {
            fillFlowData(sourceNodesHandle, sourceEdgesHandle, targetNodesHandle, targetEdgesHandle, relationNode, stopNodeIds, runBefore);
        }
    }

    private List<WorkflowRunResult> getRunUploadFilesNode(String mode, WorkflowRun workflowRun, JSONObject inputs) throws Exception {
        List<JSONObject> uploadFiles = DataParametersUtil.getParametersByTypePrefix(inputs, Lists.newArrayList(WorkflowJSONKey.uploadFiles.name(), WorkflowJSONKey.extractFiles.name()));
        if (CollectionUtils.isEmpty(uploadFiles)) {
            return Lists.newArrayList();
        }
        List<WorkflowRunResult> result = Lists.newArrayList();
        for (JSONObject uploadFile : uploadFiles) {
            String name = DataParametersUtil.getNameByParameter(uploadFile);
            String type = DataParametersUtil.getTypeByParameter(uploadFile);
            List<DfsInfo> dfsInfos = WorkflowUtil.getDfsInfoByParameter(uploadFile);
            if (CollectionUtils.isEmpty(dfsInfos)) {
                continue;
            }
            FileUpload parameterMeta = DataParametersUtil.getMetaByParameter(uploadFile, FileUpload.class);
            List<WorkflowRunResult> workflowRunResults = Lists.newArrayList();
            List<DfsInfo> allDfsInfo = Lists.newArrayList();
            for (DfsInfo dfsInfo : dfsInfos) {
                if (MapUtils.isNotEmpty(dfsInfo.getMeta()) && StringUtils.isNotBlank(dfsInfo.getMeta().getString(WorkflowJSONKey.docId.name()))) {
                    continue;
                }
                if (MapUtils.isNotEmpty(dfsInfo.getMeta())
                        && (StringUtils.isNotBlank(dfsInfo.getMeta().getString(WorkflowJSONKey.workflowRunResultId.name())))) {
                    allDfsInfo.add(dfsInfo);
                    continue;
                }
                dfsInfo.setId(DigestUtils.md5Hex(dfsInfo.getFilePath()));
                JSONObject dfsParameter = JSON.copy(uploadFile, DataParametersUtil.JSONWRITER_FEATURE);
                JSONObject dfsInputs = new JSONObject();
                DataParametersUtil.updateParameterValue(dfsParameter, null, Lists.newArrayList(dfsInfo));
                dfsInputs.put(name, dfsParameter);
                WorkflowRunResult workflowRunResult = new WorkflowRunResult();
                workflowRunResult.setInputs(dfsInputs);
                workflowRunResult.setNodeId(dfsInfo.getId());
                workflowRunResult.setData(new JSONObject());
                workflowRunResult.setNodeType(WorkflowNodeType.start_parameters.name());
                workflowRunResult.setNodeName(dfsInfo.getOriginName());
                if(type.equalsIgnoreCase(WorkflowJSONKey.extractFiles.name())){
                    workflowRunResult.setNodeTypeName("上传文件(抽取)");
                }else if (type.startsWith(WorkflowJSONKey.extractFiles.name())) {
                    workflowRunResult.setNodeTypeName(String.format("上传文件(%s))",StringUtils.substringAfterLast(type,"_")));
                }  else {
                    workflowRunResult.setNodeTypeName("上传文件");
                }
                workflowRunResult.setDegree(0);
                workflowRunResults.add(workflowRunResult);
            }
            if (CollectionUtils.isEmpty(workflowRunResults)) {
                continue;
            }
            saveWorkflowRunResult(workflowRun, workflowRunResults);
            FileUpload fileUpload = new FileUpload();
            fileUpload.setApplicationId(workflowRun.getAppId());
            fileUpload.setWorkflowId(workflowRun.getWorkflowId());
            fileUpload.setWorkflowRunId(workflowRun.getId());
            fileUpload.setExtractType(type);
            if (null != parameterMeta) {
                fileUpload.setAppId(parameterMeta.getAppId());
                fileUpload.setAppFileType(parameterMeta.getAppFileType());
                fileUpload.setAppTaskName(parameterMeta.getAppTaskName());
                fileUpload.setAppFileTypeId(parameterMeta.getAppFileTypeId());
            }
            for (WorkflowRunResult workflowRunResult : workflowRunResults) {
                List<DfsInfo> dfsInfoList = DataParametersUtil.getValuesByTemplate(name, workflowRunResult.getInputs(), DfsInfo.class, Lists.newArrayList());
                dfsInfoList.forEach(d -> {
                    JSONObject meta = new JSONObject();
                    meta.put(WorkflowJSONKey.workflowRunResultId.name(), workflowRunResult.getId().toString());
                    d.setMeta(meta);
                });
                DataParametersUtil.updateParameterValue(workflowRunResult.getInputs().getJSONObject(name), null, dfsInfoList);
                allDfsInfo.addAll(dfsInfoList);
            }
            fileUpload.setFiles(allDfsInfo);
            Response<List<TaskResponse>> taskResponses = chatgptFileApiClient.upload(fileUpload);
            if (LOG.isDebugEnabled()) {
                LOG.debug("文件上传 request={} response={}", JSONObject.from(fileUpload, JSONWriter.Feature.FieldBased, JSONWriter.Feature.SortMapEntriesByKeys), JSONArray.from(taskResponses.getData(), JSONWriter.Feature.FieldBased, JSONWriter.Feature.SortMapEntriesByKeys));
            }
            String error = null;
            if (!MsgUtil.isValidMessage(taskResponses)) {
                error = String.format("文件上传失败,%s", taskResponses.getMessage().getMessage());
            }
            if (CollectionUtils.isEmpty(taskResponses.getData())) {
                error = "文件上传返回结果为空";
            }
            if (StringUtils.isNotBlank(error)) {
                for (WorkflowRunResult w : workflowRunResults) {
                    updateWorkflowRunStatusByFailure(w, error,DateTime.now().getMillis());
                }
                throw new WorkflowException(error);
            }

            for (int i = 0; i < taskResponses.getData().size(); i++) {
                TaskResponse taskResponse = taskResponses.getData().get(i);
                DfsInfo dfsInfo = allDfsInfo.get(i);
                JSONObject meta = dfsInfo.getMeta();
                meta.put(WorkflowJSONKey.taskId.name(), taskResponse.getTaskId());
                meta.put(WorkflowJSONKey.taskType.name(), taskResponse.getTaskType());
                meta.put(WorkflowJSONKey.docId.name(), taskResponse.getEvidenceIds().get(dfsInfo.getMd5()));
                dfsInfo.setMeta(meta);
            }
            Map<String, DfsInfo> dfsInfoMap = allDfsInfo.stream().collect(Collectors.toMap(DfsInfo::getId, Function.identity(), (k1, k2) -> k1));
            for (WorkflowRunResult workflowRunResult : workflowRunResults) {
                DfsInfo dfsInfo = dfsInfoMap.get(workflowRunResult.getNodeId());
                dfsInfo.setId(dfsInfo.getMeta().getString(WorkflowJSONKey.taskId.name()));
                workflowRunResult.setNodeId(dfsInfo.getId());
                workflowRunResult.setStatus(WorkflowRunStatus.RUNNING.name());
                workflowRunResult.setRunAt(DateTime.now().getMillis());
            }
            DataParametersUtil.updateParameterValue(uploadFile, null, allDfsInfo);
            workflowRunResultClient.save(workflowRunResults);
            result.addAll(workflowRunResults);
        }
        inputs.putAll(uploadFiles.stream().collect(Collectors.toMap(DataParametersUtil::getNameByParameter, Function.identity(), (k1, k2) -> k1)));
        return result;
    }

    @Override
    public Map<String, WorkflowRunResult> getWorkflowRunResult(String mode, Workflow workflow, JSONObject inputs, String workflowRunType) throws Exception {
        if (MapUtils.isEmpty(workflow.getData())) {
            throw new WorkflowException(String.format("\"%s\"工作流为空，无法运行", workflow.getName()));
        }
        WorkflowData data = WorkflowUtil.getWorkflowData(workflow.getData());
        return getWorkflowRunResult(mode, workflow.getName(), data, inputs, workflowRunType);
    }


    @Override
    public Map<String, WorkflowRunResult> getWorkflowRunResult(String mode, String workflowName, WorkflowData data, JSONObject inputs, String workflowRunType) throws Exception {
        List<Node> nodes = data.getNodesHandle();
        List<Edge> edges = MoreObjects.firstNonNull(data.getEdgesHandle(), Lists.newArrayList());
        if (CollectionUtils.isEmpty(nodes)) {
            throw new WorkflowException(String.format("\"%s\"工作流无法运行,没有配置组件", workflowName));
        }
        List<Node> startParameterNodes = nodes.stream().filter(n -> WorkflowNodeType.start_parameters.name().equals(n.getType())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(startParameterNodes)) {
            throw new WorkflowException(String.format("\"%s\"工作流无法运行,缺少\"开始输入\"组件", workflowName));
        }
        if (startParameterNodes.size() > 1) {
            throw new WorkflowException(String.format("\"%s\"工作流无法运行,存在%d个\"开始输入\"组件，请修改工作流,只保留1个\"开始输入\"组件", workflowName, startParameterNodes.size()));
        }
        Map<String, Node> runNodes = nodes.stream().filter(n -> n.getHasInputs() == 1
                && !WorkflowNodeType.start_parameters.name().equals(n.getType())).collect(Collectors.toMap(Node::getId, Function.identity(), (k1, k2) -> k1));
        if (MapUtils.isEmpty(runNodes)) {
            throw new WorkflowException(String.format("\"%s\"工作流无法运行,缺少可运行的组件", workflowName));
        }
        List<Node> outputNodes = nodes.stream().filter(n -> WorkflowNodeType.content_output.name().equalsIgnoreCase(n.getType()) || WorkflowNodeType.json_output.name().equalsIgnoreCase(n.getType()) || WorkflowNodeType.doc_output.name().equalsIgnoreCase(n.getType())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(outputNodes) && !WorkflowRunType.DEBUG.name().equalsIgnoreCase(workflowRunType)) {
            throw new WorkflowException(String.format("\"%s\"工作流无法运行,缺少\"输出\"组件", workflowName));
        }
        if (!WorkflowRunType.DEBUG.name().equalsIgnoreCase(workflowRunType) && outputNodes.size() >= 2) {
            throw new WorkflowException(String.format("\"%s\"工作流无法运行,\"输出\"组件有%d个", workflowName, outputNodes.size()));
        }
        if (CollectionUtils.isEmpty(edges) && runNodes.size() >= 2) {
            throw new WorkflowException(String.format("\"%s\"工作流无法运行,组件之间没有连线", workflowName));
        }
        List<String> firstNodeIds = new ArrayList<>(runNodes.keySet());
        if (CollectionUtils.isNotEmpty(edges)) {
            List<Edge> errorEdges = edges.stream().filter(e -> StringUtils.isBlank(e.getTarget())
                    || null == runNodes.get(e.getTarget())
                    || StringUtils.isBlank(e.getSource())
                    || null == runNodes.get(e.getSource()))
                    .filter(e -> StringUtils.isNotBlank(e.getSourceHandle()) && !e.getSourceHandle().startsWith("ifelse")
                    || StringUtils.isNotBlank(e.getTargetHandle()) && !e.getTargetHandle().startsWith("ifelse")).collect(Collectors.toList());
            LOG.info("before target edge {}", errorEdges);
            if (CollectionUtils.isNotEmpty(errorEdges)) {
                throw new WorkflowException(String.format("\"%s\"工作流无法运行,部分组件之间连线错误", workflowName));
            }
            errorEdges = edges.stream().filter(e ->
                    StringUtils.isBlank(e.getTargetHandle())
                            || StringUtils.isBlank(e.getSourceHandle())
                            || null == runNodes.get(e.getTarget()).getData().getTemplate().get(e.getTargetHandle())
                            || null == runNodes.get(e.getSource()).getData().getTemplate().get(e.getSourceHandle())
            ).collect(Collectors.toList());
            LOG.info("before SourceHandle edge {}", errorEdges);
            errorEdges = errorEdges.stream()
                    .filter(e -> StringUtils.isNotBlank(e.getSourceHandle()) && !e.getSourceHandle().startsWith("ifelse")
                            || StringUtils.isNotBlank(e.getTargetHandle()) && !e.getTargetHandle().startsWith("ifelse")).collect(Collectors.toList());
            LOG.info("after edge {}", errorEdges);
            if (CollectionUtils.isNotEmpty(errorEdges)) {
                StringBuilder errorInfo = new StringBuilder();
                for (Edge edge : errorEdges) {
                    Node source = runNodes.get(edge.getSource());
                    Node target = runNodes.get(edge.getTarget());
                    if (null != source && (null == source.getData() || null == source.getData().getTemplate() || null == source.getData().getTemplate().get(edge.getSourceHandle()))) {
                        String name = StringUtils.isNotBlank(source.getName()) ? source.getName() : source.getTypeName();
                        if (!errorInfo.isEmpty()) {
                            errorInfo.append(";");
                        }
                        LOG.info("source => {}", source);
                        errorInfo.append(String.format("（%s）组件缺失\"%s\"", name, edge.getSourceHandle()));
                    }
                    if (null != target && (null == target.getData() || null == target.getData().getTemplate() || null == target.getData().getTemplate().get(edge.getTargetHandle()))) {
                        String name = StringUtils.isNotBlank(target.getName()) ? target.getName() : target.getTypeName();
                        if (!errorInfo.isEmpty()) {
                            errorInfo.append(";");
                        }
                        LOG.info("target => {}",  target);
                        errorInfo.append(String.format("（%s）组件缺失\"%s\"", name, edge.getTargetHandle()));
                    }
                }
                if (errorInfo.length() <= 0) {
                    errorInfo.append("部分组件之间变量连线错误");
                }
                throw new WorkflowException(String.format("\"%s\"工作流无法运行:%s", workflowName, errorInfo.toString()));
            }
            List<String> sourceList = edges.stream().map(Edge::getSource).distinct().collect(Collectors.toList());
            List<String> targetList = edges.stream().map(Edge::getTarget).distinct().collect(Collectors.toList());
            firstNodeIds = new ArrayList<>(CollectionUtils.removeAll(firstNodeIds, targetList));
            firstNodeIds = new ArrayList<>(CollectionUtils.intersection(firstNodeIds, sourceList));
            if (CollectionUtils.isEmpty(firstNodeIds)) {
                throw new WorkflowException(String.format("\"%s\"工作流无法运行,没有开始运行的组件", workflowName));
            }
            List<String> lastNodeIds = new ArrayList<>(CollectionUtils.removeAll(targetList, sourceList));
            if (CollectionUtils.isEmpty(lastNodeIds)) {
                throw new WorkflowException(String.format("\"%s\"工作流无法运行,没有输出结果的组件", workflowName));
            }
            if (lastNodeIds.size() > 1 && !WorkflowRunType.DEBUG.name().equalsIgnoreCase(workflowRunType)) {
                throw new WorkflowException(String.format("\"%s\"工作流无法运行,输出结果的组件有%d个,只能配置一个输出结果组件",workflowName, lastNodeIds.size()));
            }
            if (!WorkflowNodeType.content_output.name().equals(runNodes.get(lastNodeIds.getFirst()).getType())
                    && !WorkflowNodeType.doc_output.name().equals(runNodes.get(lastNodeIds.getFirst()).getType())
                    && !WorkflowNodeType.json_output.name().equals(runNodes.get(lastNodeIds.getFirst()).getType())
                    && !WorkflowRunType.DEBUG.name().equalsIgnoreCase(workflowRunType)) {
                throw new WorkflowException(String.format("\"%s\"工作流无法运行,输出结果的组件不是输出分类下的组件", workflowName));
            }
        } else {
            if (runNodes.size() >= 2) {
                throw new WorkflowException(String.format("\"%s\"工作流无法运行,组件之间没有连线", workflowName));
            }
            //只有一个内容呈现组件
            firstNodeIds = Lists.newArrayList(outputNodes.getFirst().getId());
        }
        Map<String, WorkflowRunResult> runResults = Maps.newHashMap();
        JSONObject runInputs = DataParametersUtil.mergeParameters(startParameterNodes.getFirst().getData().getTemplate(), inputs);
        for (String firstNodeId : firstNodeIds) {
            fillRunResults(mode, runResults, 1, firstNodeId, edges, runNodes, runInputs, workflowRunType);
        }
        if (runResults.size() == 1) {
            runResults.forEach((k, v) -> v.setPosition(Position.LEAF.name()));
        }
        if (MapUtils.isEmpty(inputs)) {
            if (MapUtils.isNotEmpty(runInputs)) {
                inputs.putAll(runInputs);
            }
            return runResults;
        }
        for (String firstNodeId : firstNodeIds) {
            WorkflowRunResult firstNode = runResults.get(firstNodeId);
            JSONObject winputs = MoreObjects.firstNonNull(firstNode.getInputs(), new JSONObject());
            winputs.putAll(runInputs);
            firstNode.setInputs(winputs);
        }
        return runResults;
    }

    @Override
    public void saveWorkflowRunResult(WorkflowRun workflowRun, List<WorkflowRunResult> workflowRunResults) throws Exception {
        if (CollectionUtils.isEmpty(workflowRunResults)) {
            return;
        }

        // 设置基础信息
        for (WorkflowRunResult workflowRunResult : workflowRunResults) {
            workflowRunResult.setWorkflowId(workflowRun.getWorkflowId());
            workflowRunResult.setWorkflowRunId(workflowRun.getId());
            workflowRunResult.setUserGroupId(workflowRun.getUserGroupId());
            workflowRunResult.setUserId(workflowRun.getUserId());
            workflowRunResult.setVersionId(workflowRun.getVersionId());
            workflowRunResult.setUserName(workflowRun.getUserName());
            workflowRunResult.setAppId(workflowRun.getAppId());
            workflowRunResult.setChannel(workflowRun.getChannel());
            workflowRunResult.setWorkflowRunType(workflowRun.getType());
            workflowRunResult.setUserInfo(workflowRun.getUserInfo());
            workflowRunResult.setMode(workflowRun.getMode());
        }

        // 🔥 优化：根据总量动态调整批量大小
        // 小于100条：批量10；100-1000条：批量50；1000-5000条：批量200；5000+条：批量500
        int batchSize = calculateOptimalBatchSize(workflowRunResults.size());
        LOG.info("工作流运行记录id={} 工作流id={} 保存工作流运行结果 总数={} 批量大小={} 预计批次={}",workflowRun.getId(),workflowRun.getWorkflowId(),
                workflowRunResults.size(), batchSize, (workflowRunResults.size() + batchSize - 1) / batchSize);

        List<List<WorkflowRunResult>> batchList = ConvertUtil.convert2BatchList(workflowRunResults, batchSize);

        // 🔥 优化：对于大批量数据，使用并行保存
        if (batchList.size() > 5 && workflowRunResults.size() > 500) {
            saveBatchesParallel(workflowRun.getId(),workflowRun.getWorkflowId(),batchList);
        } else {
            for (List<WorkflowRunResult> batch : batchList) {
                saveWorkFlowRunResults(batch);
            }
        }

        // 处理子工作流结果（递归）
        List<WorkflowRunResult> subWorkflowRunResults = Lists.newArrayList();
        for (WorkflowRunResult workflowRunResult : workflowRunResults) {
            if (MapUtils.isEmpty(workflowRunResult.getSubWorkflowRunResult())) {
                continue;
            }
            workflowRunResult.getSubWorkflowRunResult().forEach((k, v) -> {
                v.setParentNodeId(workflowRunResult.getNodeId());
                v.setParentNodeName(workflowRunResult.getNodeName());
                v.setParentId(workflowRunResult.getId());
            });
            subWorkflowRunResults.addAll(workflowRunResult.getSubWorkflowRunResult().values());
        }
        saveWorkflowRunResult(workflowRun, subWorkflowRunResults);
    }

    /**
     * 并行保存批次
     */
    private void saveBatchesParallel(Long workflowRunId,Long workflowId,List<List<WorkflowRunResult>> batchList) throws Exception {

        CompletableFuture.allOf(batchList.stream()
                .map(batch -> CompletableFuture.runAsync(() -> {
                    try {
                        saveWorkFlowRunResults(batch);
                    } catch (Exception e) {
                        LOG.error("工作流运行记录id={} 工作流id={} 并行保存批次失败 batchSize={}",workflowRunId,workflowId, batch.size(), e);
                        throw new RuntimeException(e);
                    }
                })).toArray(CompletableFuture[]::new)).join();
    }

    /**
     * 计算最优批量大小
     */
    private int calculateOptimalBatchSize(int totalSize) {
        if (totalSize < 100) {
            return saveBatchSize;
        } else if (totalSize < 1000) {
            return 50;
        } else if (totalSize < 5000) {
            return 200;
        } else {
            return 500;
        }
    }

    @Override
    public void updateWorkflowRunStatusByQueque(List<WorkflowRunResult> workflowRunResults, Boolean force) throws Exception {
        Long now = DateTime.now().getMillis();
        List<Long> workflowRunResultIds = Lists.newArrayList();
        
        // 优化：批量查询所有父节点，避免 N+1 问题
        Set<Long> parentIdsToQuery = new HashSet<>();
        for (WorkflowRunResult workflowRunResult : workflowRunResults) {
            workflowRunResultIds.add(workflowRunResult.getId());
            if (null != workflowRunResult.getParentId() && workflowRunResult.getParentId() > 0L) {
                parentIdsToQuery.add(workflowRunResult.getId());
            }
        }
        
        // 批量查询所有父节点（一次数据库调用）
        if (CollectionUtils.isNotEmpty(parentIdsToQuery)) {
            Set<Long> allParentIds = batchGetParentIds(new ArrayList<>(parentIdsToQuery), 
                workflowRunResults.getFirst().getWorkflowRunId(),
                workflowRunResults.getFirst().getWorkflowRunType());
            workflowRunResultIds.addAll(allParentIds);
        }
        
        updateWorkflowRunResult(workflowRunResultIds, now, WorkflowRunStatus.QUEUED.name(), StringUtils.EMPTY, force);
        List<Long> workFlowRunIds = workflowRunResults.stream().map(WorkflowRunResult::getWorkflowRunId).distinct().collect(Collectors.toList());
        updateWorkflowRun(workFlowRunIds, now, WorkflowRunStatus.QUEUED.name());
        workflowRunResults.forEach(w -> {
            LOG.warn("{} 节点={}({}) 工作流运行id={} 工作流id={} 入队={}"
                    , w.getNodeType(), w.getNodeName(), w.getId(), w.getWorkflowRunId(), w.getWorkflowId(), w.getNodeQueueName());
        });
    }


    @Override
    public void updateWorkflowRunStatusByRunning(WorkflowRunResult workflowRunResult) throws Exception {
        List<Long> workflowRunResultIds = Lists.newArrayList(workflowRunResult.getId());
        List<Long> parentIds = null;
        if (null != workflowRunResult.getParentId() && workflowRunResult.getParentId() > 0L) {
            parentIds = workflowRunResultClient.getParent(workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), null, workflowRunResult.getWorkflowRunType(),Boolean.TRUE).getData().stream()
                    .map(BaseModel::getId).distinct().collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(parentIds)) {
                workflowRunResultIds.addAll(parentIds);
            }
        }
        Long now = DateTime.now().getMillis();
        // 优化：updateWorkflowRunResult 已经更新了所有节点的状态和 updateAt（包括父节点），无需重复更新
        updateWorkflowRunResult(workflowRunResultIds, now, WorkflowRunStatus.RUNNING.name(), StringUtils.EMPTY, Boolean.FALSE);
        updateWorkflowRun(Lists.newArrayList(workflowRunResult.getWorkflowRunId()), now, WorkflowRunStatus.RUNNING.name());
        LOG.warn("{} 开始运行 节点={}({}) 工作流运行id={} 工作流id={}"
                , workflowRunResult.getNodeType(), workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId());
    }

    @Override
    public void updateWorkflowRunStatusByFailure(WorkflowRunResult workflowRunResult, String errorInfo,Long finishAt) throws Exception {
        workflowRunResult.setStatus(WorkflowRunStatus.FAILED.name());
        workflowRunResult.setFinishAt(finishAt);
        if (StringUtils.isNotBlank(errorInfo)) {
            workflowRunResult.setErrorInfo(errorInfo);
        } else {
            workflowRunResult.setErrorInfo(StringUtils.EMPTY);
        }
        workflowRunResultClient.save(Lists.newArrayList(workflowRunResult));
        LOG.warn("{} 运行失败 节点={}({}) 工作流运行id={} 工作流id={} 失败原因={} 花费时间={}毫秒"
                , workflowRunResult.getNodeType(), workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(), errorInfo, (workflowRunResult.getFinishAt() - workflowRunResult.getRunAt()));

        //忽略运行错误
        if (null != workflowRunResult.getIgnoreError() && workflowRunResult.getIgnoreError() >= 1) {
            updateWorkflowRunResultFinish(finishAt, workflowRunResult);
        } else {
            Map<Long, WorkflowRunResult> parentNodes = null;
            if (null != workflowRunResult.getParentId() && workflowRunResult.getParentId() > 0L) {
                parentNodes = workflowRunResultClient.getParent(workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), null, workflowRunResult.getWorkflowRunType(),Boolean.FALSE).getData().stream().collect(Collectors.toMap(WorkflowRunResult::getId, Function.identity(), (k1, k2) -> k1));
            }
            Boolean isUpdateWorkflowRunStatusFailure = isUpdateWorkflowRunStatusFailure(workflowRunResult, parentNodes);
            if (!WorkflowRunType.DEBUG.name().equalsIgnoreCase(workflowRunResult.getWorkflowRunType()) && isUpdateWorkflowRunStatusFailure) {
                Map<String, String> resetParam = Maps.newHashMap();
                resetParam.put("workflowRunId", String.valueOf(workflowRunResult.getWorkflowRunId()));
                resetParam.put("workflowRunType", workflowRunResult.getWorkflowRunType());
                resetParam.put("status", String.join(",", WorkflowRunStatus.QUEUED.name(), WorkflowRunStatus.RUNNING.name()));
                workflowRunResultClient.update(resetParam, Lists.newArrayList("status", "runAt", "updateAt"), Lists.newArrayList(WorkflowRunStatus.NOT_STARTED.name(), "0", String.valueOf(DateTime.now().getMillis())));
                if(WorkflowRunType.RUN.name().equalsIgnoreCase(workflowRunResult.getWorkflowRunType())) {
                    WorkflowRun workflowRun = workflowRunClient.getById(workflowRunResult.getWorkflowRunId()).getData();
                    if (null == workflowRun) {
                        return;
                    }
                    workflowRun.setStatus(workflowRunResult.getStatus());
                    workflowRun.setFinishAt(workflowRunResult.getFinishAt());
                    workflowRun.setOutputText(workflowRunResult.getErrorInfo());
                    workflowRun.setOutputs(workflowRunResult.getOutputs());
                    LOG.warn("工作流运行失败 工作流运行记录={}({}) 工作流id={} 失败原因={} 花费时间={}毫秒"
                            , workflowRun.getName(), workflowRun.getId(), workflowRun.getWorkflowId(), errorInfo, (workflowRun.getFinishAt() - workflowRun.getRunAt()));
                    Response<Long> result = notify(workflowRun);
                    workflowRun.setNotifyLogId(result.getData());
                    workflowRunClient.save(Lists.newArrayList(workflowRun));
                }
            }else if(MapUtils.isNotEmpty(parentNodes)){
                List<Long> parentIds = new ArrayList<>(parentNodes.keySet());
                workflowRunResultClient.update(parentIds, Lists.newArrayList("updateAt"), Lists.newArrayList( String.valueOf(DateTime.now().getMillis())));
                LOG.warn("{} 运行失败 修改父节点更新时间 节点={}({}) 工作流运行id={} 工作流id={} 父节点id={}"
                        , workflowRunResult.getNodeType(), workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(),parentIds);
            }
        }
        if (WorkflowRunType.RUN.name().equalsIgnoreCase(workflowRunResult.getWorkflowRunType())) {
            updateRunNodeCount(workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowRunType());
        }
    }

    private Boolean isUpdateWorkflowRunStatusFailure(WorkflowRunResult
                                                             workflowRunResult, Map<Long, WorkflowRunResult> parentNodes) throws Exception {
        if (MapUtils.isEmpty(parentNodes) || null == workflowRunResult.getParentId() || workflowRunResult.getParentId() <= 0L) {
            return Boolean.TRUE;
        }
        WorkflowRunResult parent = parentNodes.get(workflowRunResult.getParentId());
        if (null == parent) {
            return Boolean.TRUE;
        }
        if (null == parent.getIgnoreError() || parent.getIgnoreError() <= 0L) {
            parent.setStatus(workflowRunResult.getStatus());
            parent.setFinishAt(workflowRunResult.getFinishAt());
            parent.setErrorInfo(workflowRunResult.getErrorInfo());
            workflowRunResultClient.save(Lists.newArrayList(parent));
            LOG.warn("{} 运行失败 节点={}({}) 工作流运行id={} 工作流id={} 失败原因={} 花费时间={}毫秒"
                    , parent.getNodeType(), parent.getNodeName(), parent.getId(), parent.getWorkflowRunId(), parent.getWorkflowId(), workflowRunResult.getErrorInfo(), (parent.getFinishAt() - parent.getRunAt()));
            return isUpdateWorkflowRunStatusFailure(parent, parentNodes);
        }
        updateWorkflowRunResultFinish(workflowRunResult.getFinishAt(), workflowRunResult);
        return Boolean.FALSE;
    }

    @Override
    public void updateWorkflowRunFinish(WorkflowRunResult workflowRunResult,Long finishAt) throws Exception {
        workflowRunResult.setStatus(WorkflowRunStatus.FINISHED.name());
        workflowRunResult.setFinishAt(finishAt);
        workflowRunResult.setErrorInfo(StringUtils.EMPTY);
        workflowRunResultClient.save(Lists.newArrayList(workflowRunResult));
        if (workflowRunResult.getDegree() == 0 && WorkflowNodeType.start_parameters.name().equalsIgnoreCase(workflowRunResult.getNodeType())) {
            Map<String, String> params = Maps.newHashMap();
            params.put("degree", "0");
            params.put("nodeType", workflowRunResult.getNodeType());
            params.put("workflowRunType", workflowRunResult.getWorkflowRunType());
            params.put("workflowRunId", String.valueOf(workflowRunResult.getWorkflowRunId()));
            List<WorkflowRunResult> uploadFiles = workflowRunResultClient.find(1, Integer.MAX_VALUE, params).getData().getList();
            List<Long> unfinishIds = uploadFiles.stream().filter(w -> !WorkflowRunStatus.FINISHED.name().equalsIgnoreCase(w.getStatus())).map(w -> w.getId()).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(unfinishIds)) {
                List<Long> nextRunIds = pushTargetNodes(workflowRunResult);
                LOG.warn("{} 运行完成 节点={}({}) 工作流运行id={} 工作流id={} 花费时间={}毫秒 准备运行节点id={}"
                        , workflowRunResult.getNodeType(), workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(), (workflowRunResult.getFinishAt() - workflowRunResult.getRunAt()), nextRunIds);

            } else {
                LOG.warn("{} 运行完成 节点={}({}) 工作流运行id={} 工作流id={} 花费时间={}毫秒 未运行完成节点id={}"
                        , workflowRunResult.getNodeType(), workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(), (workflowRunResult.getFinishAt() - workflowRunResult.getRunAt()), unfinishIds);
            }
        } else {
            LOG.warn("{} 运行完成 节点={}({}) 工作流运行id={} 工作流id={} 花费时间={}毫秒", workflowRunResult.getNodeType(), workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(), (workflowRunResult.getFinishAt() - workflowRunResult.getRunAt()));
            updateWorkflowRunResultFinish(finishAt, workflowRunResult);
        }
        if (!WorkflowRunType.DEBUG.name().equalsIgnoreCase(workflowRunResult.getWorkflowRunType())) {
            updateRunNodeCount(workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowRunType());
        }
    }

    public void updateWorkflowRunResultFinish(Long finishAt, WorkflowRunResult workflowRunResult) throws Exception {
        if (Position.LEAF.name().equals(workflowRunResult.getPosition())) {
            updateWorkflowRunResultLeafFinish(finishAt, workflowRunResult);
        }else if(Position.CHILD.name().equalsIgnoreCase(workflowRunResult.getPosition())){
            updateWorkflowRunResultBranchFinish(finishAt, workflowRunResult);
        } else {
            if (null != workflowRunResult.getParentId() && workflowRunResult.getParentId() > 0L) {
                Map<Long, WorkflowRunResult> parentNodes = workflowRunResultClient.getParent(workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), null, workflowRunResult.getWorkflowRunType(),Boolean.TRUE).getData().stream().collect(Collectors.toMap(WorkflowRunResult::getId, Function.identity(), (k1, k2) -> k1));
                if(MapUtils.isNotEmpty(parentNodes)){
                    List<Long> parentIds = new ArrayList<>(parentNodes.keySet());
                    workflowRunResultClient.update(parentIds, Lists.newArrayList("updateAt"), Lists.newArrayList( String.valueOf(DateTime.now().getMillis())));
                    LOG.warn("{} 运行完成 修改父节点更新时间 节点={}({}) 工作流运行id={} 工作流id={} 父节点id={}"
                            , workflowRunResult.getNodeType(), workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(),parentIds);
                }
            }
            List<Long> nextRunIds = pushTargetNodes(workflowRunResult);
            if (!WorkflowRunMode.REALTIME.name().equalsIgnoreCase(workflowRunResult.getMode()) && LOG.isInfoEnabled()) {
                LOG.warn("{} 运行完成 节点={}({}) 工作流运行id={} 工作流id={} 花费时间={}毫秒 准备运行节点id={}"
                        , workflowRunResult.getNodeType(), workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(), (workflowRunResult.getFinishAt() - workflowRunResult.getRunAt()), nextRunIds);
            }
        }
    }

    @Override
    public void updateWorkflowRunResultLeafFinish(Long finishAt, WorkflowRunResult workflowRunResult) throws Exception{
        if (null != workflowRunResult.getParentId() && workflowRunResult.getParentId() > 0L) {
            WorkflowRunResult parent = workflowRunResultClient.getById(workflowRunResult.getParentId()).getData();
            if (null != parent) {
                parent.setStatus(WorkflowRunStatus.FINISHED.name());
                parent.setFinishAt(finishAt);
                parent.setErrorInfo(StringUtils.EMPTY);
                parent.setOutputs(workflowRunResult.getOutputs());
                workflowRunResultClient.save(Lists.newArrayList(parent));
                LOG.warn("{} 运行完成 节点={}({}) 序号={} 工作流运行id={} 工作流id={} 花费时间={}毫秒", parent.getNodeType(), parent.getNodeName(), parent.getId(),parent.getSubWorkflowRunIndex(), parent.getWorkflowRunId(), parent.getWorkflowId(), (parent.getFinishAt() - parent.getRunAt()));
                updateWorkflowRunResultBranchFinish(finishAt,parent);
            }
        } else if (WorkflowRunType.RUN.name().equalsIgnoreCase(workflowRunResult.getWorkflowRunType())) {
            LOG.warn("{} 运行完成 节点={}({}) 工作流运行id={} 工作流id={} 花费时间={}毫秒", workflowRunResult.getNodeType(), workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(), (workflowRunResult.getFinishAt() - workflowRunResult.getRunAt()));
            WorkflowRun workflowRun = workflowRunClient.getById(workflowRunResult.getWorkflowRunId()).getData();
            if (null == workflowRun || WorkflowRunStatus.FINISHED.name().equalsIgnoreCase(workflowRun.getStatus()) || WorkflowRunStatus.FAILED.name().equalsIgnoreCase(workflowRun.getStatus())) {
                return;
            }
            workflowRun.setStatus(WorkflowRunStatus.FINISHED.name());
            workflowRun.setFinishAt(finishAt);
            workflowRun.setOutputs(workflowRunResult.getOutputs());
            Response<Long> result = notify(workflowRun);
            workflowRun.setNotifyLogId(result.getData());
            workflowRunClient.save(Lists.newArrayList(workflowRun));
            LOG.warn("工作流运行完成 工作流运行={}({}) 工作流id={} 花费时间={}毫秒"
                    , workflowRun.getName(), workflowRun.getId(), workflowRun.getWorkflowId(), (workflowRun.getFinishAt() - workflowRun.getRunAt()));
            if (workflowRunVersion && WorkflowNodeType.doc_output.name().equals(workflowRun.getOutputType())) {
                WorkflowRunVersion version = new WorkflowRunVersion();
                version.setAppId(workflowRun.getAppId());
                version.setUserId(workflowRun.getUserId());
                version.setUserName(workflowRun.getUserName());
                version.setChannel(workflowRun.getChannel());
                version.setUserGroupId(workflowRun.getUserGroupId());
                version.setWorkflowId(workflowRun.getWorkflowId());
                version.setWorkflowRunId(workflowRun.getId());
                version.setName("文档生成");
                version.setOutputs(workflowRun.getOutputs());
                workflowRunVersionClient.save(Lists.newArrayList(version));
                LOG.warn("文档生成 工作流运行={}({}) 工作流id={}", workflowRun.getName(), workflowRun.getId(), workflowRun.getWorkflowId());
            }
        }
    }

    @Override
    public void updateWorkflowRunResultBranchFinish(Long finishAt, WorkflowRunResult parent) throws Exception{
        WorkflowRunResult branch = null;
        if (null != parent.getParentId() && parent.getParentId() > 0L) {
            branch = workflowRunResultClient.getById(parent.getParentId()).getData();
            if (null != branch && !Position.BRANCH.name().equalsIgnoreCase(branch.getPosition())) {
                branch = null;
            }
        }
        if (null != branch) {
            Map<String, String> subRootParam = Maps.newHashMap();
            subRootParam.put("parentId", branch.getId().toString());
            subRootParam.put("appId", branch.getAppId().toString());
            subRootParam.put("workflowId",branch.getWorkflowId().toString());
            subRootParam.put("workflowRunType", branch.getWorkflowRunType());
            subRootParam.put("workflowRunId",branch.getWorkflowRunId().toString());
            subRootParam.put("includeFields", "id,outputs,status,subWorkflowRunInputText,subWorkflowOutputType,subWorkflowRunIndex");
            List<WorkflowRunResult> runList = workflowRunResultClient.find(1, Integer.MAX_VALUE, subRootParam).getData().getList().stream().sorted(comparing(WorkflowRunResult::getSubWorkflowRunIndex)).toList();
            List<WorkflowRunResult> notFinish = runList.stream().filter(r -> !WorkflowRunStatus.FINISHED.name().equalsIgnoreCase(r.getStatus()) && !WorkflowRunStatus.FAILED.name().equalsIgnoreCase(r.getStatus())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(notFinish)) {
                branch.setStatus(WorkflowRunStatus.FINISHED.name());
                branch.setFinishAt(finishAt);
                branch.setErrorInfo(StringUtils.EMPTY);
                List outputValue = Lists.newArrayList();
                for (WorkflowRunResult r : runList) {
                    if (MapUtils.isEmpty(r.getOutputs()) || null == r.getOutputs().get(WorkflowJSONKey.output.name()) || !(r.getOutputs().get(WorkflowJSONKey.output.name()) instanceof Map)) {
                        outputValue.add(null);
                        continue;
                    }
                    outputValue.add(r.getOutputs().getJSONObject(WorkflowJSONKey.output.name()).get(WorkflowJSONKey.value.name()));
                }
                List<JSONObject> standard_chart = DataParametersUtil.getValuesByTemplate(WorkflowJSONKey.standard_chart.name(), branch.getInputs(), JSONObject.class, Lists.newArrayList());
                JSONObject branchOutputs = WorkflowUtil.mergeOutputs(runList.getFirst(), Lists.newArrayList(WorkflowUtil.createOutput(WorkflowJSONKey.output.name(), branch.getSubWorkflowOutputType(), outputValue)));
                branch.setOutputs(branchOutputs);
                workflowRunResultClient.save(Lists.newArrayList(branch));
                List<Long> nextRunIds = pushTargetNodes(branch);
                if (!WorkflowRunMode.REALTIME.name().equalsIgnoreCase(parent.getMode())) {
                    LOG.warn("{} 运行完成 节点={}({}) 输入列表大小={} 输出结果大小={} 工作流运行id={} 工作流id={}  花费时间={}毫秒 准备运行节点id={}"
                            , branch.getNodeType(), branch.getNodeName(), branch.getId(),standard_chart.size(),outputValue.size(), branch.getWorkflowRunId(), branch.getWorkflowId(), (branch.getFinishAt() - branch.getRunAt()), nextRunIds);
                    if(standard_chart.size() != outputValue.size()){
                        LOG.warn("{} 输出结果异常 节点={}({}) 工作流运行id={} 工作流id={} 输入列表={} 输出结果={}"
                                , branch.getNodeType(), branch.getNodeName(), branch.getId(), branch.getWorkflowRunId(), branch.getWorkflowId(),standard_chart,outputValue);

                    }
                }
            } else {
//                workflowRunResultClient.update(Lists.newArrayList(branch.getId()),Lists.newArrayList("updateAt"),Lists.newArrayList(String.valueOf(DateTime.now().getMillis())));
                if (LOG.isInfoEnabled()) {
                    LOG.info("{} 不运行 节点={}({}) 工作流运行id={} 工作流id={} 等待未运行完成的子节点id={}"
                            , branch.getNodeType(), branch.getNodeName(), branch.getId(), branch.getWorkflowRunId(), branch.getWorkflowId(), notFinish.stream().map(n -> n.getId()).collect(Collectors.toList()));
                }
            }
        } else {
            List<Long> nextRunIds = pushTargetNodes(parent);
            if (!WorkflowRunMode.REALTIME.name().equalsIgnoreCase(parent.getMode()) && LOG.isInfoEnabled()) {
                LOG.warn("{} 运行完成 节点={}({}) 工作流运行id={} 工作流id={} 花费时间={}毫秒 准备运行节点id={}"
                        , parent.getNodeType(), parent.getNodeName(), parent.getId(), parent.getWorkflowRunId(), parent.getWorkflowId(), (parent.getFinishAt() - parent.getRunAt()), nextRunIds);

            }
        }
    }

    @Override
    public Response<Long> notify(WorkflowRun workflowRun) {
        Response<Long> error = Response.error(null);
        if (null == workflowRun || !WorkflowRunStatus.FINISHED.name().equalsIgnoreCase(workflowRun.getStatus())
                && !WorkflowRunStatus.FAILED.name().equalsIgnoreCase(workflowRun.getStatus()) || null == workflowRun.getNotifyConfig() || StringUtils.isBlank(workflowRun.getNotifyConfig().getUrl())) {
            error.getMessage().setMessage("工作流未运行结束或者无通知配置");
            return error;
        }
        NotifyConfig notifyConfig = workflowRun.getNotifyConfig();
        OpenRunOutput output = new OpenRunOutput();
        output.setWorkflowRunId(workflowRun.getId());
        output.setStatus(workflowRun.getStatus());
        output.setName(workflowRun.getName());
        output.setOutputType(workflowRun.getOutputType());
        if (WorkflowJSONKey.json_output.name().equalsIgnoreCase(workflowRun.getOutputType()) && WorkflowRunStatus.FINISHED.name().equalsIgnoreCase(workflowRun.getStatus())) {
            output.setOutputs(WorkflowUtil.getJsonOutputs(workflowRun.getOutputs()));
        }
        JSONObject data = (JSONObject) JSON.toJSON(output, DataParametersUtil.JSONWRITER_TOJSON_FEATURE);
        NotifyLog notifyLog = new NotifyLog();
        notifyLog.setData(data);
        notifyLog.setChannel(workflowRun.getChannel());
        notifyLog.setUserInfo(workflowRun.getUserInfo());
        notifyLog.setTimeout(notifyConfig.getTimeout());
        notifyLog.setAppId(workflowRun.getAppId());
        notifyLog.setRefererId(String.valueOf(workflowRun.getId()));
        notifyLog.setHeaders(notifyConfig.getHeaders());
        notifyLog.setMethod(notifyConfig.getMethod());
        notifyLog.setType(WorkflowRun.class.getSimpleName());
        notifyLog.setUrl(notifyConfig.getUrl());
        initNotifyConfig(workflowRun.getChannel(), workflowRun.getAppId(), notifyConfig);
        if (CollectionUtils.isNotEmpty(notifyConfig.getRules())) {
            List<NotifyPlan> notifyPlans = notifyConfig.getRules().stream().filter(StringUtils::isNotBlank).map(n -> {
                NotifyPlan notifyPlan = new NotifyPlan();
                notifyPlan.setTimeout(notifyConfig.getTimeout());
                notifyPlan.setRule(n);
                return notifyPlan;
            }).collect(Collectors.toList());
            notifyLog.setPlan(notifyPlans);
        }
        try {

            Response<List<NotifyLog>> response = notifyRepository.push(Lists.newArrayList(notifyLog));
            if (CollectionUtils.isNotEmpty(response.getData())) {
                return Response.ok(response.getData().getFirst().getId());
            }
            error.setMessage(response.getMessage());
            if (LOG.isInfoEnabled()) {
                LOG.info("工作流运行结束推送通知成功 {}({}) 通知={}", workflowRun.getName(), workflowRun.getId(), new String(JSON.toJSONBytes(response.getData())));
            }
            return error;
        } catch (Exception e) {
            LOG.error("工作流运行结束推送通知失败 {}({}) 通知={} ", workflowRun.getName(), workflowRun.getId(), new String(JSON.toJSONBytes(notifyLog)), e);
            error.getMessage().setMessage(e.getMessage());
            return error;
        }

    }

    @Override
    public void initNotifyConfig(String channel, Long appId, NotifyConfig notifyConfig) {
        if (null == notifyConfig || StringUtils.isBlank(notifyConfig.getUrl()) || CollectionUtils.isNotEmpty(notifyConfig.getRules())) {
            return;
        }
        try {
            Map<String, String> appParams = Maps.newHashMap();
            appParams.put("channel", channel);
            appParams.put("appId", String.valueOf(appId));
            appParams.put("productConfig.code", "workflow_run_notify_config");
            List<ApplicationConfig> applicationConfigs = applicationConfigClient.find(1, 1, appParams).getData().getList();
            if (CollectionUtils.isEmpty(applicationConfigs)) {
                return;
            }
            ApplicationConfig applicationConfig = applicationConfigs.getFirst();
            JSONObject configData = ApplicationConfigUtil.getApplicationConfigData(applicationConfig.getProductConfig().getData(), applicationConfig.getData(), Boolean.FALSE);
            if (MapUtils.isEmpty(configData)) {
                return;
            }
            List<FieldOptions> fieldOptions = DataParametersUtil.getValuesByTemplate("run_failed_retry_rules", configData.getJSONObject(WorkflowJSONKey.template.name()), FieldOptions.class, Lists.newArrayList());
            if (CollectionUtils.isNotEmpty(fieldOptions)) {
                List<String> rules = fieldOptions.stream().map(FieldOptions::getValue).collect(Collectors.toList());
                notifyConfig.setRules(rules);
            }
        } catch (Exception e) {
            LOG.error("getInitNotifyConfig channel={} appId={} ", channel, appId, e);
        }
    }

    @Override
    public void deleteChildrenWorkflowRunResult(Long workflowId,Long workflowRunId, List<Long> parentIds, String workflowRunType) throws Exception {
        if (CollectionUtils.isEmpty(parentIds)) {
            return;
        }
        Map<String, String> params = Maps.newHashMap();
        params.put("workflowId", String.valueOf(workflowId));
        params.put("workflowRunId", String.valueOf(workflowRunId));
        params.put("parentId", parentIds.stream().map(Object::toString).distinct().collect(Collectors.joining(",")));
        params.put("workflowRunType", workflowRunType);
        params.put("includeFields", "id");
        List<Long> ids = workflowRunResultClient.find(1, Integer.MAX_VALUE, params).getData().getList().stream().map(BaseModel::getId).collect(Collectors.toList());
        deleteChildrenWorkflowRunResult(workflowId,workflowRunId, ids, workflowRunType);
        params.remove("includeFields");
        workflowRunResultClient.delete(params);
    }


    @Override
    public void updateRunNodeCount(Long workflowRunId, String workflowRunType) throws Exception {
        Integer errorCount = getWorkflowRunNodeCount(workflowRunId, WorkflowRunStatus.FAILED.name(), workflowRunType);
        Integer successCount = getWorkflowRunNodeCount(workflowRunId, WorkflowRunStatus.FINISHED.name(), workflowRunType);
        successCount += getWorkflowRunNodeCount(workflowRunId, WorkflowRunStatus.NOT_RUN.name(), workflowRunType);
        Integer total = getWorkflowRunNodeCount(workflowRunId, null, workflowRunType);
        List<String> updateKeys = Lists.newArrayList("runNodeErrorCount", "runNodeSuccessCount", "runNodeCount", "updateAt");
        List<String> updateValues = Lists.newArrayList(String.valueOf(MoreObjects.firstNonNull(errorCount, 0)), String.valueOf(MoreObjects.firstNonNull(successCount, 0)), String.valueOf(MoreObjects.firstNonNull(total, 0)), String.valueOf(DateTime.now().getMillis()));
        workflowRunClient.update(Lists.newArrayList(workflowRunId), updateKeys, updateValues);
    }


    @Override
    public WorkflowRunResult getSubWorkflowRunResult(String workflowRunType, String mode, Workflow subWorkflow, JSONObject subWorkflowInputs, WorkflowData subWorkflowData) throws Exception {
        WorkflowRunResult workflowRunResult = new WorkflowRunResult();
//        LOG.info("子工作流subWorkflowInputs：{}",subWorkflowInputs);
        workflowRunResult.setInputs(subWorkflowInputs);
        if (!WorkflowRunMode.COPY.name().equalsIgnoreCase(mode)) {
            workflowRunResult.setMode(mode);
        }
        workflowRunResult.setSubWorkflowId(subWorkflow.getId());
        workflowRunResult.setSubWorkflowRunInputText(WorkflowUtil.getInputText(subWorkflowInputs));
        workflowRunResult.setSubWorkflowInputsMd5(WorkflowUtil.getInputsMd5(subWorkflowInputs));
        workflowRunResult.setSubWorkflowOutputType(subWorkflow.getOutputType());
        workflowRunResult.setSubWorkflowDataMd5(subWorkflow.getWorkflowDataMd5());
        workflowRunResult.setWorkflowRunType(workflowRunType);
        workflowRunResult.setPosition(Position.CHILD.name());
        CopyRunResult copyRunResult = null;
        if (WorkflowRunMode.COPY.name().equalsIgnoreCase(mode)) {
            copyRunResult = getWorkflowRunResultCopy(subWorkflow.getId(), subWorkflow.getOutputType(), workflowRunResult.getSubWorkflowInputsMd5(), workflowRunResult.getSubWorkflowDataMd5(), subWorkflow.getCopyTimeout());
        }
        Map<String, WorkflowRunResult> subWorkflowRunResult = null;
        if (null != copyRunResult) {
            subWorkflowRunResult = copyRunResult.getToData().stream().collect(Collectors.toMap(WorkflowRunResult::getNodeId, Function.identity(), (k1, k2) -> k1));
            workflowRunResult.setMode(WorkflowRunMode.COPY.name());
            workflowRunResult.setCopyTimeout(subWorkflow.getCopyTimeout());
            if (null != copyRunResult.getFromWorkflowRun()) {
                workflowRunResult.setCopyUpdateAt(copyRunResult.getFromWorkflowRun().getFinishAt());
                workflowRunResult.setCopyWorkflowRunId(copyRunResult.getFromWorkflowRun().getId());
                workflowRunResult.setOutputs(copyRunResult.getFromWorkflowRun().getOutputs());
            } else {
                workflowRunResult.setCopyUpdateAt(copyRunResult.getFromWorkflowRunResult().getFinishAt());
                workflowRunResult.setCopyWorkflowRunId(copyRunResult.getFromWorkflowRunResult().getWorkflowRunId());
                workflowRunResult.setCopyWorkflowRunResultId(copyRunResult.getFromWorkflowRunResult().getId());
                workflowRunResult.setOutputs(copyRunResult.getFromWorkflowRunResult().getOutputs());
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("工作流调用组件选择的工作流{}({}),复制已有的运行结果 copyTimeout={} copyUpdateAt={} copyRunId={} copyRunResultId={}", subWorkflow.getName(), subWorkflow.getId(), workflowRunResult.getCopyTimeout(), workflowRunResult.getCopyUpdateAt(), workflowRunResult.getCopyWorkflowRunId(), workflowRunResult.getCopyWorkflowRunResultId());
            }
        } else {
            subWorkflowRunResult = getWorkflowRunResult(mode, subWorkflow.getName(), subWorkflowData, JSON.copy(subWorkflowInputs, DataParametersUtil.JSONWRITER_FEATURE), workflowRunType);
        }
        workflowRunResult.setSubWorkflowRunResult(subWorkflowRunResult);
        return workflowRunResult;
    }

    @Override
    public WorkflowRunResult getWorkflowRunResultStatus(Long workflowRunResultId) throws Exception {
        Map<String, String> params = Maps.newHashMap();
        params.put("id", String.valueOf(workflowRunResultId));
        params.put("includeFields", "id,workflowRunId,status,workflowRunType,nodeType,nodeQueueName,ignoreError,parentId");
        List<WorkflowRunResult> workflowRunResult = workflowRunResultClient.find(1, 1, params).getData().getList();
        if (CollectionUtils.isEmpty(workflowRunResult)) {
            return null;
        }
        return workflowRunResult.getFirst();
    }

    @Override
    public void updateWorkflowRunResult(List<Long> workflowRunResultIds, Long now, String status, String errorInfo, Boolean force) throws Exception {
        String nowStr = String.valueOf(now);
        Map<String, String> updateValues = Maps.newHashMap();
        updateValues.put("status", status);
        updateValues.put("updateAt", nowStr);
        DBUpdate<Long> updateQuery = new DBUpdate<>();
        DBQueryItem idItem = new DBQueryItem();
        idItem.setField("id");
        idItem.setValues(workflowRunResultIds);
        DBQueryItem statusQueryItem = new DBQueryItem();
        statusQueryItem.setField("status");
        if (WorkflowRunStatus.NOT_STARTED.name().equals(status) || WorkflowRunStatus.NOT_RUN.name().equals(status)) {
            statusQueryItem.setOperation(DBOperation.NIN);
            statusQueryItem.setValues(Lists.newArrayList(WorkflowRunStatus.FAILED.name(), WorkflowRunStatus.FINISHED.name(), WorkflowRunStatus.NOT_RUN.name()));
            updateValues.put("runAt", "0");
        } else if (WorkflowRunStatus.QUEUED.name().equalsIgnoreCase(status)) {
            statusQueryItem.setOperation(DBOperation.IN);
            statusQueryItem.setValues(Lists.newArrayList(WorkflowRunStatus.NOT_STARTED.name(),WorkflowRunStatus.QUEUED.name()));
            updateValues.put("runAt", "0");
        } else if (WorkflowRunStatus.RUNNING.name().equalsIgnoreCase(status)) {
            statusQueryItem.setOperation(DBOperation.NIN);
            statusQueryItem.setValues(Lists.newArrayList(WorkflowRunStatus.RUNNING.name()
                    , WorkflowRunStatus.FAILED.name(), WorkflowRunStatus.FINISHED.name(), WorkflowRunStatus.NOT_RUN.name()));
            updateValues.put("runAt", nowStr);
        } else if (WorkflowRunStatus.FINISHED.name().equalsIgnoreCase(status)) {
            statusQueryItem.setOperation(DBOperation.NIN);
            statusQueryItem.setValues(Lists.newArrayList(WorkflowRunStatus.FAILED.name(), WorkflowRunStatus.FINISHED.name(), WorkflowRunStatus.NOT_RUN.name()));
            updateValues.put("finishAt", nowStr);
        } else if (WorkflowRunStatus.FAILED.name().equalsIgnoreCase(status)) {
            statusQueryItem.setOperation(DBOperation.NIN);
            statusQueryItem.setValues(Lists.newArrayList(WorkflowRunStatus.FAILED.name(), WorkflowRunStatus.FINISHED.name(), WorkflowRunStatus.NOT_RUN.name()));
            updateValues.put("finishAt", nowStr);
            if (StringUtils.isNotBlank(errorInfo)) {
                updateValues.put("errorInfo", errorInfo);
            }
        }
        if (force) {
            updateQuery.setAndQuery(Lists.newArrayList(idItem));
        } else {
            updateQuery.setAndQuery(Lists.newArrayList(idItem, statusQueryItem));
        }
        updateQuery.setUpdateValues(updateValues);
        workflowRunResultClient.updateByQuery(updateQuery);
    }

    @Override
    public WorkflowRun getWorkflowRunStatus(Long workflowRunId) throws Exception {
        Map<String, String> params = Maps.newHashMap();
        params.put("id", String.valueOf(workflowRunId));
        params.put("includeFields", "id,type,status,outputType,userInfo");
        List<WorkflowRun> workflowRun = workflowRunClient.find(1, 1, params).getData().getList();
        if (CollectionUtils.isEmpty(workflowRun)) {
            return null;
        }
        return workflowRun.getFirst();
    }

    @Override
    public void updateWorkflowRun(List<Long> workflowRunIds, Long now, String status) throws Exception {
        String nowStr = String.valueOf(now);
        Map<String, String> updateValues = Maps.newHashMap();
        updateValues.put("status", status);
        updateValues.put("updateAt", nowStr);
        DBUpdate<Long> updateQuery = new DBUpdate<>();
        DBQueryItem idItem = new DBQueryItem();
        idItem.setField("id");
        idItem.setValues(workflowRunIds);
        DBQueryItem statusQueryItem = new DBQueryItem();
        statusQueryItem.setField("status");
        if (WorkflowRunStatus.QUEUED.name().equalsIgnoreCase(status)) {
            statusQueryItem.setOperation(DBOperation.EQ);
            statusQueryItem.setValues(Lists.newArrayList(WorkflowRunStatus.NOT_STARTED.name()));
        } else if (WorkflowRunStatus.RUNNING.name().equalsIgnoreCase(status)) {
            statusQueryItem.setOperation(DBOperation.NIN);
            statusQueryItem.setValues(Lists.newArrayList(WorkflowRunStatus.RUNNING.name()
                    , WorkflowRunStatus.FAILED.name(), WorkflowRunStatus.FINISHED.name()));
            updateValues.put("runAt", nowStr);
        } else if (WorkflowRunStatus.FINISHED.name().equalsIgnoreCase(status)) {
            statusQueryItem.setOperation(DBOperation.NIN);
            statusQueryItem.setValues(Lists.newArrayList(WorkflowRunStatus.FAILED.name(), WorkflowRunStatus.FINISHED.name()));
            updateValues.put("finishAt", nowStr);
        } else if (WorkflowRunStatus.FAILED.name().equalsIgnoreCase(status)) {
            statusQueryItem.setOperation(DBOperation.NIN);
            statusQueryItem.setValues(Lists.newArrayList(WorkflowRunStatus.FAILED.name(), WorkflowRunStatus.FINISHED.name()));
            updateValues.put("finishAt", nowStr);
        }
        updateQuery.setAndQuery(Lists.newArrayList(idItem, statusQueryItem));
        updateQuery.setUpdateValues(updateValues);
        workflowRunClient.updateByQuery(updateQuery);
    }


    @Override
    public void runNode(WorkflowRunResult workflowRunResult, BaseConsumer consumer) {
        try {
            if (null == workflowRunResult || null == consumer) {
                return;
            }
            if (WorkflowRunStatus.RUNNING.name().equalsIgnoreCase(workflowRunResult.getStatus())
                    || WorkflowRunStatus.FINISHED.name().equalsIgnoreCase(workflowRunResult.getStatus())
                    || WorkflowRunStatus.FAILED.name().equalsIgnoreCase(workflowRunResult.getStatus())
                    || WorkflowRunStatus.NOT_RUN.name().equals(workflowRunResult.getStatus())
                    // || WorkflowRunStatus.NOT_RUN.name().equalsIgnoreCase(workflowRunResult.getStatus())
            ) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{}[{}]节点运行记录不存在或者已被消费,不处理", workflowRunResult.getNodeQueueName(), workflowRunResult.getId());
                }
                return;
            }
            Long now = DateTime.now().getMillis();
            if (null != workflowRunResult.getParentId() && workflowRunResult.getParentId() > 0L) {
                WorkflowRunResult parent = getWorkflowRunResultStatus(workflowRunResult.getParentId());
                if (null != parent && parent.getWorkflowRunType().equalsIgnoreCase(workflowRunResult.getWorkflowRunType())
                        && (WorkflowRunStatus.FINISHED.name().equalsIgnoreCase(parent.getStatus())
                        || WorkflowRunStatus.FAILED.name().equalsIgnoreCase(parent.getStatus()))) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{}[{}]父节点运行失败或者运行结束,不处理", workflowRunResult.getNodeQueueName(), workflowRunResult.getId());
                    }
                    updateWorkflowRunResult(Lists.newArrayList(workflowRunResult.getId()), now, WorkflowRunStatus.NOT_RUN.name(), StringUtils.EMPTY, Boolean.TRUE);
                    return;
                }
            }
            if (WorkflowRunType.RUN.name().equalsIgnoreCase(workflowRunResult.getWorkflowRunType())) {
                WorkflowRun workflowRun = getWorkflowRunStatus(workflowRunResult.getWorkflowRunId());
                if (null == workflowRun || WorkflowRunStatus.FINISHED.name().equalsIgnoreCase(workflowRun.getStatus())
                        || WorkflowRunStatus.FAILED.name().equalsIgnoreCase(workflowRun.getStatus())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{}[{}]节点运行记录不存在或者运行失败或者运行结束,不处理", workflowRunResult.getNodeQueueName(), workflowRunResult.getId());
                    }
                    updateWorkflowRunResult(Lists.newArrayList(workflowRunResult.getId()), now, WorkflowRunStatus.NOT_RUN.name(), StringUtils.EMPTY, Boolean.TRUE);
                    return;
                }
                workflowRunResult.setUserInfo(workflowRun.getUserInfo());
            }
            consumer.runNode(workflowRunResult, Boolean.TRUE);
        } catch (Throwable e) {
            LOG.error("{} 运行报错 节点={}({}) 工作流运行id={} 工作流id={}"
                    , workflowRunResult.getNodeType(), workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(), e);

        }
    }

    @Override
    public void fillRunResults(String mode, Map<String, WorkflowRunResult> runResults, Integer degree,
                               String source, List<Edge> edges, Map<String, Node> runNodes,
                               JSONObject startParameterInputs, String workflowRunType) throws Exception {

        // 🔥 优化：构建边索引，一次构建，多次使用
        EdgeIndex edgeIndex = new EdgeIndex(edges);
        fillRunResultsWithIndex(mode, runResults, degree, source, edgeIndex, runNodes, startParameterInputs, workflowRunType);
    }


    /**
     * 使用边索引的迭代版本（替代递归）
     * 优化效果：
     * - 边查找：O(V×E) → O(V+E)
     * - 消除递归：避免6000层递归导致的StackOverflow
     */
    private void fillRunResultsWithIndex(String mode, Map<String, WorkflowRunResult> runResults,
                                         Integer initialDegree, String startSource, EdgeIndex edgeIndex,
                                         Map<String, Node> runNodes, JSONObject startParameterInputs, String workflowRunType) throws Exception {

        // 使用队列实现BFS，消除递归
        Queue<NodeProcessContext> queue = new LinkedList<>();
        queue.add(new NodeProcessContext(startSource, initialDegree));

        while (!queue.isEmpty()) {
            NodeProcessContext ctx = queue.poll();
            String source = ctx.getNodeId();
            int degree = ctx.getDegree();

            // 检查是否已处理且度数无需更新
            if (runResults.containsKey(source)) {
                if (runResults.get(source).getDegree() < degree) {
                    runResults.get(source).setDegree(degree);
                }
                // 即使已处理，仍需继续处理下游节点
            } else {
                // 首次处理该节点 - 创建WorkflowRunResult
                WorkflowRunResult result = createWorkflowRunResult(
                        mode, source, degree, edgeIndex, runNodes, startParameterInputs, workflowRunType);
                runResults.put(source, result);
            }

            // 🔥 使用索引快速获取目标节点 O(1)
            List<String> targets = edgeIndex.getTargetNodeIds(source);
            for (String target : targets) {
                // 只处理未访问过或需要更新度数的节点
                if (!runResults.containsKey(target) || runResults.get(target).getDegree() < degree + 1) {
                    queue.add(new NodeProcessContext(target, degree + 1));
                }
            }
        }
    }

    /**
     * 创建单个节点的WorkflowRunResult
     */
    private WorkflowRunResult createWorkflowRunResult(String mode, String source, int degree,
                                                      EdgeIndex edgeIndex, Map<String, Node> runNodes,
                                                      JSONObject startParameterInputs, String workflowRunType) throws Exception {

        Node sourceNode = runNodes.get(source);
        Node node = new Node();
        BeanUtils.copyProperties(sourceNode, node);

        if (null != sourceNode.getData()) {
            NodeData nodeData = new NodeData();
            nodeData.setTemplate(new JSONObject());
            NodeData sourceNodeData = sourceNode.getData();
            if (MapUtils.isNotEmpty(sourceNodeData.getTemplate())) {
                nodeData.getTemplate().putAll(sourceNodeData.getTemplate());
            }
            node.setData(nodeData);
        }

        WorkflowRunResult workflowRunResult = new WorkflowRunResult();
        workflowRunResult.setDegree(degree);
        if (!WorkflowRunMode.COPY.name().equalsIgnoreCase(mode)) {
            workflowRunResult.setMode(mode);
        }
        workflowRunResult.setNodeId(source);

        WorkflowData data = new WorkflowData();
        data.setNodesHandle(Lists.newArrayList(node));

        // 🔥 使用索引获取节点相关边 O(1)
        List<Edge> nodeEdges = edgeIndex.getEdgesByNode(source);
        data.setEdgesHandle(nodeEdges);

        workflowRunResult.setData((JSONObject) JSON.toJSON(data, DataParametersUtil.JSONWRITER_TOJSON_FEATURE));
        workflowRunResult.setNodeDataMd5(WorkflowUtil.getWorkflowDataMd5(data));
        workflowRunResult.setNodeType(node.getType());
        workflowRunResult.setNodeTypeName(node.getTypeName());

        if (StringUtils.isBlank(node.getTypeName())) {
            workflowRunResult.setNodeTypeName(node.getType());
        }
        workflowRunResult.setNodeName(node.getName());
        if (StringUtils.isBlank(workflowRunResult.getNodeName())) {
            workflowRunResult.setNodeName(workflowRunResult.getNodeTypeName());
        }

        Boolean ignore_error = DataParametersUtil.getValueByTemplate("ignore_error", node.getData().getTemplate(), Boolean.class, Boolean.FALSE);
        workflowRunResult.setIgnoreError(ignore_error ? 1 : 0);
        workflowRunResult.setNodeQueueName(node.getQueueName());

        JSONObject inputs = WorkflowUtil.getInputsByTemplate(node.getData().getTemplate());
        inputs.putAll(startParameterInputs);
        workflowRunResult.setInputs(inputs);

        // 🔥 使用索引判断位置 O(1)
        List<Edge> sourceEdges = edgeIndex.getEdgesBySource(source);
        List<Edge> targetEdges = edgeIndex.getEdgesByTarget(source);

        if (WorkflowNodeType.sub_workflow_list.name().equals(workflowRunResult.getNodeType())) {
            workflowRunResult.setPosition(Position.BRANCH.name());
            if (!WorkflowRunType.DEBUG.name().equalsIgnoreCase(workflowRunType) && CollectionUtils.isEmpty(targetEdges)) {
                throw new WorkflowException(String.format("\"%s\"节点无法运行,无上游节点连线", workflowRunResult.getNodeName()));
            }
        } else if (CollectionUtils.isNotEmpty(sourceEdges) && CollectionUtils.isNotEmpty(targetEdges)) {
            workflowRunResult.setPosition(Position.CHILD.name());
        } else if (CollectionUtils.isEmpty(targetEdges)) {
            workflowRunResult.setPosition(Position.ROOT.name());
        } else {
            workflowRunResult.setPosition(Position.LEAF.name());
        }

        if (WorkflowNodeType.sub_workflow.name().equals(workflowRunResult.getNodeType()) || WorkflowNodeType.sub_workflow_list.name().equals(workflowRunResult.getNodeType())) {
            JSONObject template = node.getData().getTemplate();
            JSONObject subWorkflowParameter = DataParametersUtil.getParameterByTemplate(WorkflowJSONKey.subWorkflow.name(), template);
            Object workflowObj = DataParametersUtil.getValueByParameter(subWorkflowParameter, null);
            if (null == workflowObj) {
                throw new WorkflowException(String.format("\"%s\"节点无法运行,没有选择工作流", workflowRunResult.getNodeName()));
            }
            Workflow workflow = null;
            if (workflowObj instanceof Workflow) {
                workflow = (Workflow) workflowObj;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("工作流调用组件的值为Workflow对象 workflowId={} nodeId={}", workflowRunResult.getWorkflowId(), workflowRunResult.getNodeId());
                }
            } else if (workflowObj instanceof Map) {
                JSONObject subWorkflow = DataParametersUtil.getValueByTemplate(WorkflowJSONKey.subWorkflow.name(), template, JSONObject.class, null);
                Long id = subWorkflow.getLong("id");
                if (null == id || id <= 0L) {
                    throw new WorkflowException(String.format("\"%s\"节点无法运行,没有选择工作流", workflowRunResult.getNodeName()));
                }
                workflow = workflowClient.getById(id).getData();
                if (null == workflow) {
                    throw new WorkflowException(String.format("\"%s\"节点无法运行,调用的工作流\"%s\"不存在", workflowRunResult.getNodeName(), subWorkflow.getString(WorkflowJSONKey.name.name())));
                }
            }
            if (null == workflow) {
                throw new WorkflowException(String.format("\"%s\"节点无法运行,调用的工作流不存在", workflowRunResult.getNodeName()));
            }
            if (workflow.getId().equals(workflowRunResult.getWorkflowId())) {
                throw new WorkflowException(String.format("\"%s\"节点无法运行,调用的工作流会导致运行是循环", workflowRunResult.getNodeName()));
            }
            if(null != workflow.getReleaseVersionId() && workflow.getReleaseVersionId() >0L){
                WorkflowVersion subWorkflowVersion = workflowVersionClient.getById(workflow.getReleaseVersionId()).getData();
                if(null == subWorkflowVersion){
                    throw new WorkflowException(String.format("\"%s\"节点无法运行,调用的工作流\"%s\"发布的线上版本不存在", workflowRunResult.getNodeName(), workflow.getName()));
                }
                WorkflowUtil.copyVersion2Workflow(subWorkflowVersion,workflow);
            }
            workflowRunResult.setSubWorkflowId(workflow.getId());
            workflowRunResult.setSubWorkflowDataMd5(workflow.getWorkflowDataMd5());
            workflowRunResult.setSubWorkflowOutputType(workflow.getOutputType());
            JSONObject subWorkflowInputs = MoreObjects.firstNonNull(workflow.getInputs(), new JSONObject());
            List<JSONObject> parameters_mapping = DataParametersUtil.getValuesByTemplate(WorkflowJSONKey.parameters_mapping.name(), template, JSONObject.class, Lists.newArrayList());
            if ((WorkflowNodeType.sub_workflow_list.name().equals(workflowRunResult.getNodeType()) || MapUtils.isNotEmpty(subWorkflowInputs)) && CollectionUtils.isEmpty(parameters_mapping)) {
                throw new WorkflowException(String.format("\"%s\"节点无法运行,替换变量为空", workflowRunResult.getNodeName()));
            }
        }
        return workflowRunResult;
    }


    @Override
    public void pushWorkFlowRunResult(List<WorkflowRunResult> workflowRunResults, Boolean force) throws Exception {
        if (CollectionUtils.isEmpty(workflowRunResults)) {
            return;
        }
        workflowRunResults = workflowRunResults.stream().filter(w -> StringUtils.isNotBlank(w.getNodeQueueName())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(workflowRunResults)) {
            return;
        }
        // 过滤出 if_else 类型的组件，并且其 edgesHandle 中包含至少两个 targetHandle
        List<WorkflowRunResult> filterIfElses = workflowRunResults.stream()
                .filter(w -> "if_else".equalsIgnoreCase(w.getNodeType())) // 筛选 if_else 类型的组件
                .filter(w -> {
                    JSONArray edgesHandle = w.getData().getJSONArray("edgesHandle");
                    List<String> targets = edgesHandle.stream()
                            .filter(e -> e instanceof JSONObject) // 确保元素是 JSONObject
                            .map(e -> (JSONObject) e)
                            .filter(obj -> obj.containsKey("target") && w.getNodeId().equals(obj.getString("target"))) // 确保 target 存在且匹配 nodeId
                            .map(obj -> obj.getString("targetHandle")) // 提取 targetHandle
                            .toList();
                    // 判断 targetHandles 是否至少有两个且至少有一个以 "ifelse" 开头
                    return targets.size() >= 2 && targets.stream().anyMatch(t -> t.startsWith("ifelse"));
                })
                .collect(Collectors.toList());
        // 并 source 节点 有没有运行完成的状态的才过滤
        LOG.info("至少两个 targetHandle 且至少有一个以 ifelse 开头 , filterIfElses={}", filterIfElses);
        filterIfElses = filterIfElses.stream().filter(workflowRunResult -> {
            List<Edge> edges = workflowRunResult.getData().getList(WorkflowJSONKey.edgesHandle.name(), Edge.class);
            List<Edge> sources = edges.stream().filter(e -> workflowRunResult.getNodeId().equals(e.getTarget()) && StringUtils.isNotBlank(e.getSource())).toList();
            List<String> sourceNodeIds = sources.stream().map(Edge::getSource).distinct().collect(Collectors.toList());
            Map<String, WorkflowRunResult> sourceRunResult = null;
            try {
                sourceRunResult = getWorkflowRunResult(workflowRunResult.getWorkflowRunId(), workflowRunResult.getParentId(), sourceNodeIds
                        , Lists.newArrayList(WorkflowRunStatus.FINISHED.name(), WorkflowRunStatus.FAILED.name(), WorkflowRunStatus.NOT_RUN.name()), null, workflowRunResult.getWorkflowRunType()).stream().collect(Collectors.toMap(WorkflowRunResult::getNodeId, Function.identity(), (k1, k2) -> k1));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (MapUtils.isEmpty(sourceRunResult) || sourceRunResult.size() < sourceNodeIds.size()
                    || WorkflowRunStatus.FINISHED.name().equals(workflowRunResult.getStatus())
                    || WorkflowRunStatus.FAILED.name().equals(workflowRunResult.getStatus())
                    || WorkflowRunStatus.NOT_RUN.name().equals(workflowRunResult.getStatus())) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        LOG.info("多个 source 节点中有没有运行完成, filterIfElses={}", filterIfElses);
        workflowRunResults.removeAll(filterIfElses);

        Map<String, List<WorkflowRunResult>> queueData = workflowRunResults.stream().collect(Collectors.groupingBy(WorkflowRunResult::getNodeQueueName));
        for (Map.Entry<String, List<WorkflowRunResult>> queueEntry : queueData.entrySet()) {
            List<String> workflowResultIds = queueEntry.getValue().stream().map(q -> String.valueOf(q.getId())).distinct().collect(Collectors.toList());
            if (WorkflowRunMode.REALTIME.name().equalsIgnoreCase(queueEntry.getValue().getFirst().getMode())) {
                BaseConsumer consumer = consumerRepository.getConsumer(queueEntry.getValue().getFirst().getNodeType());
                for (WorkflowRunResult workflowRunResult : queueEntry.getValue()) {
                    runNode(workflowRunResult, consumer);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{}[{}]运行结束,工作流{}({})", workflowRunResult.getNodeQueueName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId());
                    }
                }
            } else if (ConsumerMode.TABLE.name().equals(consumerMode)) {
                updateWorkflowRunStatusByQueque(queueEntry.getValue(), force);
            } else if (ConsumerMode.ZSET.name().equalsIgnoreCase(consumerMode)) {
                List<SysMqPriorityVo> priorityUsers = null;
                try {
                    JSONObject response = userClient.getMqPriority();
                    priorityUsers  = AuthUtil.getSysMqPriorityVo(response);
                }catch (Exception e){
                    LOG.error("调用优先客户接口报错",e);
                }
                priorityUsers = MoreObjects.firstNonNull(priorityUsers,Lists.newArrayList());
                List<ZSetInfo> zSetInfos = Lists.newArrayList();
                for (WorkflowRunResult workflowRunResult : queueEntry.getValue()) {
                    ZSetInfo zSetInfo = new ZSetInfo();
                    zSetInfo.setValue(String.valueOf(workflowRunResult.getId()));
                    Optional<SysMqPriorityVo> priorityUserOptional = priorityUsers.stream().filter(p->workflowRunResult.getChannel().equals(p.getTenantId()) && null != p.getUserId() && workflowRunResult.getUserId().equals(String.valueOf(p.getUserId()))).findFirst();
                    if(priorityUserOptional.isPresent() && NumberUtils.isDigits(priorityUserOptional.get().getMqPriority()) && Integer.valueOf(priorityUserOptional.get().getMqPriority())>0){
                        zSetInfo.setScore(Double.valueOf(priorityUserOptional.get().getMqPriority()));
                    }else {
                        zSetInfo.setScore(Double.valueOf(workflowRunResult.getId()));
                    }
                    zSetInfos.add(zSetInfo);
                }
                if(LOG.isInfoEnabled()){
                    LOG.info("工作流运行记录={} 推送={} {}",workflowRunResults.getFirst().getWorkflowRunId(),consumerMode,new String(JSON.toJSONBytes(zSetInfos)));
                }
                redisZsetClient.save(queueEntry.getKey(), zSetInfos);
                updateWorkflowRunStatusByQueque(queueEntry.getValue(), force);
            } else {
                for(String workflowResultId:workflowResultIds){
                    Response<String> response = redisQueueClient.index(queueEntry.getKey(),QueueOperate.INDEX_OF,workflowResultId);
                    if(null == response || !NumberUtils.isDigits(response.getData()) || Long.parseLong(response.getData())<=-1){
                        redisQueueClient.push(queueEntry.getKey(), QueueOperate.LEFT_PUSH, workflowResultId);
                        LOG.info("推送节点={}({}) 工作流运行id={} 工作流id={}",
                                workflowResultId,queueEntry.getKey(),workflowRunResults.getFirst().getWorkflowRunId(),workflowRunResults.getFirst().getWorkflowId());
                    }else {
                        LOG.info("已在队列不推送节点={}({}) 工作流运行id={} 工作流id={} 队列索引={}",
                                workflowResultId,queueEntry.getKey(),workflowRunResults.getFirst().getWorkflowRunId(),workflowRunResults.getFirst().getWorkflowId(),response.getData());
                    }
                }
                updateWorkflowRunStatusByQueque(queueEntry.getValue(), force);
            }

        }

    }

    @Override
    public List<Long> pushTargetNodes(WorkflowRunResult workflowRunResult) throws Exception {
        if (workflowRunResult.getDegree() == 0
                && WorkflowNodeType.start_parameters.name().equalsIgnoreCase(workflowRunResult.getNodeType())) {
            Map<String, String> targetParam = Maps.newHashMap();
            targetParam.put("workflowRunId", workflowRunResult.getWorkflowRunId().toString());
            targetParam.put("degree", "1");
            targetParam.put("workflowRunType", workflowRunResult.getWorkflowRunType());
            List<DBQueryItem> and = Lists.newArrayList();
            DBUtil.fillDBQueryItem(and, targetParam, null, null);
            DBQuery<Long> query = new DBQuery<>();
            query.setCurrentPage(1);
            query.setPageSize(Integer.MAX_VALUE);
            query.setAndQuery(and);
            DBQueryItem parentQuery = new DBQueryItem();
            parentQuery.setField("parentId");
            parentQuery.setOperation(DBOperation.IS_NULL);
            and.add(parentQuery);
            List<WorkflowRunResult> target = workflowRunResultClient.query(query).getData().getList();
            if (LOG.isDebugEnabled()) {
                LOG.debug("start_parameters prepare pushTargetNodes={} sourceId={} targetIds={}", target.stream().map(WorkflowRunResult::getNodeId).collect(Collectors.toList()), workflowRunResult.getId(), target.stream().map(t -> t.getId()).collect(Collectors.toList()));
            }
            pushWorkFlowRunResult(target, Boolean.FALSE);
            return target.stream().map(BaseModel::getId).collect(Collectors.toList());
        }
        if (MapUtils.isEmpty(workflowRunResult.getData()) || CollectionUtils.isEmpty(workflowRunResult.getData().getJSONArray(WorkflowJSONKey.edgesHandle.name()))) {
            return Lists.newArrayList();
        }
        List<Edge> edges = workflowRunResult.getData().getList(WorkflowJSONKey.edgesHandle.name(), Edge.class);
        if (CollectionUtils.isEmpty(edges)) {
            return Lists.newArrayList();
        }
        
        // 处理 if-else 组件的输出结果
        boolean isIfElseComponent = "if_else".equals(workflowRunResult.getNodeType());
        
        // 根据组件类型和分支信息过滤目标节点
        List<String> targetNodeIds = Collections.emptyList();
        if (isIfElseComponent && MapUtils.isNotEmpty(workflowRunResult.getOutputs())) {
            // 获取分支类型和唯一标识
            JSONObject outputs = workflowRunResult.getOutputs();
            String branchType = null;
            String branchId = "-1";
            JSONObject ifElseOutputs = null;
            if (outputs.containsKey("output")) {
                ifElseOutputs = outputs.getJSONObject("output").getJSONObject("value");
            }

            if (ifElseOutputs != null) {
                if (ifElseOutputs.containsKey("branchType")) {
                    branchType = ifElseOutputs.getString("branchType");
                }
                if (ifElseOutputs.containsKey("branchId")) {
                    branchId = ifElseOutputs.getString("branchId");
                }

                LOG.info("if-else component output: branchType={}, branchId={}", branchType, branchId);

                // 如果有有效的分支信息
                if (StringUtils.isNotBlank(branchType)) {
                    // 先获取所有可能的目标节点
                    List<Edge> sourceEdges = edges.stream()
                            .filter(e -> workflowRunResult.getNodeId().equals(e.getSource()))
                            .toList();

                    // 根据条件类型和唯一标识匹配边节点
                    List<Edge> matchedEdges = new ArrayList<>();
                    // 需要设置为 NOT_RUN 状态的边
                    List<Edge> notRunEdges = new ArrayList<>();

                    // 遍历所有边，查找匹配的边
                    for (Edge edge : sourceEdges) {
                        // 检查边的属性 - 使用自定义属性来存储分支类型和ID
                        // 分支类型和ID信息存储在边的自定义属性中
                        // Condition condition = edge.getCondition();
                        // if (condition == null) {
                        //     continue;
                        // }
                        // // 从边的属性中获取分支类型和ID
                        // String edgeBranchType = condition.getBranchType();
                        // String edgeBranchId = condition.getBranchId();
                        //
                        // // 如果边的条件类型与当前满足的条件类型匹配，则选择该边
                        // if (StringUtils.isNotBlank(branchType) && branchType.equals(edgeBranchType)) {
                        //     // 进一步匹配唯一标识
                        //     if (StringUtils.isNotBlank(edgeBranchId)) {
                        //         // 唯一标识也匹配，则选择该边
                        //         if (branchId.equals(edgeBranchId)) {
                        //             matchedEdges.add(edge);
                        //         }
                        //     } else {
                        //         notRunEdges.add(edge);
                        //     }
                        // } else {
                        //     notRunEdges.add(edge);
                        // }
                        if (branchId.equals(edge.getSourceHandle())) {
                            matchedEdges.add(edge);
                        } else {
                            notRunEdges.add(edge);
                        }
                    }
                    LOG.info("matchedEdges {}", matchedEdges);
                    LOG.info("notRunEdges {}", notRunEdges);

                    if (CollectionUtils.isNotEmpty(notRunEdges)) {
                        List<String> notRunTargetNodeIds = notRunEdges.stream()
                                .map(Edge::getTarget)
                                .filter(StringUtils::isNotBlank)
                                .distinct()
                                .toList();
                        Map<String, String> params = Maps.newHashMap();
                        params.put("nodeId", String.join(",", notRunTargetNodeIds));
                        params.put("workflowRunId", String.valueOf(workflowRunResult.getWorkflowRunId()));
                        if (workflowRunResult.getParentId() != null) {
                            params.put("parentId", String.valueOf(workflowRunResult.getParentId()));
                        }
                        params.put("includeFields", "id,workflowRunId,nodeId,status,parentId,workflowRunType,nodeType,nodeQueueName,ignoreError,parentId");
                        // params.put("status", WorkflowRunStatus.NOT_STARTED.name());
                        List<WorkflowRunResult> notRunTargets = workflowRunResultClient.find(1, Integer.MAX_VALUE, params).getData().getList();
                        LOG.info("处理 workflowRunResult, params={} notRunTargets={}", params, notRunTargets);
                        if (CollectionUtils.isNotEmpty(notRunTargets)) {
                            // debug 复现并发问题
                            // sleep(10000);
                            List<Long> workflowRunResultIds = notRunTargets.stream().map(WorkflowRunResult::getId).collect(Collectors.toList());
                            updateWorkflowRunResult(workflowRunResultIds,  DateTime.now().getMillis(), WorkflowRunStatus.NOT_RUN.name(), StringUtils.EMPTY, true);
                            LOG.info("处理 workflowRunResult, params={} ids={}", params, workflowRunResultIds);
                            List<WorkflowRunResult> list = workflowRunResultClient.find(workflowRunResultIds).getData().getList();
                            LOG.info("处理 workflowRunResult, workflowRunResultIds={} list={}", workflowRunResultIds, list);
                            // 则将其及子节点状态设置为 NOT_RUN
                            processNotRunWorkflowRunResult(notRunTargets);
                        }
                    }

                    // 如果找到了匹配的边，则使用这些边的目标节点
                    if (CollectionUtils.isNotEmpty(matchedEdges)) {
                        targetNodeIds = matchedEdges.stream()
                                .map(Edge::getTarget)
                                .filter(StringUtils::isNotBlank)
                                .distinct()
                                .collect(Collectors.toList());

                        LOG.info("if-else component matched edges by branch type and id, targetNodeIds: {}", targetNodeIds);
                    }
                } else {
                    // 如果没有有效的分支信息，则不选择任何目标节点
                    LOG.info("if-else component has no valid branch, no target nodes selected");
                }
            }

        } else {
            // 对于其他组件，保持原有逻辑
            targetNodeIds = edges.stream()
                .filter(e -> workflowRunResult.getNodeId().equals(e.getSource()))
                .map(e -> e.getTarget())
                .filter(s -> StringUtils.isNotBlank(s))
                .distinct()
                .collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(targetNodeIds)) {
            return Lists.newArrayList();
        }
        List<WorkflowRunResult> target = getWorkflowRunResult(workflowRunResult.getWorkflowRunId(), workflowRunResult.getParentId(), targetNodeIds, null, null, workflowRunResult.getWorkflowRunType());
        if (LOG.isDebugEnabled()) {
            LOG.debug("prepare pushTargetNodes={} sourceId={} targetIds={}", targetNodeIds, workflowRunResult.getId(), target.stream().map(t -> t.getId()).collect(Collectors.toList()));
        }
        if (CollectionUtils.isEmpty(target)) {
            return Lists.newArrayList();
        }
        pushWorkFlowRunResult(target, Boolean.FALSE);
        return target.stream().map(BaseModel::getId).collect(Collectors.toList());
    }

    private List<Long> processNotRunWorkflowRunResult(List<WorkflowRunResult> notRunTargets) throws Exception {
        List<Long> result = Lists.newArrayList();
        for (WorkflowRunResult workflowRunResult : notRunTargets) {
            LOG.info("processNotRunWorkflowRunResult workflowRunResultId={}", workflowRunResult.getId());
            try {
                WorkflowRunResult data = workflowRunResultClient.getById(workflowRunResult.getId()).getData();
                if (data == null || data.getData() == null) {
                    continue;
                }

                JSONArray edgesHandle = data.getData().getJSONArray("edgesHandle");
                if (edgesHandle == null || edgesHandle.isEmpty()) {
                    continue;
                }

                List<String> targets = edgesHandle.stream()
                        .filter(e -> {
                            if (!(e instanceof JSONObject)) return false;
                            JSONObject obj = (JSONObject) e;
                            return obj.containsKey("source") && data.getNodeId().equals(obj.getString("source"));
                        })
                        .filter(e -> {
                            if (!(e instanceof JSONObject)) return false;
                            JSONObject obj = (JSONObject) e;
                            String nodeType = obj.getString("nodeType");
                            return !"doc_output".equals(nodeType) && !"json_output".equals(nodeType);
                        })
                        .map(e -> (JSONObject) e)
                        .map(obj -> obj.getString("target"))
                        .collect(Collectors.toList());

                LOG.info("if-else component not run target nodeIds: {}", targets);
                if (CollectionUtils.isEmpty(targets)) {
                    continue;
                }

                Map<String, String> params = Maps.newHashMap();
                params.put("nodeId", String.join(",", targets));
                params.put("workflowRunId", String.valueOf(data.getWorkflowRunId()));
                if (workflowRunResult.getParentId() != null) {
                    params.put("parentId", String.valueOf(workflowRunResult.getParentId()));
                }
                params.put("includeFields", "id,workflowRunId,nodeId,status,parentId,workflowRunType,nodeType,nodeQueueName,ignoreError,parentId");
                // params.put("status", WorkflowRunStatus.NOT_STARTED.name());

                // LOG.info("workflowRunResultClient find params: {}", params);

                Response<PageResponse<WorkflowRunResult>> response = workflowRunResultClient.find(1, Integer.MAX_VALUE, params);
                List<WorkflowRunResult> notRunTargetsBySource = Optional.ofNullable(response)
                        .map(Response::getData)
                        .map(PageResponse::getList)
                        .orElse(Collections.emptyList());

                notRunTargetsBySource = notRunTargetsBySource.stream().filter(e -> !"doc_output".equals(e.getNodeType()) && !"json_output".equals(e.getNodeType()))
                        .collect(Collectors.toList());

                LOG.info("处理 workflowRunResult, params={} sourceId={}, notRunTargetsBySource={}", params, workflowRunResult.getId(), notRunTargetsBySource);

                if (CollectionUtils.isNotEmpty(notRunTargetsBySource)) {
                    List<Long> workflowRunResultIds = notRunTargetsBySource.stream()
                            .map(WorkflowRunResult::getId)
                            .collect(Collectors.toList());
                    updateWorkflowRunResult(workflowRunResultIds,  DateTime.now().getMillis(), WorkflowRunStatus.NOT_RUN.name(), StringUtils.EMPTY, true);
                    result.addAll(workflowRunResultIds);
                    LOG.info("处理 workflowRunResult, params={} sourceId={}, workflowRunResultIds={}", params, workflowRunResult.getId(), workflowRunResultIds);
                    // 递归调用并将结果加入最终结果
                    result.addAll(processNotRunWorkflowRunResult(notRunTargetsBySource));
                }
            } catch (Exception e) {
                // 记录日志并继续处理其他项
                LOG.error("处理 workflowRunResult 出错: {}", workflowRunResult, e);
            }
        }
        return result;
    }

    @Override
    public List<WorkflowRunResult> getWorkflowRunResult(Long workflowRunId, Long
            parentId, List<String> nodeIds, List<String> status, List<String> position, String workflowRunType) throws
            Exception {
        Map<String, String> params = Maps.newHashMap();
        params.put("workflowRunId", String.valueOf(workflowRunId));
        params.put("workflowRunType", workflowRunType);
        if (null != parentId) {
            params.put("parentId", String.valueOf(parentId));
        }
        if (CollectionUtils.isNotEmpty(nodeIds)) {
            params.put("nodeId", String.join(",", nodeIds));
        }
        if (CollectionUtils.isNotEmpty(status)) {
            params.put("status", String.join(",", status));
        }
        if (CollectionUtils.isNotEmpty(position)) {
            params.put("position", String.join(",", position));
        }
        List<DBQueryItem> and = Lists.newArrayList();
        DBUtil.fillDBQueryItem(and, params, null, null);
        DBQuery<Long> query = new DBQuery<>();
        query.setCurrentPage(1);
        query.setPageSize(Integer.MAX_VALUE);
        query.setAndQuery(and);
        if (null == parentId) {
            DBQueryItem parentQuery = new DBQueryItem();
            parentQuery.setField("parentId");
            parentQuery.setOperation(DBOperation.IS_NULL);
            and.add(parentQuery);
        }
        return workflowRunResultClient.query(query).getData().getList();
    }

    @Override
    public Integer getWorkflowRunNodeCount(Long workflowRunId, String status, String workflowRunType) throws
            Exception {
        if (null == workflowRunId || workflowRunId <= 0L) {
            return 0;
        }
        Map<String, String> errorParam = Maps.newHashMap();
        errorParam.put("workflowRunId", String.valueOf(workflowRunId));
        if (StringUtils.isNotBlank(status)) {
            errorParam.put("status", status);
        }
        errorParam.put("workflowRunType", workflowRunType);
        Long errorCount = workflowRunResultClient.find(1, 0, errorParam).getData().getTotalCount();
        return null != errorCount ? errorCount.intValue() : 0;
    }

    /**
     * 批量查询父节点ID（优化：减少数据库调用次数）
     * @param nodeIds 子节点ID列表
     * @param workflowRunId 工作流运行ID
     * @param workflowRunType 工作流运行类型
     * @return 所有父节点ID集合
     */
    private Set<Long> batchGetParentIds(List<Long> nodeIds, Long workflowRunId, String workflowRunType) throws Exception {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return Collections.emptySet();
        }
        
        Set<Long> allParentIds = new HashSet<>();
        
        // 使用 IN 查询批量获取所有父节点
        Map<String, String> params = Maps.newHashMap();
        params.put("id", nodeIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        params.put("workflowRunId", String.valueOf(workflowRunId));
        params.put("workflowRunType", workflowRunType);
        params.put("includeFields", "id,parentId");
        
        List<WorkflowRunResult> nodes = workflowRunResultClient.find(1, Integer.MAX_VALUE, params).getData().getList();
        
        // 收集所有有父节点的节点ID
        List<Long> nodesWithParent = nodes.stream()
            .filter(n -> n.getParentId() != null && n.getParentId() > 0L)
            .map(BaseModel::getId)
            .collect(Collectors.toList());
        
        if (CollectionUtils.isEmpty(nodesWithParent)) {
            return allParentIds;
        }
        
        // 批量查询所有父节点（一次调用）
        for (Long nodeId : nodesWithParent) {
            List<WorkflowRunResult> parents = workflowRunResultClient.getParent(
                nodeId, workflowRunId, null, workflowRunType, Boolean.TRUE
            ).getData();
            
            allParentIds.addAll(parents.stream()
                .map(BaseModel::getId)
                .collect(Collectors.toSet()));
        }
        
        return allParentIds;
    }

    /**
     * 批量查询节点详情（优化：减少数据库调用次数）
     * @param nodeIds 节点ID列表
     * @return 节点ID到节点对象的映射
     */
    private Map<Long, WorkflowRunResult> batchGetNodesByIds(List<Long> nodeIds) throws Exception {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return Collections.emptyMap();
        }
        
        Map<String, String> params = Maps.newHashMap();
        params.put("id", nodeIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        
        List<WorkflowRunResult> nodes = workflowRunResultClient.find(1, Integer.MAX_VALUE, params).getData().getList();
        
        return nodes.stream()
            .collect(Collectors.toMap(BaseModel::getId, Function.identity(), (k1, k2) -> k1));
    }

//    public static void main(String[] args) {
//
//        System.out.println(Math.random()*10*10);
//        System.out.println(Math.random()*10*10);
//        System.out.println(Math.random()*10*10);
//        System.out.println(Math.random()*10*10);
//    }

}
