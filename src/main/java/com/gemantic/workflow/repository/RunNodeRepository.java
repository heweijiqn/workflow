package com.gemantic.workflow.repository;

import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.workflow.consumer.BaseConsumer;

public interface RunNodeRepository {

    void runNode(WorkflowRunResult workflowRunResult, BaseConsumer baseConsumer);
}
