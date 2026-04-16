package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.db.batch.DBDataBatch;
import com.gemantic.gpt.client.WorkflowClient;
import com.gemantic.gpt.client.WorkflowVersionClient;
import com.gemantic.gpt.constant.*;
import com.gemantic.gpt.model.*;
import com.gemantic.gpt.support.workflow.ReRunResult;
import com.gemantic.gpt.support.workflow.WorkflowData;
import com.gemantic.gpt.support.workflow.WorkflowException;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.MsgUtil;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 循环调用工作流
 */
@Component("subWorkflowList")
public class SubWorkflowList extends SubWorkflowBase {

    @Value("${subWorkflowList.batchSize:100}")
    private Integer batchSize;

    @Value("${subWorkflowList.concurrent:true}")
    private Boolean concurrent;

    @Resource
    @Qualifier("subWorkflowListExecutorService")
    private ExecutorService executorService;

    @Value("${CONSUMER_SUB_WORKFLOW_LIST_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize(){
        return MoreObjects.firstNonNull(consumerBatchSize,super.getConsumerBatchSize());
    }

    @Override
    protected ExecutorService getExecutorService() {
        return executorService != null ? executorService : super.getExecutorService();
    }

    @Override
    protected Response<Void> saveWorkflowResult(WorkflowRunResult workflowRunResult, Workflow workflow, List<JSONObject> parameters_mapping, JSONObject inputs) throws Exception {
        if (CollectionUtils.isEmpty(parameters_mapping)) {
            return Response.error(null,String.format("（%s）组件替换变量为空", workflowRunResult.getNodeName()));
        }
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
                return Response.error(null,"运行记录已不存在");
            }
        }
        run.setType(workflowRunResult.getWorkflowRunType());
        run.setMode(workflowRunResult.getMode());
        List<JSONObject> standard_chart = DataParametersUtil.getValuesByTemplate(WorkflowJSONKey.standard_chart.name(), inputs, JSONObject.class, Lists.newArrayList());
        if (CollectionUtils.isEmpty(standard_chart)) {
            return Response.error(null,String.format("（%s）组件列表数据为空", workflowRunResult.getNodeName()));
        }
        WorkflowData subWorkflowdata = WorkflowUtil.getWorkflowData(workflow.getData());
        List<JSONObject> subWorkflowInputs = WorkflowUtil.getSubWorkflowInputs(inputs, standard_chart, parameters_mapping, workflow.getInputs());
        LOG.warn("{} 节点={}({}) 工作流运行id={} 工作流id={} 输入列表大小={} 输入调用工作流参数大小={} 批量大小={} 并发={}"
                    ,workflowRunResult.getNodeType(), workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(),standard_chart.size(),subWorkflowInputs.size(), batchSize, concurrent);
        
        // 批量创建和保存工作流运行结果
        List<WorkflowRunResult> allWorkflowRunResults = createWorkflowRunResults(subWorkflowInputs, run, workflow, workflowRunResult, subWorkflowdata);
        
        // 批量保存
        if (CollectionUtils.isNotEmpty(allWorkflowRunResults)) {
            workflowRepository.saveWorkflowRunResult(run, allWorkflowRunResults);
        }
        
        return Response.ok();
    }

    /**
     * 批量创建工作流运行结果
     * 支持并发创建以提升性能
     */
    private List<WorkflowRunResult> createWorkflowRunResults(List<JSONObject> subWorkflowInputs, WorkflowRun run, 
                                                              Workflow workflow, WorkflowRunResult workflowRunResult, 
                                                              WorkflowData subWorkflowdata) throws Exception {
        if (CollectionUtils.isEmpty(subWorkflowInputs)) {
            return Lists.newArrayList();
        }

        int totalSize = subWorkflowInputs.size();
        List<WorkflowRunResult> allResults = Lists.newArrayListWithCapacity(totalSize);

        if (concurrent && executorService != null && totalSize > 10) {
            // 并发创建
            allResults = createWorkflowRunResultsConcurrent(subWorkflowInputs, run, workflow, workflowRunResult, subWorkflowdata);
        } else {
            // 顺序创建
            allResults = createWorkflowRunResultsSequential(subWorkflowInputs, run, workflow, workflowRunResult, subWorkflowdata);
        }

        return allResults;
    }

    /**
     * 并发创建工作流运行结果
     */
    private List<WorkflowRunResult> createWorkflowRunResultsConcurrent(List<JSONObject> subWorkflowInputs, WorkflowRun run,
                                                                        Workflow workflow, WorkflowRunResult workflowRunResult,
                                                                        WorkflowData subWorkflowdata) throws Exception {
        int totalSize = subWorkflowInputs.size();
        List<WorkflowRunResult> allResults = Lists.newArrayListWithCapacity(totalSize);
        List<Future<WorkflowRunResult>> futures = Lists.newArrayList();
        AtomicInteger completedCount = new AtomicInteger(0);

        // 提交任务
        for (int i = 0; i < totalSize; i++) {
            final int index = i + 1;
            final JSONObject inputs = subWorkflowInputs.get(i);
            Future<WorkflowRunResult> future = executorService.submit(() -> {
                try {
                    WorkflowRunResult wr = createSingleWorkflowRunResult(index, inputs, run, workflow, workflowRunResult, subWorkflowdata);
                    int completed = completedCount.incrementAndGet();
                    if (completed % 100 == 0) {
                        LOG.info("循环调用工作流进度: {}/{}", completed, totalSize);
                    }
                    return wr;
                } catch (Exception e) {
                    LOG.error("创建工作流运行结果失败 index={}", index, e);
                    throw new RuntimeException("创建工作流运行结果失败", e);
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成并收集结果
        for (Future<WorkflowRunResult> future : futures) {
            try {
                WorkflowRunResult wr = future.get();
                if (wr != null) {
                    allResults.add(wr);
                }
            } catch (ExecutionException e) {
                LOG.error("获取工作流运行结果失败", e);
                throw new RuntimeException("获取工作流运行结果失败", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.error("创建工作流运行结果被中断", e);
                throw new RuntimeException("创建工作流运行结果被中断", e);
            }
        }

        // 按索引排序，确保顺序正确
        allResults.sort((a, b) -> Integer.compare(a.getSubWorkflowRunIndex(), b.getSubWorkflowRunIndex()));

        LOG.info("并发创建完成，共创建 {} 个工作流运行结果", allResults.size());
        return allResults;
    }

    /**
     * 顺序创建工作流运行结果
     */
    private List<WorkflowRunResult> createWorkflowRunResultsSequential(List<JSONObject> subWorkflowInputs, WorkflowRun run,
                                                                        Workflow workflow, WorkflowRunResult workflowRunResult,
                                                                        WorkflowData subWorkflowdata) throws Exception {
        List<WorkflowRunResult> allResults = Lists.newArrayListWithCapacity(subWorkflowInputs.size());
        
        for (int i = 0; i < subWorkflowInputs.size(); i++) {
            WorkflowRunResult wr = createSingleWorkflowRunResult(i + 1, subWorkflowInputs.get(i), run, workflow, workflowRunResult, subWorkflowdata);
            allResults.add(wr);
            if ((i + 1) % 100 == 0) {
                LOG.info("循环调用工作流进度: {}/{}", i + 1, subWorkflowInputs.size());
            }
        }
        
        return allResults;
    }

    /**
     * 创建单个工作流运行结果
     */
    private WorkflowRunResult createSingleWorkflowRunResult(Integer index, JSONObject inputs, WorkflowRun run,
                                                             Workflow workflow, WorkflowRunResult workflowRunResult,
                                                             WorkflowData subWorkflowdata) throws Exception {
        WorkflowRunResult wr = workflowRepository.getSubWorkflowRunResult(workflowRunResult.getWorkflowRunType(), 
                workflowRunResult.getMode(), workflow, inputs, subWorkflowdata);
        wr.setParentNodeId(workflowRunResult.getNodeId());
        wr.setParentNodeName(workflowRunResult.getNodeName());
        wr.setNodeId(String.join("_", workflowRunResult.getNodeId(), String.valueOf(wr.getSubWorkflowRunIndex())));
        wr.setNodeName(workflowRunResult.getNodeName());
        wr.setNodeType(workflowRunResult.getNodeType());
        wr.setSubWorkflowRunIndex(index);
        wr.setParentId(workflowRunResult.getId());
        wr.setDegree(0);
        wr.setWorkflowRunId(run.getId());
        wr.setSubWorkflowId(workflowRunResult.getSubWorkflowId());
        wr.setSubWorkflowDataMd5(workflow.getWorkflowDataMd5());
        wr.setIgnoreError(workflowRunResult.getIgnoreError());
        return wr;
    }

    @Override
    public TraceSession reRun(WorkflowRunResult workflowRunResult, ReRunResult data) throws Exception {
        TraceSession traceSession = new TraceSession();
        traceSession.setStart_time(DateTime.now().getMillis());
        traceSession.setTags(Lists.newArrayList(WorkflowReRunType.重新生成.name()));
        JSONObject inputs = MoreObjects.firstNonNull(workflowRunResult.getInputs(), new JSONObject());
        traceSession.setInputs(inputs);
        Response<Workflow> workflowResponse = getWorkflow(workflowRunResult);
        if(!MsgUtil.isValidMessage(workflowResponse)){
            traceSession.setEnd_time(DateTime.now().getMillis());
            traceSession.setStatus(TraceSessionStatus.FAILED.getStatus());
            traceSession.setError(workflowResponse.getMessage().getMessage());
            return traceSession;
        }
        Workflow workflow = workflowResponse.getData();
        workflowRepository.deleteChildrenWorkflowRunResult(workflowRunResult.getWorkflowId(),workflowRunResult.getWorkflowRunId(),Lists.newArrayList(workflowRunResult.getId()),workflowRunResult.getWorkflowRunType());
        List<JSONObject> parameters_mapping = DataParametersUtil.getValuesByTemplate(WorkflowJSONKey.parameters_mapping.name(), inputs, JSONObject.class, Lists.newArrayList());
        Response<Void> saveResult = saveWorkflowResult(workflowRunResult, workflow,parameters_mapping, inputs);
        if(!MsgUtil.isValidMessage(saveResult)){
            traceSession.setEnd_time(DateTime.now().getMillis());
            traceSession.setStatus(TraceSessionStatus.FAILED.getStatus());
            traceSession.setError(saveResult.getMessage().getMessage());
            return traceSession;
        }
        String workflowRunType = workflowRunResult.getWorkflowRunType();
        Map<String, String> subRunParam = Maps.newHashMap();
        subRunParam.put("parentId", String.valueOf(workflowRunResult.getId()));
        subRunParam.put("workflowRunType",workflowRunType);

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
                        workflowRunType
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
        traceSession.setEnd_time(DateTime.now().getMillis());
        workflowRunResult.setInputs(inputs);
        workflowRunResult = workflowRunResultClient.getById(workflowRunResult.getId()).getData();
        if(WorkflowRunStatus.FINISHED.name().equalsIgnoreCase(workflowRunResult.getStatus())){
            traceSession.setStatus(TraceSessionStatus.CORRECT_OR_SUCCESS.getStatus());
        }else if(WorkflowRunStatus.FAILED.name().equalsIgnoreCase(workflowRunResult.getStatus())) {
            traceSession.setStatus(TraceSessionStatus.FAILED.getStatus());
        }
        traceSession.setOutputs(workflowRunResult.getOutputs());
        return traceSession;
    }


}
