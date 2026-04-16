package com.gemantic.workflow.controller;


import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.gemantic.db.batch.DBDataBatch;
import com.gemantic.db.support.DBQuery;
import com.gemantic.db.support.DBQueryItem;
import com.gemantic.db.util.DBUtil;
import com.gemantic.gpt.client.WorkflowClient;
import com.gemantic.gpt.client.WorkflowRunClient;
import com.gemantic.gpt.client.WorkflowRunResultClient;
import com.gemantic.gpt.client.WorkflowVersionClient;
import com.gemantic.gpt.constant.*;
import com.gemantic.gpt.model.*;
import com.gemantic.gpt.support.workflow.*;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.UserUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.constant.DBOperation;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.user.UserInfo;
import com.gemantic.springcloud.utils.MsgUtil;
import com.gemantic.springcloud.utils.SpringBeanUtil;
import com.gemantic.utils.JsonUtils;
import com.gemantic.workflow.consumer.BaseConsumer;
import com.gemantic.workflow.repository.WorkflowRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/workflow/run")
@Tag(name = "工作流运行", description = "工作流运行相关接口")
public class WorkflowRunController {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowRunController.class);

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private WorkflowClient workflowClient;

    @Resource
    private WorkflowRunResultClient workflowRunResultClient;

    @Resource
    private WorkflowRunClient workflowRunClient;

    @Resource
    private WorkflowVersionClient workflowVersionClient;


    @Operation(
        summary = "运行",
        description = "运行工作流",
        operationId = "run"
    )
    @PostMapping
    public Response<Long> run(
            @Parameter(description = "运行输入参数", required = true) @RequestBody RunInputs runInputs
    ) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("工作流运行入参={}", runInputs);
        }
        Response<Long> error = Response.error(null);
        try {
//            if (WorkflowRunMode.COPY.name().equalsIgnoreCase(runInputs.getMode())) {
//                runInputs.setMode(WorkflowRunMode.REALTIME.name());
//            }
            return workflowRepository.runWorkflow(runInputs);
        } catch (JSONException e) {
            LOG.error("工作流运行失败,json格式解析错误 runInputs={}", runInputs, e);
            error.getMessage().setMessage("json格式无法解析");
            return error;
        }

    }


    @Operation(
            summary = "内存运行",
            description = "内存运行",
            operationId = "memory"

    )
    @PostMapping("/memory")
    public Response<OpenRunOutput> memoryRun(
            @Parameter(description = "输入参数", required = true) @RequestBody OpenRunInput data
    ) throws Exception {
        Response<OpenRunOutput> error = Response.error(null);
        try {
            Response<Void> userCheck = UserUtil.allowWrite("内存运行工作流",data.getUserInfo());
            if(!MsgUtil.isValidMessage(userCheck)){
                error.getMessage().setMessage("账户缺失");
                return error;
            }
            if(null == data.getWorkflowId() || data.getWorkflowId()<=0L){
                error.getMessage().setMessage("workflowId参数无效");
                return error;
            }
            return workflowRepository.runWorkflowInMemory(data);
        } catch (Exception e) {
            LOG.error("内存运行工作流失败={}",new String(JSON.toJSONBytes(data)), e);
            error.getMessage().setMessage(e.getMessage());
            return error;
        }
    }


    @Operation(
            summary = "节点运行列表",
            description = "节点运行列表",
            operationId = "plan"
    )
    @GetMapping("/plan")
    public Response<WorkflowPlan> plan(
            @Parameter(description = "工作流id")  @RequestParam(required = false) Long workflowId,
            @Parameter(description = "工作流版本id") @RequestParam(required = false) Long versionId
    ) throws Exception {
        Response<WorkflowPlan> error = Response.error(null);
        if((null == workflowId || workflowId<=0L) && (null == versionId || versionId<=0L)){
            error.getMessage().setMessage("workflowId或者versionId必须>0");
            return error;
        }
        WorkflowVersion workflowVersion = null;
        if(null != versionId && versionId > 0L) {
            workflowVersion = workflowVersionClient.getById(versionId).getData();
            if(null == workflowVersion){
                error.getMessage().setMessage("工作流对应版本不存在");
                return error;
            }
            workflowId = workflowVersion.getWorkflowId();
            Map<String,String> param = Maps.newHashMap();
            param.put("id",workflowId.toString());
            param.put("includeFields","id,name");
            List<Workflow> workflows = workflowClient.find(1,1,param).getData().getList();
            if(CollectionUtils.isEmpty(workflows)){
                error.getMessage().setMessage("工作流不存在");
                return error;
            }
            workflowVersion.setWorkflowName(workflows.getFirst().getName());
        }else {
            Workflow workflow = workflowClient.getById(workflowId).getData();
            if(null == workflow){
                error.getMessage().setMessage("工作流不存在");
                return error;
            }
            workflowVersion = new WorkflowVersion();
            WorkflowUtil.copyWorkflow2Version(workflow,workflowVersion);
        }
        return  workflowRepository.getPlan(workflowVersion);
    }

    @Operation(
        summary = "重新通知",
        description = "重新发送工作流运行通知",
        operationId = "notify"
    )
    @PostMapping("/notify")
    public Response<Long> notify(
            @Parameter(description = "运行输入参数", required = true) @RequestBody RunInputs runInputs
    ) throws Exception {
        Response<Long> error = Response.error(null);
        if (null == runInputs.getWorkflowRunId() || runInputs.getWorkflowRunId() <= 0L) {
            error.getMessage().setMessage("workflowRunId必填");
            return error;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("重新通知={}", runInputs);
        }
        try {
            WorkflowRun workflowRun = workflowRunClient.getById(runInputs.getWorkflowRunId()).getData();
            if (null == workflowRun) {
                error.getMessage().setMessage("工作流运行记录不存在");
                return error;
            }
            if (null != runInputs.getNotifyConfig()) {
                workflowRun.setNotifyConfig(runInputs.getNotifyConfig());
            } else if (null == workflowRun.getNotifyConfig()) {
                Workflow workflow = workflowClient.getById(workflowRun.getWorkflowId()).getData();
                if (null == workflow) {
                    error.getMessage().setMessage("工作流运行记录不存在");
                    return error;
                }
                workflowRun.setNotifyConfig(workflow.getNotifyConfig());
            }
            Response<Long> notifyResult = workflowRepository.notify(workflowRun);
            workflowRun.setNotifyLogId(notifyResult.getData());
            workflowRunClient.save(Lists.newArrayList(workflowRun));
            return notifyResult;
        } catch (JSONException e) {
            LOG.error("工作流运行失败,json格式解析错误 runInputs={}", runInputs, e);
            error.getMessage().setMessage("json格式无法解析");
            return error;
        }

    }

    @Operation(
        summary = "节点调试",
        description = "调试工作流节点",
        operationId = "nodeDebug"
    )
    @PostMapping("/node/debug")
    public Response<WorkflowRunResult> nodeDebug(
            @Parameter(description = "工作流组件运行对象", required = true) @RequestBody RunResultInputs runInputs
    ) throws Exception {
        Response<WorkflowRunResult> error = Response.error(null);
        try {
            if (StringUtils.isBlank(runInputs.getNodeType()) || MapUtils.isEmpty(runInputs.getInputs()) || StringUtils.isBlank(runInputs.getNodeId())) {
                error.getMessage().setMessage("nodeType inputs nodeId必填");
                return error;
            }
            if (null == runInputs.getAppId() || runInputs.getAppId() <= 0L) {
                error.getMessage().setMessage("appId无效");
                return error;
            }
            if (null == runInputs.getWorkflowId() || runInputs.getWorkflowId() <= 0L) {
                error.getMessage().setMessage("workflowId无效");
                return error;
            }

            if (null == runInputs.getUserInfo()) {
                error.getMessage().setMessage("userInfo必填");
                return error;
            }
            Response<Void> allowWrite = UserUtil.allowWrite("debug工作流节点", runInputs.getUserInfo());
            if (!MsgUtil.isValidMessage(allowWrite)) {
                error.setMessage(allowWrite.getMessage());
                return error;
            }
            String nodeType = runInputs.getNodeType();
            String className = StrUtil.toCamelCase(nodeType);
            Map<String, BaseConsumer> consumerMap = SpringBeanUtil.getBeansOfType(BaseConsumer.class);
            BaseConsumer consumer = consumerMap.get(className);
            if (null == consumer) {
                error.getMessage().setMessage("无法识别该组件类型");
                return error;
            }
            if (!consumer.available()) {
                error.getMessage().setMessage("该组件已废弃,请更换组件");
                return error;
            }
            Map<String, String> params = Maps.newHashMap();
            params.put("workflowId", runInputs.getWorkflowId().toString());
            params.put("appId", runInputs.getAppId().toString());
            params.put("workflowRunId", "0");
            params.put("workflowRunType", WorkflowRunType.DEBUG.name());
            params.put("nodeId", runInputs.getNodeId());
            params.put("userId", runInputs.getUserInfo().getUserId());
            DBQuery<Long> query = new DBQuery<>();
            query.setAndQuery(Lists.newArrayList());
            DBUtil.fillDBQueryItem(query.getAndQuery(),params,null,null);
            DBQueryItem parentIdItem = new DBQueryItem();
            parentIdItem.setField("parentId");
            parentIdItem.setOperation(DBOperation.IS_NULL);
            query.getAndQuery().add(parentIdItem);
            List<WorkflowRunResult> workflowRunResults = workflowRunResultClient.find(1,1,params).getData().getList();
            WorkflowRunResult workflowRunResult = new WorkflowRunResult();
            if(CollectionUtils.isNotEmpty(workflowRunResults)){
                workflowRunResult.setId(workflowRunResults.getFirst().getId());
            }
            JSONObject inputs = WorkflowUtil.getInputsByTemplate(runInputs.getInputs());
            JSONObject outputs = WorkflowUtil.getOutputsByTemplate(runInputs.getInputs());
            workflowRunResult.setWorkflowRunId(0L);
            workflowRunResult.setOutputs(null);
            workflowRunResult.setInputs(inputs);
            workflowRunResult.setNodeInputsMd5(WorkflowUtil.getInputsMd5(inputs));
            workflowRunResult.setNodeId(runInputs.getNodeId());
            workflowRunResult.setAppId(runInputs.getAppId());
            workflowRunResult.setWorkflowId(runInputs.getWorkflowId());
            workflowRunResult.setNodeType(runInputs.getNodeType());
            workflowRunResult.setWorkflowRunType(WorkflowRunType.DEBUG.name());
            workflowRunResult.setNodeName(runInputs.getNodeName());
            workflowRunResult.setUserInfo(runInputs.getUserInfo());
            workflowRunResult.setNodeTypeName(runInputs.getNodeTypeName());
            workflowRunResult.setUserId(runInputs.getUserInfo().getUserId());
            workflowRunResult.setUserGroupId(runInputs.getUserInfo().getUserGroupId());
            workflowRunResult.setUserName(runInputs.getUserInfo().getUserName());
            workflowRunResult.setPosition(Position.CHILD.name());
            workflowRunResult.setMode(WorkflowRunMode.REALTIME.name());
            workflowRunResult.setChannel(runInputs.getUserInfo().getChannel());
            if (StringUtils.isBlank(workflowRunResult.getNodeName())) {
                workflowRunResult.setNodeName(workflowRunResult.getNodeTypeName());
            }
            if(WorkflowNodeType.sub_workflow_list.name().equalsIgnoreCase(runInputs.getNodeType())) {
                workflowRunResult.setPosition(Position.BRANCH.name());
            }
            workflowRunResult.setData(new JSONObject());
            List<Long> ids = workflowRunResultClient.save(Lists.newArrayList(workflowRunResult)).getData();
            workflowRunResult.setId(ids.getFirst());
            JSONObject runOutput = consumer.runNode(workflowRunResult, Boolean.FALSE);
            if (WorkflowNodeType.sub_workflow.name().equalsIgnoreCase(runInputs.getNodeType()) || WorkflowNodeType.sub_workflow_list.name().equalsIgnoreCase(runInputs.getNodeType())) {
                workflowRunResult = workflowRunResultClient.getById(workflowRunResult.getId()).getData();
            } else {
                outputs = DataParametersUtil.mergeParameters(outputs, runOutput);
                workflowRunResult.setOutputs(outputs);
                workflowRunResultClient.save(Lists.newArrayList(workflowRunResult));
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("node debug workflowRunResultId={} consumer={} status={} error={} inputs={} outputs={}", workflowRunResult.getId(), consumer.getClass().getSimpleName(), workflowRunResult.getStatus(), workflowRunResult.getErrorInfo(), runInputs, workflowRunResult.getOutputs());
            }
            return Response.ok(workflowRunResult);
        } catch (JSONException e) {
            LOG.error("工作流组件调试失败,json格式解析错误 runInputs={}", runInputs, e);
            error.getMessage().setMessage("json格式无法解析");
            return error;
        }
    }


    @Operation(
            summary = "节点重新生成",
            description = "节点重新生成：<pre>重新生成工作流节点：调用工作流节点\n{\n" +
                "  \"workflowRunId\": 1,\n" +
                "  \"workflowRunResultId\": 1\n" +
                "}</pre>",
        operationId = "nodeRerun"
    )
    @PostMapping("/node/rerun")
    public Response<TraceSession> nodeRerun(
            @Parameter(description = "重新运行参数", required = true) @RequestBody ReRunResult data
    ) throws Exception {
        Response<TraceSession> error = Response.error(null);
        if (null == data.getWorkflowRunId() || data.getWorkflowRunId() <= 0L || null == data.getWorkflowRunResultId() || data.getWorkflowRunResultId() <= 0L) {
            error.getMessage().setMessage("workflowRunId,workflowRunResultId必填");
            return error;
        }
        WorkflowRunResult workflowRunResult = workflowRunResultClient.getById(data.getWorkflowRunResultId()).getData();
        if (null == workflowRunResult) {
            error.getMessage().setMessage("节点运行记录不存在");
            return error;
        }
        if (!WorkflowNodeType.sub_workflow.name().equalsIgnoreCase(workflowRunResult.getNodeType()) && !WorkflowNodeType.sub_workflow_list.name().equalsIgnoreCase(workflowRunResult.getNodeType()) && StringUtils.isBlank(data.getPosition())) {
            error.getMessage().setMessage("position必填");
            return error;
        }
        Map<String, String> params = Maps.newHashMap();
        params.put("workflowRunId", String.valueOf(data.getWorkflowRunId()));
        params.put("parentId", String.valueOf(data.getWorkflowRunResultId()));
        if(!WorkflowNodeType.sub_workflow.name().equalsIgnoreCase(workflowRunResult.getNodeType()) && !WorkflowNodeType.sub_workflow_list.name().equalsIgnoreCase(workflowRunResult.getNodeType())) {
            params.put("position", data.getPosition());
        }
        params.put("workflowRunType", WorkflowRunType.RERUN.name());
        List<WorkflowRunResult> workflowRunResults = workflowRunResultClient.find(1, 1, params).getData().getList();
        if (CollectionUtils.isNotEmpty(workflowRunResults)) {
            workflowRunResult = workflowRunResults.getFirst();
            workflowRunResult.setOutputs(null);
        } else {
            workflowRunResult.setParentId(workflowRunResult.getId());
            workflowRunResult.setId(null);
            workflowRunResult.setCreateAt(DateTime.now().getMillis());
            workflowRunResult.setUpdateAt(null);
            if(!WorkflowNodeType.sub_workflow_list.name().equalsIgnoreCase(workflowRunResult.getNodeType()) && !WorkflowNodeType.sub_workflow.name().equalsIgnoreCase(workflowRunResult.getNodeType())){
                workflowRunResult.setPosition(data.getPosition());
            }
        }
        workflowRunResult.setWorkflowRunType(WorkflowRunType.RERUN.name());
        workflowRunResult.setStatus(WorkflowRunStatus.NOT_STARTED.name());
        workflowRunResult.setRunAt(0L);
        workflowRunResult.setOutputs(null);
        workflowRunResult.setFinishAt(0L);
        workflowRunResult.setMode(WorkflowRunMode.REALTIME.name());
        List<WorkflowRunResult> save = Lists.newArrayList(workflowRunResult);
        List<Long> ids = workflowRunResultClient.save(save).getData();
        workflowRunResult = workflowRunResultClient.getById(ids.getFirst()).getData();
//        UserInfo userInfo = new UserInfo();
//        userInfo.setUserGroupId("100");
//        userInfo.setUserId("1");
//        userInfo.setUserName("admin");
//        userInfo.setChannel("rxhui");
//        data.setUserInfo(userInfo);
        workflowRunResult.setUserInfo(data.getUserInfo());
        Response<Void> allowWrite = UserUtil.allowWrite(String.format("\"%s\"重新生成", workflowRunResult.getNodeName()), workflowRunResult);
        if (!MsgUtil.isValidMessage(allowWrite)) {
            error.setMessage(allowWrite.getMessage());
            return error;
        }
        String nodeType = workflowRunResult.getNodeType();
        if(WorkflowNodeType.sub_workflow_list.name().equalsIgnoreCase(nodeType) && Position.CHILD.name().equalsIgnoreCase(workflowRunResult.getPosition())){
            nodeType = WorkflowNodeType.sub_workflow.name();
        }
        String className = StrUtil.toCamelCase(nodeType);
        Map<String, BaseConsumer> consumerMap = SpringBeanUtil.getBeansOfType(BaseConsumer.class);
        BaseConsumer consumer = consumerMap.get(className);
        if (null == consumer) {
            error.getMessage().setMessage(String.format("节点运行器\"%s\"不存在", nodeType));
            return error;
        }
        if (!consumer.available()) {
            error.getMessage().setMessage("该组件已废弃,请更换组件");
            return error;
        }
        TraceSession traceSession = consumer.reRun(workflowRunResult, data);
        if(!WorkflowNodeType.sub_workflow.name().equalsIgnoreCase(workflowRunResult.getNodeType()) && !WorkflowNodeType.sub_workflow_list.name().equalsIgnoreCase(workflowRunResult.getNodeType())) {
            workflowRunResultClient.save(Lists.newArrayList(workflowRunResult));
        }
        if(StringUtils.isBlank(traceSession.getId())){
            traceSession.setId(workflowRunResult.getId().toString());
        }
        traceSession.setReference_workflow_run_id(data.getWorkflowRunId().toString());
        traceSession.setReference_workflow_run_result_id(data.getWorkflowRunResultId().toString());
        traceSession.setReference_workflow_id(workflowRunResult.getWorkflowId().toString());
        traceSession.setApp_id(workflowRunResult.getAppId().toString());
        traceSession.setReference_workflow_node_type(workflowRunResult.getNodeType());
        traceSession.setReference_workflow_node_id(workflowRunResult.getNodeId());
        return Response.ok(traceSession);
    }


    @Operation(
            summary = "节点推送队列",
            description = "节点推送队列",
            operationId = "nodePush"
    )
    @PostMapping("/node/push")
    public Response<Integer> nodePush(
            @Parameter(description = "运行节点id,多个逗号分隔")  @RequestParam(required = false) List<Long> id,
            @Parameter(description = "工作流id")  @RequestParam(required = false) List<Long> workflowId,
            @Parameter(description = "应用id")  @RequestParam(required = false) List<Long> appId,
            @Parameter(description = "工作流运行id")  @RequestParam(required = false) List<Long> workflowRunId,
            @Parameter(description = "运行节点状态:默认-QUEUED")  @RequestParam(required = false,defaultValue = "QUEUED") String status
            , @Parameter(description = "其他参数", hidden = true) @RequestParam(required = false) Map<String, String> params
    ) throws Exception {
        if(CollectionUtils.isEmpty(id) && CollectionUtils.isEmpty(workflowId) && CollectionUtils.isEmpty(appId) && CollectionUtils.isEmpty(workflowRunId)){
            return Response.error(0,"id,workflowId,appId,workflowRunId不能都为空");
        }
        params.remove("id");
        params.put("status", status);
        params.put("workflowRunType", WorkflowRunType.RUN.name());
        params.put("mode", WorkflowRunMode.RUNTIME.name());
        Pair<Integer,Integer> total = DBDataBatch.execute(id,params,null,null,50, batch->{
            workflowRepository.pushWorkFlowRunResult(batch,Boolean.TRUE);
            return Lists.newArrayList();
        },workflowRunResultClient);
        return Response.ok(total.getLeft());
    }

    @Operation(
        summary = "模拟接口",
        description = "获取模拟财务数据",
        operationId = "getFinData"
    )
    @GetMapping("/finData")
    public Response<JSONObject> getFinData() {
        LOG.info("获取模拟财务数据");
        String balanceData = "real_report.json";
        String balanceDataContent = ResourceUtil.readUtf8Str(balanceData);
        // 解析 JSON 字符串
        JSONObject balanceDataObject = JSONObject.parseObject(balanceDataContent);
        return Response.ok(balanceDataObject);
    }

    @Operation(
        summary = "模拟中原三资产负债表接口",
        description = "获取模拟资产负债表数据",
        operationId = "getBalanceFinData"
    )
    @GetMapping("/balanceFinData")
    public Response<JSONObject> getBalanceFinData() {
        String balanceData = "balance_fin_report.json";
        String balanceDataContent = ResourceUtil.readUtf8Str(balanceData);

        JSONObject balanceDataObject = JsonUtils.parseJsonObjectWithArm(balanceDataContent);

        return Response.ok(balanceDataObject);
    }

    @Operation(
        summary = "批量运行工作流",
        description = "批量运行多个工作流",
        operationId = "batch"
    )
    @PostMapping("/batch")
    public Response<List<Response<Long>>> batch(
            @Parameter(description = "运行输入参数列表", required = true) @RequestBody List<RunInputs> runInputs
    ) throws Exception {
        List<Response<Long>> runBatch = Lists.newArrayList();
        for (RunInputs runInput : runInputs) {
            Response<Long> runId = workflowRepository.runWorkflow(runInput);
            runBatch.add(runId);
        }
        return Response.ok(runBatch);
    }
}