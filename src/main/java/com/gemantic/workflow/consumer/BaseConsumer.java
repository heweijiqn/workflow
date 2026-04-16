package com.gemantic.workflow.consumer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gemantic.db.model.BaseModel;
import com.gemantic.dfs.support.DfsInfo;
import com.gemantic.es.utils.EsClientUtil;
import com.gemantic.gpt.client.ArticleClient;
import com.gemantic.gpt.client.ArticleEsClient;
import com.gemantic.gpt.client.WorkflowRunClient;
import com.gemantic.gpt.client.WorkflowRunResultClient;
import com.gemantic.gpt.constant.*;
import com.gemantic.gpt.model.Article;
import com.gemantic.gpt.model.TraceSession;
import com.gemantic.gpt.model.Workflow;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.qdata.FileInfo;
import com.gemantic.gpt.support.workflow.Edge;
import com.gemantic.gpt.support.workflow.Node;
import com.gemantic.gpt.support.workflow.ReRunResult;
import com.gemantic.gpt.support.workflow.WorkflowException;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.redisearch.client.RedisQueueClient;
import com.gemantic.redisearch.client.RedisZsetClient;
import com.gemantic.redisearch.constants.QueueOperate;
import com.gemantic.redisearch.support.ZSetInfo;
import com.gemantic.search.constant.CommonFields;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.MsgUtil;
import com.gemantic.springcloud.utils.RandomUtil;
import com.gemantic.springcloud.utils.ULID;
import com.gemantic.workflow.repository.WorkflowRepository;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;


@NoRepositoryBean
abstract public class BaseConsumer implements Runnable {

    protected Logger LOG = LoggerFactory.getLogger(getClass().getSimpleName());

    @Resource
    protected RedisQueueClient redisQueueClient;

    @Resource
    protected RedisZsetClient redisZsetClient;

    @Resource
    protected WorkflowRunResultClient workflowRunResultClient;

    @Resource
    protected WorkflowRepository workflowRepository;

    @Resource
    protected WorkflowRunClient workflowRunClient;

    @Resource
    protected ArticleClient articleClient;

    @Resource
    protected EsClientUtil esClientUtil;

    @Resource
    protected ArticleEsClient articleEsClient;

    @Resource(name = "consumerExecutorService")
    protected ExecutorService defaultExecutorService;


    @Value("${consumer.intervalMilliseconds:1000}")
    protected Long intervalMilliseconds;

    @Value("${consumer.batchSize:10}")
    protected Integer defaultBatchSize;

    @Value("${consumer.concurrent:true}")
    @Getter
    protected Boolean concurrent;

    @Setter
    @Getter
    protected Date startDate;

    @Value("${consumer.mode:QUEUE}")
    private String consumerMode;

    public Integer getConsumerBatchSize() {
        return defaultBatchSize;
    }

    @Override
    public void run() {
//        LOG.info("开始运行 {} ", getQueueName());
        List<String> dataList = Lists.newArrayList();
        while (true) {
            dataList.clear();
            try {
                // 为了能一直循环而不结束
                if (ConsumerMode.ZSET.name().equalsIgnoreCase(consumerMode)) {
                    // 批量拉取，支持并发处理
                    List<ZSetInfo> zSetInfoList = redisZsetClient.pop(getQueueName(), "ASC", getConsumerBatchSize()).getData();
                    if (CollectionUtils.isEmpty(zSetInfoList)) {
                        return;
                    }
                    dataList = zSetInfoList.stream().map(ZSetInfo::getValue).filter(NumberUtils::isDigits).distinct().collect(Collectors.toList());
                    if (CollectionUtils.isEmpty(dataList)) {
                        return;
                    }
                    try {
                        runNodeIds(dataList);
                    }catch (Exception e){
                        redisZsetClient.save(getQueueName(),zSetInfoList);
                        LOG.error("节点运行错误重新推送队列 节点运行id={} 队列={}", dataList, getQueueName(), e);
                    }

                } else if (ConsumerMode.TABLE.name().equals(consumerMode)) {
//                    Map<String, String> params = Maps.newHashMap();
//                    params.put("status", String.join(",", WorkflowRunStatus.QUEUED.name(), WorkflowRunStatus.RUNNING.name()));
//                    params.put("includeFields", "id");
//                    params.put("updateAt", String.join(StringUtils.EMPTY, "range:", String.valueOf(DateTime.now().plusMinutes(-180).getMillis())));
//                    List<Long> workflowRunIds = workflowRunClient.find(1, Integer.MAX_VALUE, params).getData().getList().stream().map(w -> w.getId()).collect(Collectors.toList());
//                    if (CollectionUtils.isEmpty(workflowRunIds)) {
//                        return;
//                    }
                    Map<String, String> resultPrams = Maps.newHashMap();
//                    resultPrams.put("workflowRunId", workflowRunIds.stream().map(w -> w.toString()).collect(Collectors.joining(",")));
                    resultPrams.put("nodeQueueName", getQueueName());
                    resultPrams.put("status", WorkflowRunStatus.QUEUED.name());
                    resultPrams.put("includeFields", "id,nodeType,parentId");
                    resultPrams.put("updateAt", String.join(StringUtils.EMPTY, "range:", String.valueOf(DateTime.now().plusDays(-1).getMillis())));
                    List<WorkflowRunResult> workflowRunResults = workflowRunResultClient.find(Lists.newArrayList("createAt"), Lists.newArrayList("ASC"), 1, Integer.MAX_VALUE, resultPrams).getData().getList();
                    if (CollectionUtils.isEmpty(workflowRunResults)) {
                        return;
                    }
                    dataList.addAll(workflowRunResults.stream().map(w -> w.getId().toString()).toList());
                    runNodeIds(dataList);
                } else {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("{} 开始出队",getQueueName());
                    }
                    List<String> queueData = redisQueueClient.pop(getQueueName(), QueueOperate.RIGHT_POP, getConsumerBatchSize()).getData();
                    if (CollectionUtils.isEmpty(queueData)) {
                        return;
                    }
                    dataList.addAll(queueData.stream().distinct().toList());
                    try {
                        runNodeIds(dataList);
                    }catch (Exception e){
                        if(CollectionUtils.isNotEmpty(dataList)) {
                            redisQueueClient.push(getQueueName(), QueueOperate.LEFT_PUSH, dataList);
                            LOG.error("节点运行错误重新推送队列 节点运行id={} 队列={}", dataList, getQueueName(), e);
                        }
                    }
                }
            } catch (Throwable e) {
                LOG.error("节点运行错误 节点运行id={} 队列={}", dataList, getQueueName(), e);
            }
        }
    }

    /**
     * 获取执行器服务
     * 子类可以覆盖此方法以使用专用的执行器
     * @return 执行器服务
     */
    protected ExecutorService getExecutorService() {
        return defaultExecutorService;
    }

    public void runNodeIds(List<String> ids) throws Exception {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        // 如果启用并发且执行器可用，则并发执行
        if (getConcurrent() && getExecutorService() != null && ids.size() > 1) {
            runNodeIdsConcurrent(ids);
        } else {
            // 顺序执行
            runNodeIdsSequential(ids);
        }
    }

    /**
     * 并发执行节点
     */
    private void runNodeIdsConcurrent(List<String> ids) throws Exception {
        List<Future<?>> futures = Lists.newArrayList();
        List<String> failedIds = Lists.newArrayList();
        if(LOG.isInfoEnabled()) {
            LOG.info("{} 并发出队节点id={} ",getQueueName(), ids);
        }
        long begin = DateTime.now().getMillis();
        
        // 优化：批量查询所有节点详情，减少数据库调用次数
        List<Long> validNodeIds = ids.stream()
            .filter(NumberUtils::isDigits)
            .map(Long::valueOf)
            .collect(Collectors.toList());
        
        if (CollectionUtils.isEmpty(validNodeIds)) {
            return;
        }

        // 批量查询节点（一次数据库调用）
        Map<Long, WorkflowRunResult> nodeMap = batchGetWorkflowRunResults(validNodeIds);
        if(MapUtils.isEmpty(nodeMap)){
            return;
        }
        for(Map.Entry<Long, WorkflowRunResult> nodeEntry : nodeMap.entrySet()){
            Future<?> future = getExecutorService().submit(() -> {
                try {
                    workflowRepository.runNode(nodeEntry.getValue(), BaseConsumer.this);
                } catch (Exception e) {
                    LOG.error("并发执行节点失败 nodeId={} queue={}", nodeEntry.getKey(), getQueueName(), e);
                    synchronized (failedIds) {
                        failedIds.add(String.valueOf(nodeEntry.getKey()));
                    }
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                LOG.error("节点执行异常", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.error("节点执行被中断", e);
            }
        }

        // 如果有失败的节点，记录日志
        if (CollectionUtils.isNotEmpty(failedIds)) {
            LOG.warn("部分节点执行失败 queue={} failedIds={}", getQueueName(), failedIds);
        }
        if(LOG.isInfoEnabled()) {
            LOG.info("{} 并发完成 耗时={} 节点id={}",getQueueName(),DateTime.now().getMillis()-begin, ids);
        }
    }

    /**
     * 顺序执行节点（原有逻辑）
     */
    private void runNodeIdsSequential(List<String> ids) throws Exception {
        if(LOG.isInfoEnabled()) {
            LOG.info("{} 顺序出队 节点id={} ", getQueueName(), ids);
        }
        long begin = DateTime.now().getMillis();
        
        // 优化：批量查询所有节点详情，减少数据库调用次数
        List<Long> validNodeIds = ids.stream()
            .filter(NumberUtils::isDigits)
            .map(Long::valueOf)
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(validNodeIds)) {
            return;
        }
        // 批量查询节点（一次数据库调用）
        Map<Long, WorkflowRunResult> nodeMap = batchGetWorkflowRunResults(validNodeIds);
        if(MapUtils.isEmpty(nodeMap)){
            return;
        }
        // 按原顺序执行
        for(Map.Entry<Long, WorkflowRunResult> nodeEntry : nodeMap.entrySet()){
            workflowRepository.runNode(nodeEntry.getValue(), this);
        }
        if(LOG.isInfoEnabled()) {
            LOG.info("{} 顺序完成 耗时={} 节点id={}",getQueueName(),DateTime.now().getMillis()-begin, nodeMap.keySet());
        }
    }


    public JSONObject runNode(WorkflowRunResult workflowRunResult, Boolean notDebug) throws Exception {
        if(LOG.isInfoEnabled()) {
            LOG.info("runNode {} 组件运行 WorkflowRunResult={}", workflowRunResult.getNodeType(), workflowRunResult);
        }
        Boolean updateStatus = notDebug || WorkflowNodeType.sub_workflow.name().equalsIgnoreCase(workflowRunResult.getNodeType()) || WorkflowNodeType.sub_workflow_list.name().equalsIgnoreCase(workflowRunResult.getNodeType());
        try {
            JSONObject inputs = getInputs(workflowRunResult, notDebug);
            if (MapUtils.isEmpty(inputs)) {
                if(updateStatus && WorkflowRunType.RUN.name().equalsIgnoreCase(workflowRunResult.getWorkflowRunType()) && WorkflowRunMode.RUNTIME.name().equalsIgnoreCase(workflowRunResult.getMode()) && WorkflowRunStatus.QUEUED.name().equalsIgnoreCase(workflowRunResult.getStatus())){
                    workflowRepository.updateWorkflowRunResult(Lists.newArrayList(workflowRunResult.getId()),DateTime.now().getMillis(),WorkflowRunStatus.QUEUED.name(),null,Boolean.FALSE);
                }
                return new JSONObject();
            }
            String nodeType = workflowRunResult.getNodeType();
            if(LOG.isInfoEnabled()) {
                LOG.info("runNode {} 组件的Inputs为完成状态 节点={}({})  工作流运行id={} 工作流id={} "
                        , nodeType, workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId());
            }
            // 运行前状态检查，是否有连接条件组件，如果是，再次验证条件组件运行状态是否一致(因为数据库的更新状态可能滞后)
            List<Edge> edges = workflowRunResult.getData().getList(WorkflowJSONKey.edgesHandle.name(), Edge.class);
            if (CollectionUtils.isNotEmpty(edges) && !"doc_output".equals(nodeType) && !"json_output".equals(nodeType)) {
                List<Edge> ifelseEdges = edges.stream()
                        // 排除自身为 source 的边，只判断上游节点
                        .filter(e -> StringUtils.isNotBlank(e.getSource()) && !e.getSource().equals(workflowRunResult.getNodeId()))
                        .filter(e -> StringUtils.isNotBlank(e.getTargetHandle()) && e.getTargetHandle().startsWith("ifelse"))
                        .toList();
                if (CollectionUtils.isNotEmpty(ifelseEdges)) {
                    // 找到上游的条件组件
                    List<String> sourceNodeIds = ifelseEdges.stream().map(Edge::getSource).toList();
                    Map<String, String> params = Maps.newHashMap();
                    params.put("nodeId", String.join(",", sourceNodeIds));
                    params.put("workflowId", String.valueOf(workflowRunResult.getWorkflowId()));
                    params.put("workflowRunId", String.valueOf(workflowRunResult.getWorkflowRunId()));
                    if (workflowRunResult.getParentId() != null) {
                        params.put("parentId", String.valueOf(workflowRunResult.getParentId()));
                    }
                    params.put("includeFields", "id,parentId,outputs,data");
                    List<WorkflowRunResult> ifelseRunResults = workflowRunResultClient.find(1, Integer.MAX_VALUE, params).getData().getList();
                    if (CollectionUtils.isNotEmpty(ifelseRunResults)) {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("runNode {} 运行前的条件组件运行状态检查 节点={}({})  工作流运行id={} 工作流id={} 条件组件运行结果={}",
                                    nodeType, workflowRunResult.getNodeName(), workflowRunResult.getId(),
                                    workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(), ifelseRunResults);
                        }
                        List<String> curWorkflowRunResultBindingTargetHandles = ifelseEdges.stream().map(Edge::getTargetHandle).toList();
                        List<List<JSONObject>> edgesHandle = ifelseRunResults.stream().map(WorkflowRunResult::getData).map(r -> r.getList("edgesHandle", JSONObject.class)).toList();
                        List<JSONObject> flatList = edgesHandle.stream().flatMap(Collection::stream).toList();
                        List<String> sourceHandle = flatList.stream().filter(jsonObject -> curWorkflowRunResultBindingTargetHandles.contains(jsonObject.getString("targetHandle")))
                                .map(jsonObject -> jsonObject.getString("sourceHandle")).toList();
                        if (CollectionUtils.isNotEmpty(sourceHandle)) {

                            // 判断是否命中条件组件
                            List<JSONObject> outputs = ifelseRunResults.stream().map(WorkflowRunResult::getOutputs).toList();
                            List<String> ifelseBranchIds = outputs.stream().map(output -> output.getJSONObject("output"))
                                    .map(e -> e.getJSONObject("value"))
                                    .map(e -> e.getString("branchId"))
                                    .toList();
                            if(LOG.isInfoEnabled()) {
                                LOG.info("runNode {} 运行前的条件组件命中检查sourceHandle 节点={}({})  工作流运行id={} 工作流id={} 条件组件={} 为真的条件组件={}",
                                        nodeType, workflowRunResult.getNodeName(), workflowRunResult.getId(),
                                        workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(), sourceHandle, ifelseBranchIds);
                            }
                            List<String> hitTheBranch = sourceHandle.stream().filter(ifelseBranchIds::contains).toList();
                            if(CollectionUtils.isEmpty(hitTheBranch)) {
                                if(LOG.isInfoEnabled()) {
                                    LOG.info("runNode {} 没有命中条件组件，不需要执行 节点={}({})  工作流运行id={} 工作流id={} "
                                            , nodeType, workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId());
                                }
                                return new JSONObject();
                            }
                        }
                    }

                }
            }


            if (workflowRunResult.getStatus().equals(WorkflowRunStatus.NOT_RUN.name())) {
                if(LOG.isInfoEnabled()) {
                    LOG.info("runNode {} 组件运行状态为NOT_RUN 节点={}({})  工作流运行id={} 工作流id={} "
                            , nodeType, workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId());
                }
                return new JSONObject();
            }
            workflowRunResult.setNodeInputsMd5(WorkflowUtil.getInputsMd5(inputs));
            workflowRunResult.setRunAt(DateTime.now().getMillis());
            workflowRunResult.setStatus(WorkflowRunStatus.RUNNING.name());
            workflowRunResult.setInputs(inputs);
            if (updateStatus) {
                if(!successUpdateFinish()){
                    //运行完成不更新节点运行状态的先把输入参数保存
                    workflowRunResultClient.save(Lists.newArrayList(workflowRunResult));
                }
                workflowRepository.updateWorkflowRunStatusByRunning(workflowRunResult);
            }
            String error = null;
            if (null == workflowRunResult.getCopyWorkflowRunId()
                    || workflowRunResult.getCopyWorkflowRunId() <= 0L) {
                long l1 = DateTime.now().getMillis();
                Response<List<JSONObject>> outputResultResponse = getOutputs(workflowRunResult);
                if(successUpdateFinish()){
                    if(LOG.isInfoEnabled()) {
                        LOG.info("{} 组件运行完成 节点={}({})  工作流运行id={} 工作流id={} 耗时(毫秒)={} 运行结果={}"
                                , nodeType, workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(), (DateTime.now().getMillis() - l1), new String(JSON.toJSONBytes(outputResultResponse)));
                    }
                }
                if (MsgUtil.isValidMessage(outputResultResponse)) {
                    if (successUpdateFinish()) {
                        JSONObject outputs = WorkflowUtil.mergeOutputs(workflowRunResult, outputResultResponse.getData());
                        workflowRunResult.setOutputs(outputs);
                    } else {
                        if (!WorkflowRunMode.REALTIME.name().equalsIgnoreCase(workflowRunResult.getMode()) && LOG.isInfoEnabled()) {
                            LOG.info("{}组件{}({})运行后不更新组件运行记录状态", workflowRunResult.getNodeTypeName(), workflowRunResult.getNodeName(), workflowRunResult.getId());
                        }
                    }
                } else {
                    if(null != outputResultResponse && null != outputResultResponse.getMessage()) {
                        error = outputResultResponse.getMessage().getMessage();
                    }else {
                        error = String.format("（%s）组件输出结果为空", workflowRunResult.getNodeName());
                    }
                }
            }
            if(StringUtils.isNotBlank(error)){
                updateWorkflowRunStatusByFailure(workflowRunResult, error, updateStatus);
                return workflowRunResult.getOutputs();
            }else if ((null == workflowRunResult.getCopyWorkflowRunId()
                    || workflowRunResult.getCopyWorkflowRunId() <= 0L) && successUpdateFinish()
                    || null != workflowRunResult.getCopyWorkflowRunId()
                    && workflowRunResult.getCopyWorkflowRunId() > 0L) {
                if (updateStatus) {
                    if (MapUtils.isEmpty(workflowRunResult.getOutputs())) {
                        error = String.format("（%s）组件输出结果为空", workflowRunResult.getNodeName());
                        workflowRepository.updateWorkflowRunStatusByFailure(workflowRunResult, error,DateTime.now().getMillis());
                    } else {
                        workflowRepository.updateWorkflowRunFinish(workflowRunResult,DateTime.now().getMillis());
                    }
                } else {
                    if (MapUtils.isEmpty(workflowRunResult.getOutputs())) {
                        workflowRunResult.setStatus(WorkflowRunStatus.FAILED.name());
                        error = String.format("（%s）组件输出结果为空", workflowRunResult.getNodeName());
                        workflowRunResult.setErrorInfo(error);
                    } else {
                        workflowRunResult.setErrorInfo(StringUtils.EMPTY);
                        workflowRunResult.setStatus(WorkflowRunStatus.FINISHED.name());
                    }
                    workflowRunResult.setFinishAt(DateTime.now().getMillis());
                }
                return workflowRunResult.getOutputs();
            }
        } catch (Throwable e) {
            String errorInfo = e.getMessage();
            LOG.error("节点运行失败 节点={}({}) 工作流运行id={} 工作流id={}"
                    ,workflowRunResult.getNodeName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(), e);
//            if(e instanceof TimeoutException){
//                errorInfo = String.format("（%s）组件运行超时%d分钟", workflowRunResult.getNodeName(),getRunTimeout());
//            }else
            if (!(e instanceof WorkflowException)) {
                errorInfo = String.format("（%s）组件运行出错，请重试", workflowRunResult.getNodeName());
            }
            updateWorkflowRunStatusByFailure(workflowRunResult, errorInfo, updateStatus);
        }
        return null;

    }

    protected void updateWorkflowRunStatusByFailure(WorkflowRunResult workflowRunResult, String errorInfo, Boolean updateStatus) {
        try {
            if (updateStatus) {
                workflowRepository.updateWorkflowRunStatusByFailure(workflowRunResult, errorInfo,DateTime.now().getMillis());
            } else {
                workflowRunResult.setErrorInfo(errorInfo);
                workflowRunResult.setStatus(WorkflowRunStatus.FAILED.name());
                workflowRunResult.setFinishAt(DateTime.now().getMillis());
            }
        } catch (Exception e) {
            LOG.error("updateWorkflowRunStatusByFailure error id={}", workflowRunResult.getId(), e);
        }

    }

    public abstract Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception;


    public void updateInputs4UploadFilesContent(List<String> parameterNames,JSONObject inputs) throws Exception {
        Map<String, List<String>> nameTaskIds = WorkflowUtil.getUploadFileTaskIds(parameterNames,inputs);
        if (MapUtils.isEmpty(nameTaskIds)) {
            return;
        }
        List<String> taskIds = nameTaskIds.values().stream().flatMap(Collection::stream).distinct().collect(Collectors.toList());
        Map<String, String> params = Maps.newHashMap();
        params.put("taskId", String.join(",", taskIds));
        List<Article> articleList = esClientUtil.isEsClient() ? articleEsClient.search(1, CommonFields.PAGE_MAX_SIZE, params).getData().getList(): articleClient.search(1, CommonFields.PAGE_MAX_SIZE, params).getData().getList();
        if(CollectionUtils.isEmpty(articleList)){
            return;
        }
        Map<String,Article> articles = Maps.newHashMap();
        for(Article article : articleList){
            Collection<String> taskId = CollectionUtils.intersection(article.getTaskId(),taskIds);
            for(String tid :  taskId){
                articles.put(tid,article);
            }
        }
        if (MapUtils.isEmpty(articles)) {
            return;
        }
        for (Map.Entry<String, List<String>> nameDocIdsEntry : nameTaskIds.entrySet()) {
            String name = nameDocIdsEntry.getKey();
            JSONObject uploadFile = inputs.getJSONObject(name);
            List<String> contents = Lists.newArrayList();
            for (String taskId : nameDocIdsEntry.getValue()) {
                if (null == articles.get(taskId)) {
                    continue;
                }
                String content = articles.get(taskId).getContent();
                contents.add(content);
            }
            DataParametersUtil.updateParameterValue(uploadFile, WorkflowJSONKey.str.name(), contents);
        }
    }

    public JSONObject getInputs(WorkflowRunResult workflowRunResult, Boolean notDebug) throws Exception {
        if (!notDebug) {
            if (MapUtils.isNotEmpty(workflowRunResult.getInputs())) {
                return workflowRunResult.getInputs();
            } else if (CollectionUtils.isNotEmpty(workflowRunResult.getData().getJSONArray(WorkflowJSONKey.nodesHandle.name()))) {
                return WorkflowUtil.getInputsByTemplate(workflowRunResult.getData().getList(WorkflowJSONKey.nodesHandle.name(), Node.class).getFirst().getData().getTemplate());
            }
            return new JSONObject();
        }
        List<Edge> edges = workflowRunResult.getData().getList(WorkflowJSONKey.edgesHandle.name(), Edge.class);
        if (CollectionUtils.isEmpty(edges)) {
            return workflowRunResult.getInputs();
        }
        List<Edge> sources = edges.stream().filter(e -> workflowRunResult.getNodeId().equals(e.getTarget()) && StringUtils.isNotBlank(e.getSource())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sources)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("执行顶点{}({})为第一个顶点,使用顶点执行记录初始化的输入", workflowRunResult.getId(), workflowRunResult.getWorkflowRunId());
            }
            return workflowRunResult.getInputs();
        }
        List<String> sourceNodeIds = sources.stream().map(Edge::getSource).distinct().collect(Collectors.toList());
        Map<String, WorkflowRunResult> sourceRunResult = workflowRepository.getWorkflowRunResult(workflowRunResult.getWorkflowRunId(), workflowRunResult.getParentId(), sourceNodeIds
                , WorkflowUtil.WORKFLOW_RUN_STATUS_FINISHED, null, workflowRunResult.getWorkflowRunType()).stream().collect(Collectors.toMap(WorkflowRunResult::getNodeId, Function.identity(), (k1, k2) -> k1));
        if (LOG.isInfoEnabled()) {
            LOG.info("执行节点{}({})的输入节点为{}, sourceNodeIds{}", workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), sourceRunResult, sourceNodeIds);
        }
        return getInputs(sources,sourceNodeIds,sourceRunResult,workflowRunResult,null == workflowRunResult.getWorkflowRunId() ? "0" : workflowRunResult.getWorkflowRunId().toString());
    }


    public JSONObject getInputs(List<Edge> sources,List<String> sourceNodeIds,Map<String, WorkflowRunResult> sourceRunFinishedResult,WorkflowRunResult workflowRunResult,String workflowRunId){
        if (MapUtils.isEmpty(sourceRunFinishedResult) || sourceRunFinishedResult.size() < sourceNodeIds.size()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("运行组件={} 工作流运行id={} 需要的输入组件未运行完", workflowRunResult.getNodeName(), workflowRunId);
            }
            return null;
        }
        List<WorkflowRunResult> notIgnoreErrorResult = sourceRunFinishedResult.values().stream().filter(s -> WorkflowRunStatus.FAILED.name().equalsIgnoreCase(s.getStatus()) && (null == s.getIgnoreError() || s.getIgnoreError() <= 0)).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(notIgnoreErrorResult)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("运行组件={} 工作流运行id={} 需要的输入组件运行错误={}", workflowRunResult.getNodeName(), workflowRunId, notIgnoreErrorResult.stream().map(WorkflowRunResult::getNodeName).collect(Collectors.toList()));
            }
            return null;
        }
        JSONObject inputs = MoreObjects.firstNonNull(workflowRunResult.getInputs(), new JSONObject());
        for (Map.Entry<String, WorkflowRunResult> sourceRunResultEntry : sourceRunFinishedResult.entrySet()) {
            String sourceNodeId = sourceRunResultEntry.getKey();
            WorkflowRunResult sourceRun = sourceRunResultEntry.getValue();
            JSONObject sourceOutputs = sourceRun.getOutputs();
            if (MapUtils.isEmpty(sourceOutputs)) {
                continue;
            }
            JSONObject sourceTemplates = sourceRun.getData().getList(WorkflowJSONKey.nodesHandle.name(), Node.class).getFirst().getData().getTemplate();
            JSONObject sourceInputs = sourceRun.getInputs();
            List<Edge> source = sources.stream().filter(s -> sourceNodeId.equalsIgnoreCase(s.getSource())).collect(Collectors.toList());
            for (Edge sourceEdge : source) {
                String sourceHandle = sourceEdge.getSourceHandle();
                if (StringUtils.isBlank(sourceHandle)) {
                    continue;
                }
                String targetHandle = sourceEdge.getTargetHandle();
                if (StringUtils.isBlank(targetHandle)) {
                    targetHandle = sourceHandle;
                }
                JSONObject sourceOutput = null;
                if (sourceOutputs.containsKey(sourceHandle)) {
                    sourceOutput = JSON.copy(sourceOutputs.getJSONObject(sourceHandle), DataParametersUtil.JSONWRITER_FEATURE);
                } else if (sourceInputs.containsKey(sourceHandle)) {
                    sourceOutput = JSON.copy(sourceInputs.getJSONObject(sourceHandle), DataParametersUtil.JSONWRITER_FEATURE);
                } else if (sourceTemplates.containsKey(sourceHandle)) {
                    sourceOutput = JSON.copy(sourceTemplates.getJSONObject(sourceHandle), DataParametersUtil.JSONWRITER_FEATURE);
                }
                if (MapUtils.isEmpty(sourceOutput)) {
                    continue;
                }
                sourceOutput.put(WorkflowJSONKey.name.name(), targetHandle);
                if (inputs.containsKey(targetHandle)) {
                    sourceOutput = DataParametersUtil.mergeParameter(inputs.getJSONObject(targetHandle), sourceOutput);
                }
                sourceOutput.put(WorkflowJSONKey.source_node_id.name(), sourceNodeId);
                sourceOutput.put(WorkflowJSONKey.source_node_type.name(), sourceRun.getNodeType());
                sourceOutput.put(WorkflowJSONKey.source_node_handle.name(), sourceHandle);
                sourceOutput.put(WorkflowJSONKey.source_node_name.name(), sourceRun.getNodeName());
                if (null != sourceRun.getId()) {
                    sourceOutput.put(WorkflowJSONKey.source_workflow_run_result_id.name(), String.valueOf(sourceRun.getId()));
                }
                inputs.put(targetHandle, sourceOutput);
            }
        }
        return inputs;
    }

    public String getQueueName() {
        return String.join("_", "llm_workflow", StrUtil.toUnderlineCase(getClass().getSimpleName()).toLowerCase());
    }

    public Boolean available() {
        return Boolean.TRUE;
    }

    public Long getIntervalMilliseconds() {
        return intervalMilliseconds + RandomUtil.generateInRange(0,850);
    }

    public TraceSession reRun(WorkflowRunResult workflowRunResult, ReRunResult data) throws Exception {
        JSONObject outputs = JSON.copy(MoreObjects.firstNonNull(workflowRunResult.getOutputs(), new JSONObject()), DataParametersUtil.JSONWRITER_FEATURE);
        JSONObject output_rerun_chat = DataParametersUtil.getParameterByTemplate(WorkflowJSONKey.output_rerun_chat.name(), outputs);
        if (MapUtils.isEmpty(output_rerun_chat)) {
            output_rerun_chat.put(WorkflowJSONKey.name.name(), WorkflowJSONKey.output_rerun_chat.name());
            output_rerun_chat.put(WorkflowJSONKey.display_name.name(), "重新生成聊天记录");
            output_rerun_chat.put(WorkflowJSONKey.type.name(), WorkflowJSONKey.chat_history.name());
            output_rerun_chat.put(WorkflowJSONKey.value.name(), Lists.newArrayList());
        }
        List<TraceSession> output_rerun_chat_history = Lists.newArrayList();
        if (!data.getCleanHistory()) {
            output_rerun_chat_history = DataParametersUtil.getValuesByParameter(output_rerun_chat, TraceSession.class, Lists.newArrayList());
        }
        List<JSONObject> chat_history = chatHistory2Json(output_rerun_chat_history, Boolean.FALSE);
        List<JSONObject> fininshReRun = chat_history.stream().filter(c -> TraceSessionStatus.CORRECT_OR_SUCCESS.getStatus().equalsIgnoreCase(c.getString("status")) && c.getJSONArray("tags").contains(WorkflowReRunType.重新生成.name())).collect(Collectors.toList()).stream().collect(Collectors.toList());
        TraceSession traceSession = new TraceSession();
        if (StringUtils.isNotBlank(data.getId())) {
            traceSession.setId(data.getId());
        } else {
            traceSession.setId(new ULID().nextULID());
        }
        traceSession.setInput(data.getInput());
        traceSession.setStart_time(DateTime.now().getMillis());
        WorkflowReRunType workflowReRunType = WorkflowReRunType.重新生成;
        if (CollectionUtils.isNotEmpty(fininshReRun) && StringUtils.isNotBlank(data.getInput()) && !data.getInput().contains(WorkflowReRunType.重新生成.name())) {
            workflowReRunType = WorkflowReRunType.再次生成;
            traceSession.setParent_id(fininshReRun.get(fininshReRun.size() - 1).getString(WorkflowJSONKey.id.name()));
        }
        traceSession.setTags(Lists.newArrayList(workflowReRunType.name()));
        chat_history.add(chatHistory2Json(traceSession, Boolean.FALSE));
        JSONObject inputs = MoreObjects.firstNonNull(workflowRunResult.getInputs(), new JSONObject());
        JSONObject parameter = new JSONObject();
        parameter.put(WorkflowJSONKey.name.name(), WorkflowJSONKey.chat_history.name());
        parameter.put(WorkflowJSONKey.display_name.name(), WorkflowJSONKey.chat_history.name());
        parameter.put(WorkflowJSONKey.type.name(), WorkflowJSONKey.chat_history.name());
        parameter.put(WorkflowJSONKey.value.name(), chat_history);
        inputs.put(WorkflowJSONKey.chat_history.name(), parameter);
        workflowRunResult.setInputs(inputs);
        runNode(workflowRunResult, Boolean.FALSE);
        if (WorkflowRunStatus.FINISHED.name().equalsIgnoreCase(workflowRunResult.getStatus())) {
            JSONObject runOutputs = workflowRunResult.getOutputs();
            traceSession.setOutputs(runOutputs);
            String output = DataParametersUtil.getValueByTemplate(getOutputTextParameterName(), runOutputs, String.class, StringUtils.EMPTY);
            traceSession.setOutput(output);
            traceSession.setStatus(TraceSessionStatus.CORRECT_OR_SUCCESS.getStatus());
        } else {
            traceSession.setStatus(TraceSessionStatus.FAILED.getStatus());
            traceSession.setOutput(workflowRunResult.getErrorInfo());
        }
        traceSession.setEnd_time(DateTime.now().getMillis());
        output_rerun_chat_history.add(traceSession);
        DataParametersUtil.updateParameterValue(output_rerun_chat, WorkflowJSONKey.chat_history.name(), chatHistory2Json(output_rerun_chat_history, Boolean.TRUE));
        outputs.put(WorkflowJSONKey.output_rerun_chat.name(), output_rerun_chat);
        inputs.remove(WorkflowJSONKey.chat_history.name());
        workflowRunResult.setInputs(inputs);
        workflowRunResult.setOutputs(outputs);
        return traceSession;
    }


    public List<TraceSession> getChatHistory(JSONObject inputs) {
        return DataParametersUtil.getValuesByTemplate(WorkflowJSONKey.chat_history.name(), inputs, TraceSession.class, Lists.newArrayList());
    }

    public List<JSONObject> chatHistory2Json(List<TraceSession> traceSessions, Boolean withOutputs) {
        if (CollectionUtils.isEmpty(traceSessions)) {
            return Lists.newArrayList();
        }
        return traceSessions.stream().map(c -> chatHistory2Json(c, withOutputs)).filter(MapUtils::isNotEmpty).sorted(comparing(k -> k.getLong("start_time"))).collect(Collectors.toList());
    }


    public JSONObject chatHistory2Json(TraceSession traceSession, Boolean withOutputs) {
        if (null == traceSession) {
            return null;
        }
        JSONObject t = new JSONObject();
        t.put(WorkflowJSONKey.id.name(), traceSession.getId());
        t.put("input", traceSession.getInput());
        t.put(WorkflowJSONKey.output.name(), traceSession.getOutput());
        if (withOutputs) {
            t.put("outputs", traceSession.getOutputs());
        }
        t.put("tags", traceSession.getTags());
        t.put("status", traceSession.getStatus());
        t.put("check_status", traceSession.getCheck_status());
        t.put("check_at", traceSession.getCheck_at());
        t.put("start_time", traceSession.getStart_time());
        t.put("end_time", traceSession.getEnd_time());
        t.put("parent_id", traceSession.getParent_id());
        return t;

    }

    public WorkflowReRunType getWorkflowReRunType(List<TraceSession> chatHistory) {
        if (CollectionUtils.isEmpty(chatHistory)) {
            return WorkflowReRunType.重新生成;
        }
        return WorkflowReRunType.valueOf(chatHistory.get(chatHistory.size() - 1).getTags().getFirst());
    }


    public List<FileInfo> dfs2Files(List<DfsInfo> dfsList){
        if(CollectionUtils.isEmpty(dfsList)){
            return Lists.newArrayList();
        }
        return dfsList.stream().map(d->{
            if(MapUtils.isEmpty(d.getMeta())){
                return null;
            }
            String doc_id = d.getMeta().getString(WorkflowJSONKey.docId.name());
            String id = d.getMeta().getString(WorkflowJSONKey.taskId.name());
            if(StringUtils.isBlank(doc_id)){
                return null;
            }
            FileInfo fileInfo = new FileInfo();
            fileInfo.setName(d.getOriginName());
            fileInfo.setPath(d.getFilePath());
            fileInfo.setFile_size(d.getFileSize());
            fileInfo.setDoc_id(doc_id);
            fileInfo.setId(id);
            return fileInfo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public String getOutputTextParameterName() {
        return WorkflowJSONKey.output.name();
    }

    /**
     * 是否执行成功后更新完成状态
     *
     * @return 结果
     */
    public Boolean successUpdateFinish() {
        return Boolean.TRUE;
    }
    
    /**
     * 批量查询工作流运行节点详情（优化：减少数据库调用次数）
     * @param nodeIds 节点ID列表
     * @return 节点ID到节点对象的映射
     */
    private Map<Long, WorkflowRunResult> batchGetWorkflowRunResults(List<Long> nodeIds) throws Exception {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return Collections.emptyMap();
        }
        List<WorkflowRunResult> nodes = workflowRunResultClient.find(nodeIds).getData().getList();
        
        return nodes.stream().filter(Objects::nonNull)
            .collect(Collectors.toMap(BaseModel::getId, Function.identity(), (k1, k2) -> k1));
    }


}

