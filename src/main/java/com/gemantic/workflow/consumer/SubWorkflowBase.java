package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.gemantic.db.batch.DBDataBatch;
import com.gemantic.gpt.client.WorkflowClient;
import com.gemantic.gpt.client.WorkflowVersionClient;
import com.gemantic.gpt.constant.Position;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.constant.WorkflowNodeType;
import com.gemantic.gpt.constant.WorkflowRunType;
import com.gemantic.gpt.model.Workflow;
import com.gemantic.gpt.model.WorkflowRun;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.model.WorkflowVersion;
import com.gemantic.gpt.support.workflow.WorkflowData;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.MsgUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 调用工作流
 */
public abstract class SubWorkflowBase extends BaseConsumer {

    @Resource
    protected WorkflowClient workflowClient;

    @Resource
    protected WorkflowVersionClient workflowVersionClient;

    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        JSONObject inputs = DataParametersUtil.getTemplateByTemplate(workflowRunResult.getInputs());
        JSONObject subWorkflowParameter = DataParametersUtil.getParameterByTemplate(WorkflowJSONKey.subWorkflow.name(), inputs);
        JSONObject subWorkflow = DataParametersUtil.getValueByParameter(subWorkflowParameter, JSONObject.class, new JSONObject());
        Response<List<JSONObject>> errorResponse = Response.error(null);
        if (MapUtils.isEmpty(subWorkflow)) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件没有选择工作流", workflowRunResult.getNodeName()));
            return errorResponse;
        }
        Long id = subWorkflow.getLong("id");
        if (null == id || id <= 0L) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件没有选择工作流", workflowRunResult.getNodeName()));
            return errorResponse;
        }
        Workflow workflow = workflowClient.getById(id).getData();
        if (null == workflow) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件调用的工作流\"%s\"不存在", workflowRunResult.getNodeName(), subWorkflow.getString(WorkflowJSONKey.name.name())));
            return errorResponse;
        }
        if (workflow.getId().equals(workflowRunResult.getWorkflowId())) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件调用的工作流\"%s\"会导致运行死循环,请选择其他工作流", workflowRunResult.getNodeName(), subWorkflow.getString(WorkflowJSONKey.name.name())));
            return errorResponse;
        }
        if(null != workflow.getReleaseVersionId() && workflow.getReleaseVersionId() >0L){
            WorkflowVersion subWorkflowVersion = workflowVersionClient.getById(workflow.getReleaseVersionId()).getData();
            if(null == subWorkflowVersion){
                errorResponse.getMessage().setMessage(String.format("\"%s\"节点无法运行,调用的工作流\"%s\"发布的线上版本不存在", workflowRunResult.getNodeName(), workflow.getName()));
                return errorResponse;
            }
            WorkflowUtil.copyVersion2Workflow(subWorkflowVersion,workflow);
        }
        workflowRepository.deleteChildrenWorkflowRunResult(workflowRunResult.getWorkflowId(),workflowRunResult.getWorkflowRunId(),Lists.newArrayList(workflowRunResult.getId()),workflowRunResult.getWorkflowRunType());
        List<JSONObject> parameters_mapping = DataParametersUtil.getValuesByTemplate(WorkflowJSONKey.parameters_mapping.name(), inputs, JSONObject.class, Lists.newArrayList());
        Response<Void> saveResult = saveWorkflowResult(workflowRunResult, workflow,parameters_mapping, inputs);
        if(!MsgUtil.isValidMessage(saveResult)){
            errorResponse.setMessage(saveResult.getMessage());
            return errorResponse;
        }
        if(MapUtils.isNotEmpty(workflowRunResult.getData()) && !WorkflowRunType.DEBUG.name().equalsIgnoreCase(workflowRunResult.getWorkflowRunType())) {
            WorkflowData workflowData = WorkflowUtil.getWorkflowData(workflowRunResult.getData());
            if(CollectionUtils.isNotEmpty(workflowData.getNodesHandle()) && null != workflowData.getNodesHandle().getFirst()) {
                JSONObject template = workflowData.getNodesHandle().getFirst().getData().getTemplate();
                JSONObject nodeSubWorkflowParameter = DataParametersUtil.getParameterByTemplate(WorkflowJSONKey.subWorkflow.name(), template);
                JSONObject workflowJson = JSONObject.from(workflow, JSONWriter.Feature.FieldBased, JSONWriter.Feature.LargeObject);
                DataParametersUtil.updateParameterValue(nodeSubWorkflowParameter,null,workflowJson);
                workflowRunResult.setData((JSONObject) JSON.toJSON(workflowData, JSONWriter.Feature.LargeObject, JSONWriter.Feature.FieldBased));
                workflowRunResultClient.save(Lists.newArrayList(workflowRunResult));
            }
            workflowRepository.updateRunNodeCount(workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowRunType());
        }
        if(WorkflowNodeType.sub_workflow.name().equalsIgnoreCase(workflowRunResult.getNodeType())){
            Map<String, String> subParam = Maps.newHashMap();
            subParam.put("parentId", String.valueOf(workflowRunResult.getId()));
            subParam.put("workflowRunType", workflowRunResult.getWorkflowRunType());
            subParam.put("position", String.join(",", Position.ROOT.name(), Position.LEAF.name()));
            List<WorkflowRunResult> firstNodes = workflowRunResultClient.find(1, Integer.MAX_VALUE, subParam).getData().getList();
            if (firstNodes.size() > 1) {
                firstNodes = firstNodes.stream().filter(f -> Position.ROOT.name().equals(f.getPosition())).collect(Collectors.toList());
            }
            workflowRepository.pushWorkFlowRunResult(firstNodes,Boolean.FALSE);
        }else {
            Map<String, String> subRunParam = Maps.newHashMap();
            subRunParam.put("parentId", String.valueOf(workflowRunResult.getId()));
            subRunParam.put("workflowRunType", workflowRunResult.getWorkflowRunType());
            DBDataBatch.execute(null, subRunParam, null, null, 5, batch -> {
                List<WorkflowRunResult> pushNodes = Lists.newArrayList();
                
                // 优化：批量查询所有子工作流的 firstNodes，避免 N+1 问题
                if (CollectionUtils.isNotEmpty(batch)) {
                    List<Long> parentIds = batch.stream()
                        .map(WorkflowRunResult::getId)
                        .collect(Collectors.toList());
                    
                    // 批量查询所有子工作流的 firstNodes（一次数据库调用）
                    Map<Long, List<WorkflowRunResult>> firstNodesMap = batchGetFirstNodesByParentIds(
                        parentIds, 
                        workflowRunResult.getWorkflowRunType()
                    );
                    
                    // 从批量查询结果中获取每个子工作流的 firstNodes
                    for (WorkflowRunResult sub : batch) {
                        List<WorkflowRunResult> firstNodes = firstNodesMap.getOrDefault(sub.getId(), Lists.newArrayList());
                        if (firstNodes.size() > 1) {
                            firstNodes = firstNodes.stream()
                                .filter(f -> Position.ROOT.name().equals(f.getPosition()))
                                .toList();
                        }
                        pushNodes.addAll(firstNodes);
                    }
                }
                
                workflowRepository.pushWorkFlowRunResult(pushNodes, Boolean.FALSE);
                return Lists.newArrayList();
            }, workflowRunResultClient);
        }

        return Response.ok(Lists.newArrayList());
    }


    protected Response<Workflow> getWorkflow(WorkflowRunResult workflowRunResult) throws Exception{
        WorkflowData workflowData = WorkflowUtil.getWorkflowData(workflowRunResult.getData());
        if(CollectionUtils.isNotEmpty(workflowData.getNodesHandle()) && null != workflowData.getNodesHandle().getFirst()) {
            JSONObject template = workflowData.getNodesHandle().getFirst().getData().getTemplate();
            JSONObject nodeSubWorkflowParameter = DataParametersUtil.getParameterByTemplate(WorkflowJSONKey.subWorkflow.name(), template);
            JSONObject workflowJson = DataParametersUtil.getValueByParameter(nodeSubWorkflowParameter,JSONObject.class,null);
            if(MapUtils.isNotEmpty(workflowJson) && MapUtils.isNotEmpty(workflowJson.getJSONObject(WorkflowJSONKey.data.name()))){
                return Response.ok(JSON.to(Workflow.class,workflowJson));
            }
        }
        Workflow workflow = workflowClient.getById(workflowRunResult.getSubWorkflowId()).getData();
        if (null == workflow) {
            return Response.error(null,String.format("（%s）组件调用的工作流ID\"%s\"不存在", workflowRunResult.getNodeName(), workflowRunResult.getSubWorkflowId()));
        }
        WorkflowVersion subWorkflowVersion = null;
        Map<String,String> workflowRunParams = Maps.newHashMap();
        workflowRunParams.put("includeFields","id,versionId");
        workflowRunParams.put("id",workflowRunResult.getWorkflowRunId().toString());
        List<WorkflowRun> workflowRuns = workflowRunClient.find(1,1,workflowRunParams).getData().getList();
        if(CollectionUtils.isEmpty(workflowRuns)){
            return Response.error(null,String.format("工作流运行记录\"%s\"已被删除",workflowRunResult.getWorkflowRunId().toString()));
        }
        if(null != workflowRuns.getFirst().getVersionId() && workflowRuns.getFirst().getVersionId()>0L){
            subWorkflowVersion = workflowVersionClient.getById(workflowRuns.getFirst().getVersionId()).getData();
            if(null == subWorkflowVersion){
                return Response.error(null,String.format("\"%s\"节点无法运行,调用的工作流\"%s\"的版本已被删除", workflowRunResult.getNodeName(), workflow.getName()));
            }
            WorkflowUtil.copyVersion2Workflow(subWorkflowVersion,workflow);
        }
        return Response.ok(workflow);
    }

    protected abstract Response<Void> saveWorkflowResult(WorkflowRunResult workflowRunResult, Workflow workflow,List<JSONObject> parameters_mapping, JSONObject inputs) throws Exception;

    /**
     * 批量查询多个父节点的 firstNodes（优化：减少数据库调用次数）
     * @param parentIds 父节点ID列表
     * @param workflowRunType 工作流运行类型
     * @return 父节点ID到其firstNodes列表的映射
     */
    protected Map<Long, List<WorkflowRunResult>> batchGetFirstNodesByParentIds(List<Long> parentIds, String workflowRunType) throws Exception {
        if (CollectionUtils.isEmpty(parentIds)) {
            return Maps.newHashMap();
        }
        
        Map<Long, List<WorkflowRunResult>> resultMap = Maps.newHashMap();
        
        // 使用 IN 查询批量获取所有 firstNodes
        Map<String, String> params = Maps.newHashMap();
        params.put("parentId", parentIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        params.put("workflowRunType", workflowRunType);
        params.put("position", String.join(",", Position.ROOT.name(), Position.LEAF.name()));
        
        List<WorkflowRunResult> allFirstNodes = workflowRunResultClient.find(1, Integer.MAX_VALUE, params).getData().getList();
        
        // 按 parentId 分组
        Map<Long, List<WorkflowRunResult>> groupedByParent = allFirstNodes.stream()
            .collect(Collectors.groupingBy(WorkflowRunResult::getParentId));
        
        // 确保所有 parentId 都有对应的列表（即使为空）
        for (Long parentId : parentIds) {
            resultMap.put(parentId, groupedByParent.getOrDefault(parentId, Lists.newArrayList()));
        }
        
        return resultMap;
    }

    @Override
    public Boolean successUpdateFinish() {
        return Boolean.FALSE;
    }

}
