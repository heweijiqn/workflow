package com.gemantic.workflow.support;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.model.WorkflowRunResult;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.Optional;

/**
 * 工作流运行状态，继承 LangGraph4j AgentState
 */
public class WorkflowState extends AgentState {

    public static final String RUN_RESULTS = "runResults";
    public static final String INPUTS = "inputs";
    public static final String CURRENT_NODE_ID = "currentNodeId";
    public static final String ERROR = "error";

    public WorkflowState(Map<String, Object> initData) {
        super(initData);
    }

    @SuppressWarnings("unchecked")
    public Map<String, WorkflowRunResult> getRunResults() {
        return this.<Map<String, WorkflowRunResult>>value(RUN_RESULTS).orElse(null);
    }

    public JSONObject getInputs() {
        return this.<JSONObject>value(INPUTS).orElse(new JSONObject());
    }

    public Optional<String> getCurrentNodeId() {
        return value(CURRENT_NODE_ID);
    }

    public Optional<String> getError() {
        return value(ERROR);
    }
}
