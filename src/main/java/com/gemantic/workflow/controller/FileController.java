package com.gemantic.workflow.controller;


import com.alibaba.fastjson2.JSON;
import com.gemantic.gpt.client.WorkflowClient;
import com.gemantic.gpt.client.WorkflowRunClient;
import com.gemantic.gpt.client.WorkflowRunResultClient;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.springcloud.model.Response;
import com.gemantic.workflow.model.ExtractTask;
import com.gemantic.workflow.repository.WorkflowRepository;
import com.gemantic.workflow.support.ProcessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController
@RequestMapping(path = "/file")
@Tag(name = "文件上传", description = "文件上传相关接口")
public class FileController {

    private static final Logger LOG = LoggerFactory.getLogger(FileController.class);

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private WorkflowClient workflowClient;

    @Resource
    private WorkflowRunResultClient workflowRunResultClient;

    @Resource
    private WorkflowRunClient workflowRunClient;

    @Operation(
            summary = "接收索引结果",
            description = "接收文件解析索引的结果，包括成功和失败状态",
            operationId = "updateStatus"
    )
    @PostMapping("/status")
    @Async
    public void updateStatus(
            @Parameter(description = "文件解析任务对象", required = true) @RequestBody ExtractTask extractTask
    ) throws Exception {
        if (LOG.isInfoEnabled()) {
            LOG.info("接收文件解析索引成功/失败 {}", new String(JSON.toJSONBytes(extractTask)));
        }
        if (MapUtils.isEmpty(extractTask.getMeta())) {
            LOG.warn("extractTaskId={} meta没有值", extractTask.getId());
            return;
        }
        if (!NumberUtils.isDigits(extractTask.getMeta().getString(WorkflowJSONKey.workflowRunResultId.name()))) {
            LOG.warn("接收文件解析索引成功/失败 extractTaskId={} meta没有workflowRunResultId", extractTask.getId());
            return;
        }
        Long workflowRunResultId = extractTask.getMeta().getLong(WorkflowJSONKey.workflowRunResultId.name());
        WorkflowRunResult workflowRunResult = workflowRunResultClient.getById(workflowRunResultId).getData();
        if (null == workflowRunResult) {
            LOG.warn("接收文件解析索引成功/失败 extractTaskId={} workflowRunResultId={} 不存在", extractTask.getId(), workflowRunResultId);
            return;
        }
        if (CollectionUtils.isNotEmpty(extractTask.getDealSteps())) {
            if (extractTask.getDealStatus().intValue() == 1) {
                workflowRepository.updateWorkflowRunFinish(workflowRunResult, DateTime.now().getMillis());
                LOG.warn("文件处理成功 extractTaskId={} workflowRunResultId={}", extractTask.getId(), workflowRunResultId);

            } else if (extractTask.getDealStatus().intValue() == 0) {
                LOG.warn("文件处理中 {} extractTaskId={} workflowRunResultId={} ", extractTask.getDealMsg(), extractTask.getId(), workflowRunResultId);
            } else {
                String errorInfo = extractTask.getDealMsg();
                if (StringUtils.isNotBlank(extractTask.getDealError())) {
                    errorInfo = String.join(":", errorInfo, extractTask.getDealError());
                }
                workflowRepository.updateWorkflowRunStatusByFailure(workflowRunResult, errorInfo, DateTime.now().getMillis());
                LOG.warn("文件处理失败 {} extractTaskId={} workflowRunResultId={} 失败原因={}", extractTask.getDealMsg(), extractTask.getId(), workflowRunResultId, extractTask.getDealError());
            }
        } else {
            if (ProcessStatus.FINISHED.name().equalsIgnoreCase(extractTask.getIndexParseStatus())) {
                workflowRepository.updateWorkflowRunFinish(workflowRunResult, DateTime.now().getMillis());
                LOG.warn("文件索引成功 extractTaskId={} workflowRunResultId={}", extractTask.getId(), workflowRunResultId);
            } else if (ProcessStatus.FAILED.name().equalsIgnoreCase(extractTask.getParseStatus())) {
                String errorMsg = extractTask.getParseMsg();
                if (StringUtils.isBlank(errorMsg)) {
                    errorMsg = "文件解析失败";
                }
                workflowRepository.updateWorkflowRunStatusByFailure(workflowRunResult, errorMsg, DateTime.now().getMillis());

                LOG.warn("文件解析失败 extractTaskId={} workflowRunResultId={} error={}", extractTask.getId(), workflowRunResultId, errorMsg);

            } else if (ProcessStatus.FAILED.name().equalsIgnoreCase(extractTask.getExtractStatus())) {
                String errorMsg = extractTask.getExtractMsg();
                if (StringUtils.isBlank(errorMsg)) {
                    errorMsg = "文件目录抽取失败";
                }
                workflowRepository.updateWorkflowRunStatusByFailure(workflowRunResult, errorMsg, DateTime.now().getMillis());
                LOG.warn("文件目录抽取失败 extractTaskId={} workflowRunResultId={} error={}", extractTask.getId(), workflowRunResultId, errorMsg);

            } else if (ProcessStatus.FAILED.name().equalsIgnoreCase(extractTask.getIndexParseStatus())) {
                String errorMsg = extractTask.getIndexParseMsg();
                if (StringUtils.isBlank(errorMsg)) {
                    errorMsg = "文件索引失败";
                }
                workflowRepository.updateWorkflowRunStatusByFailure(workflowRunResult, errorMsg, DateTime.now().getMillis());
                LOG.warn("文件索引失败 extractTaskId={} workflowRunResultId={} errorMsg={}", extractTask.getId(), workflowRunResultId, errorMsg);

            }
        }
    }


}