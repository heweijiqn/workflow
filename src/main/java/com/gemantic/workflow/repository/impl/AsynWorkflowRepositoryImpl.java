package com.gemantic.workflow.repository.impl;

import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.workflow.repository.AsynWorkflowRepository;
import com.gemantic.workflow.repository.ConsumerRepository;
import com.gemantic.workflow.repository.WorkflowRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class AsynWorkflowRepositoryImpl implements AsynWorkflowRepository {
    

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private ConsumerRepository consumerRepository;

    @Override
    public void pushWorkFlowRunResult(List<WorkflowRunResult> workflowRunResults) throws Exception {
        if (CollectionUtils.isEmpty(workflowRunResults)) {
            return;
        }
        workflowRunResults = workflowRunResults.stream().filter(w -> StringUtils.isNotBlank(w.getNodeQueueName())).collect(Collectors.toList());
        for (WorkflowRunResult workflowRunResult : workflowRunResults) {
            workflowRepository.runNode(workflowRunResult,consumerRepository.getConsumer(workflowRunResult.getNodeType()));
        }
    }


}
