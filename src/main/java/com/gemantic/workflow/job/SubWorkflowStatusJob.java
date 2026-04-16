package com.gemantic.workflow.job;

import com.alibaba.fastjson2.JSON;
import com.gemantic.gpt.client.WorkflowRunClient;
import com.gemantic.gpt.client.WorkflowRunResultClient;
import com.gemantic.gpt.constant.*;
import com.gemantic.gpt.model.WorkflowRun;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.utils.RandomUtil;
import com.gemantic.workflow.repository.WorkflowRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
public class SubWorkflowStatusJob implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SubWorkflowStatusJob.class);
    private static final List<String> FINISHED_STATUS = Lists.newArrayList(WorkflowRunStatus.NOT_RUN.name(),WorkflowRunStatus.FAILED.name(),WorkflowRunStatus.FINISHED.name());
    @Resource
    protected WorkflowRunResultClient workflowRunResultClient;
    @Resource
    protected WorkflowRunClient workflowRunClient;
    @Resource
    protected WorkflowRepository workflowRepository;
    @Value("${subworkflow.status.job.milliseconds:120000}")
    protected Long intervalMilliseconds;


    @Setter
    @Getter
    protected Date startDate;

    public void run() {
        long maxUpdateAt = DateTime.now().plusMinutes(-10).getMillis();
        long minUpdateAt = new DateTime(maxUpdateAt).plusHours(-2).getMillis();
        runByUpdateAt(minUpdateAt,maxUpdateAt);
    }

    public void runByUpdateAt(Long updateAtMin,Long updateAtMax) {
        Map<String, String> params = Maps.newHashMap();
        params.put("status", String.join(",", WorkflowRunStatus.RUNNING.name()));
        params.put("nodeType",String.join(",", WorkflowNodeType.sub_workflow.name(),WorkflowNodeType.sub_workflow_list.name()));
        params.put("workflowRunType", WorkflowRunType.RUN.name());
        params.put("mode", WorkflowRunMode.RUNTIME.name());
        params.put("includeFields", "id,nodeType,workflowRunId,workflowId,parentId,position,updateAt,nodeQueueName,ignoreError");
        if(null != updateAtMin && null != updateAtMax) {
            params.put("updateAt", String.join(":", "range", updateAtMin.toString(), updateAtMax.toString()));
        }else if(null != updateAtMin){
            params.put("updateAt", String.join(":", "range", updateAtMin.toString()));
        }else if(null != updateAtMax){
            params.put("updateAt", String.join("::", "range", updateAtMax.toString()));
        }
        try {
            List<WorkflowRunResult> workflowRunResults = workflowRunResultClient.find(Lists.newArrayList("workflowRunId","parentId"),Lists.newArrayList("ASC","DESC"),1, Integer.MAX_VALUE, params).getData().getList();
            if (CollectionUtils.isEmpty(workflowRunResults)) {
                return;
            }
            long l1 = DateTime.now().getMillis();
            int queryCount =  workflowRunResults.size();
            LOG.warn("疑似子工作流运行状态异常数={} 查询参数={}",queryCount, params);
            int dealCount = 0;
            for (WorkflowRunResult workflowRunResult : workflowRunResults) {
                dealCount+=run(workflowRunResult);
            }
            LOG.warn("疑似子工作流运行状态异常数={} 处理数={} 处理耗时={} 查询参数={}", queryCount,dealCount,(DateTime.now().getMillis()-l1), params);
        } catch (Exception e) {
            LOG.error("查询疑似子工作流运行状态异常记录出错 查询参数={}", params, e);
        }
    }

    private int run(WorkflowRunResult workflowRunResult) {
        try {
            Map<String, String> runParams = Maps.newHashMap();
            runParams.put("id", String.valueOf(workflowRunResult.getWorkflowRunId()));
            runParams.put("includeFields", "id,status");
            List<WorkflowRun> workflowRuns = workflowRunClient.find(1, 1, runParams).getData().getList();
            if (CollectionUtils.isEmpty(workflowRuns)) {
                return 0;
            }
            if (WorkflowRunStatus.FAILED.name().equals(workflowRuns.getFirst().getStatus()) || WorkflowRunStatus.FINISHED.name().equals(workflowRuns.getFirst().getStatus())) {
                WorkflowRunResult result = workflowRunResultClient.getById(workflowRunResult.getId()).getData();
                if(null == result || !WorkflowRunStatus.RUNNING.name().equalsIgnoreCase(result.getStatus())){
                    return 0;
                }
                workflowRepository.updateWorkflowRunResult(Lists.newArrayList(workflowRunResult.getId()), DateTime.now().getMillis(), workflowRuns.getFirst().getStatus(), StringUtils.EMPTY, Boolean.TRUE);
                LOG.warn("{} 子工作流运行状态异常修复 节点={}({}) 更新状态为工作流运行状态 工作流运行id={} 工作流id={} 工作流运行状态={}"
                        , result.getNodeType(), result.getNodeName(), result.getId(), result.getWorkflowRunId(), result.getWorkflowId(),workflowRuns.getFirst().getStatus());
                return 1;
            }
            WorkflowRunResult result = workflowRunResultClient.getById(workflowRunResult.getId()).getData();
            if(null == result || !WorkflowRunStatus.RUNNING.name().equalsIgnoreCase(result.getStatus())){
                return 0;
            }
            Map<String, String> subParams = Maps.newHashMap();
            subParams.put("parentId", String.valueOf(workflowRunResult.getId()));
            subParams.put("workflowRunType", WorkflowRunType.RUN.name());
            subParams.put("mode", WorkflowRunMode.RUNTIME.name());
            subParams.put("includeFields", "id,status,subWorkflowRunIndex,nodeType,position,parentId,errorInfo,finishAt,updateAt");
            List<WorkflowRunResult> children = workflowRunResultClient.find(Lists.newArrayList("updateAt"),Lists.newArrayList("DESC"),1,Integer.MAX_VALUE,subParams).getData().getList();
            if(CollectionUtils.isEmpty(children) || (DateTime.now().getMillis() - children.getFirst().getUpdateAt()) < intervalMilliseconds){
                return 0;
            }
            List<WorkflowRunResult> errors = children.stream().filter(s->WorkflowRunStatus.FAILED.name().equalsIgnoreCase(s.getStatus())).collect(Collectors.toList());
            if((null == workflowRunResult.getIgnoreError() || workflowRunResult.getIgnoreError() == 0) && CollectionUtils.isNotEmpty(errors)){
                String errorInfo = errors.stream().map(WorkflowRunResult::getErrorInfo).filter(StringUtils::isNotBlank).collect(Collectors.joining(";"));
                workflowRepository.updateWorkflowRunStatusByFailure(result,errorInfo,DateTime.now().getMillis());
                LOG.warn("{} 子工作流运行状态异常修复 节点={}({}) 运行失败 工作流运行id={} 工作流id={} error={}"
                        , result.getNodeType(), result.getNodeName(), result.getId(), result.getWorkflowRunId(), result.getWorkflowId(),errorInfo);
                return 1;
            }
            List<WorkflowRunResult> notFinished = children.stream().filter(c->!FINISHED_STATUS.contains(c.getStatus())).collect(Collectors.toList());
            if(CollectionUtils.isNotEmpty(notFinished)){
                return 0;
            }
            if(Position.BRANCH.name().equalsIgnoreCase(workflowRunResult.getPosition())) {
                WorkflowRunResult firstNode = workflowRunResultClient.getById(children.getFirst().getId()).getData();
                if(null == firstNode){
                    LOG.warn("{} 子工作流运行状态异常修复忽略 节点={}({}) 工作流运行id={} 工作流id={} 列表数据序号={}({})已被删除"
                            , result.getNodeType(), result.getNodeName(), result.getId(), result.getWorkflowRunId(), result.getWorkflowId(),children.getFirst().getSubWorkflowRunIndex(),children.getFirst().getId());
                    return 0;
                }
                workflowRepository.updateWorkflowRunResultBranchFinish(DateTime.now().getMillis(),firstNode);
                LOG.warn("{} 子工作流运行状态异常修复 节点={}({}) 运行完成 工作流运行id={} 工作流id={}"
                        , result.getNodeType(), result.getNodeName(), result.getId(), result.getWorkflowRunId(), result.getWorkflowId());
                return 1;
            }else {
                Optional<WorkflowRunResult> output = children.stream().filter(c-> WorkflowUtil.isOutputNode(c.getNodeType())).findFirst();
                if(output.isEmpty()){
                    LOG.warn("{} 子工作流运行状态异常修复忽略 找不到输出组件 节点={}({}) 工作流运行id={} 工作流id={}"
                            , result.getNodeType(), result.getNodeName(), result.getId(), result.getWorkflowRunId(), result.getWorkflowId());
                    return 0;
                }
                WorkflowRunResult outputNode = workflowRunResultClient.getById(output.get().getId()).getData();
                if(null == outputNode){
                    LOG.warn("{} 子工作流运行状态异常修复忽略 输出组件已被删除 节点={}({}) 工作流运行id={} 工作流id={}"
                            , result.getNodeType(), result.getNodeName(), result.getId(), result.getWorkflowRunId(), result.getWorkflowId());
                    return 0;
                }
                workflowRepository.updateWorkflowRunResultLeafFinish(DateTime.now().getMillis(), outputNode);
                LOG.warn("{} 子工作流运行状态异常修复 节点={}({}) 运行完成 工作流运行id={} 工作流id={}"
                        , result.getNodeType(), result.getNodeName(), result.getId(), result.getWorkflowRunId(), result.getWorkflowId());
                return 1;
            }

        } catch (Throwable e) {
            LOG.error("{} 子工作流运行状态异常修复出错 工作流节点运行id={} 工作流运行id={} 工作流id={}", workflowRunResult.getNodeType(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(),workflowRunResult.getWorkflowId(), e);
        }
        return 0;
    }

    public Long getIntervalMilliseconds() {
        return intervalMilliseconds + RandomUtil.generateInRange(0,20000);
    }

}

