package com.gemantic.workflow.repository;

import com.gemantic.gpt.model.WorkflowRunResult;
import org.springframework.scheduling.annotation.Async;

import java.util.List;

public interface AsynWorkflowRepository {

    @Async
    void pushWorkFlowRunResult(List<WorkflowRunResult> workflowRunResults) throws Exception;


}
