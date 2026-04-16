﻿package com.gemantic.workflow.support;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.model.WorkflowPlan;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.workflow.Edge;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.MsgUtil;
import com.gemantic.workflow.consumer.BaseConsumer;
import com.gemantic.workflow.repository.ConsumerRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.NodeAction;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

/**
 * 将 WorkflowPlan 构建为 LangGraph4j StateGraph
 */
public class WorkflowGraphBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowGraphBuilder.class);

    private final ConsumerRepository consumerRepository;

    public WorkflowGraphBuilder(ConsumerRepository consumerRepository) {
        this.consumerRepository = consumerRepository;
    }

    public StateGraph<WorkflowState> buildGraph(
            WorkflowPlan workflowPlan,
            Map<String, WorkflowRunResult> plan) throws Exception {

        var graph = new StateGraph<>(WorkflowState::new);

        List<String> beginNodeIds = workflowPlan.getBeginNodeIds();
        List<String> endNodeIds = workflowPlan.getEndNodeIds();

        // 添加所有节点
        for (Map.Entry<String, WorkflowRunResult> entry : plan.entrySet()) {
            String nodeId = entry.getKey();
            WorkflowRunResult runResult = entry.getValue();
            graph.addNode(nodeId, buildNodeAction(nodeId, runResult, plan));
        }

        // START -> 第一个起始节点
        if (!beginNodeIds.isEmpty()) {
            graph.addEdge(START, beginNodeIds.get(0));
        }

        // 添加节点间的边
        for (Map.Entry<String, WorkflowRunResult> entry : plan.entrySet()) {
            String nodeId = entry.getKey();
            WorkflowRunResult runResult = entry.getValue();

            if (runResult.getData() == null) continue;
            List<Edge> edges = runResult.getData().getList(WorkflowJSONKey.edgesHandle.name(), Edge.class);
            if (CollectionUtils.isEmpty(edges)) continue;

            List<Edge> outEdges = edges.stream()
                .filter(e -> nodeId.equals(e.getSource()) && StringUtils.isNotBlank(e.getTarget()))
                .filter(e -> plan.containsKey(e.getTarget()))
                .collect(Collectors.toList());

            if (CollectionUtils.isEmpty(outEdges)) {
                graph.addEdge(nodeId, END);
                continue;
            }

            boolean isIfElse = "if_else".equalsIgnoreCase(runResult.getNodeType());

            if (isIfElse) {
                Map<String, String> branchMapping = new HashMap<>();
                for (Edge e : outEdges) {
                    String handle = StringUtils.isNotBlank(e.getSourceHandle()) ? e.getSourceHandle() : e.getTarget();
                    branchMapping.put(handle, e.getTarget());
                }
                branchMapping.put(END, END);

                graph.addConditionalEdges(nodeId, state -> {
                    WorkflowRunResult result = state.getRunResults() != null
                        ? state.getRunResults().get(nodeId) : null;
                    if (result == null || result.getOutputs() == null) return END;
                    JSONObject ifElseValue = DataParametersUtil.getValueByTemplate(
                        WorkflowJSONKey.output.name(), result.getOutputs(), JSONObject.class, new JSONObject());
                    String branchId = ifElseValue.getString(WorkflowJSONKey.branchId.name());
                    if (StringUtils.isBlank(branchId)) return END;
                    return branchMapping.containsKey(branchId) ? branchId : END;
                }, branchMapping);
            } else {
                for (Edge e : outEdges) {
                    graph.addEdge(nodeId, e.getTarget());
                }
            }
        }

        return graph;
    }

    private NodeAction<WorkflowState> buildNodeAction(
            String nodeId,
            WorkflowRunResult runResult,
            Map<String, WorkflowRunResult> plan) {

        return state -> {
            if (WorkflowUtil.WORKFLOW_RUN_STATUS_FINISHED.contains(runResult.getStatus())) {
                return Map.of(WorkflowState.CURRENT_NODE_ID, nodeId);
            }

            BaseConsumer consumer = consumerRepository.getConsumer(runResult.getNodeType());
            if (consumer == null) {
                String error = String.format("（%s）组件已废弃，请更换其他组件", runResult.getNodeName());
                runResult.setStatus("FAILED");
                runResult.setErrorInfo(error);
                runResult.setFinishAt(DateTime.now().getMillis());
                if (runResult.getIgnoreError() != 1) {
                    return Map.of(WorkflowState.ERROR, error, WorkflowState.CURRENT_NODE_ID, nodeId);
                }
                return Map.of(WorkflowState.CURRENT_NODE_ID, nodeId);
            }

            try {
                JSONObject inputs = resolveInputs(state, runResult, plan);
                if (inputs == null) {
                    return Map.of(WorkflowState.CURRENT_NODE_ID, nodeId);
                }
                runResult.setInputs(inputs);
                runResult.setRunAt(DateTime.now().getMillis());
                runResult.setStatus("RUNNING");

                Response<List<JSONObject>> outputResponse = consumer.getOutputs(runResult);

                if (MsgUtil.isValidMessage(outputResponse)) {
                    JSONObject outputs = WorkflowUtil.mergeOutputs(runResult, outputResponse.getData());
                    runResult.setOutputs(outputs);
                    runResult.setStatus("FINISHED");
                    runResult.setFinishAt(DateTime.now().getMillis());
                    LOG.info("节点运行成功 nodeId={} nodeType={}", nodeId, runResult.getNodeType());
                } else {
                    String error = outputResponse != null && outputResponse.getMessage() != null
                        ? outputResponse.getMessage().getMessage()
                        : String.format("（%s）组件输出结果为空", runResult.getNodeName());
                    runResult.setStatus("FAILED");
                    runResult.setErrorInfo(error);
                    runResult.setFinishAt(DateTime.now().getMillis());
                    LOG.error("节点运行失败 nodeId={} error={}", nodeId, error);
                    if (runResult.getIgnoreError() != 1) {
                        return Map.of(WorkflowState.ERROR, error, WorkflowState.CURRENT_NODE_ID, nodeId);
                    }
                }
            } catch (Exception e) {
                String error = String.format("（%s）组件运行出错: %s", runResult.getNodeName(), e.getMessage());
                runResult.setStatus("FAILED");
                runResult.setErrorInfo(error);
                runResult.setFinishAt(DateTime.now().getMillis());
                LOG.error("节点运行异常 nodeId={}", nodeId, e);
                if (runResult.getIgnoreError() != 1) {
                    return Map.of(WorkflowState.ERROR, error, WorkflowState.CURRENT_NODE_ID, nodeId);
                }
            }

            return Map.of(WorkflowState.CURRENT_NODE_ID, nodeId);
        };
    }

    private JSONObject resolveInputs(WorkflowState state,
                                     WorkflowRunResult runResult,
                                     Map<String, WorkflowRunResult> plan) {
        if (runResult.getData() == null) return runResult.getInputs();

        List<Edge> edges = runResult.getData().getList(WorkflowJSONKey.edgesHandle.name(), Edge.class);
        JSONObject baseInputs = DataParametersUtil.mergeParameters(runResult.getInputs(), state.getInputs());
        runResult.setInputs(baseInputs);

        if (CollectionUtils.isEmpty(edges)) return baseInputs;

        List<Edge> sources = edges.stream()
            .filter(e -> runResult.getNodeId().equals(e.getTarget()) && StringUtils.isNotBlank(e.getSource()))
            .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(sources)) return baseInputs;

        List<String> sourceNodeIds = sources.stream().map(Edge::getSource).distinct().collect(Collectors.toList());
        Map<String, WorkflowRunResult> sourceResults = sourceNodeIds.stream()
            .filter(plan::containsKey)
            .filter(id -> WorkflowUtil.WORKFLOW_RUN_STATUS_FINISHED.contains(plan.get(id).getStatus()))
            .collect(Collectors.toMap(id -> id, plan::get));

        if (sourceResults.size() < sourceNodeIds.size()) {
            LOG.info("节点 {} 上游未全部完成，跳过", runResult.getNodeId());
            return null;
        }

        JSONObject mergedInputs = baseInputs;
        for (Edge edge : sources) {
            WorkflowRunResult sourceResult = sourceResults.get(edge.getSource());
            if (sourceResult == null || sourceResult.getOutputs() == null) continue;
            if (StringUtils.isBlank(edge.getSourceHandle()) || StringUtils.isBlank(edge.getTargetHandle())) continue;
            JSONObject sourceOutput = DataParametersUtil.getParameterByTemplate(
                edge.getSourceHandle(), sourceResult.getOutputs());
            if (sourceOutput != null) {
                mergedInputs.put(edge.getTargetHandle(), sourceOutput);
            }
        }
        return mergedInputs;
    }
}
