package com.gemantic.workflow.job;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.gemantic.db.batch.DBDataBatch;
import com.gemantic.db.support.DBQuery;
import com.gemantic.db.support.DBQueryItem;
import com.gemantic.db.util.DBUtil;
import com.gemantic.gpt.client.TaskExecutionClient;
import com.gemantic.gpt.client.WorkflowRunClient;
import com.gemantic.gpt.client.WorkflowRunResultClient;
import com.gemantic.gpt.constant.*;
import com.gemantic.gpt.model.TaskExecution;
import com.gemantic.gpt.model.WorkflowRun;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.redisearch.client.RedisQueueClient;
import com.gemantic.redisearch.client.RedisZsetClient;
import com.gemantic.redisearch.constants.QueueOperate;
import com.gemantic.springcloud.constant.DBOperation;
import com.gemantic.springcloud.model.PageResponse;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.MsgUtil;
import com.gemantic.springcloud.utils.RandomUtil;
import com.gemantic.springcloud.utils.SpringBeanUtil;
import com.gemantic.workflow.consumer.BaseConsumer;
import com.gemantic.workflow.repository.WorkflowRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;


@Component
public class RunTimeoutJob implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(RunTimeoutJob.class);

    @Resource
    protected WorkflowRunResultClient workflowRunResultClient;

    @Resource
    protected WorkflowRunClient workflowRunClient;

    @Resource
    protected WorkflowRepository workflowRepository;

    @Value("${consumer.mode:QUEUE}")
    private String consumerMode;
    /**
     * 节点运行超时时间
     */
    @Value("${node.run.timeout.milliseconds:1800000}")
    protected Long nodeRunTimeoutMilliseconds;

    @Value("${node.run.retry.milliseconds:120000}")
    protected Long nodeRunRetryMilliseconds;

    @Value("${timeout.job.milliseconds:1800000}")
    protected Long intervalMilliseconds;

    @Resource
    protected RedisQueueClient redisQueueClient;

    @Resource
    protected RedisZsetClient redisZsetClient;

    @Resource
    private TaskExecutionClient taskExecutionClient;

    @Getter
    @Setter
    @Value("${workflow.task.execution}")
    private Boolean workflowTaskExecution;

    @Setter
    protected Date startDate;

    public void run() {
        runTimeout();
    }

    private void runTimeout(){
        if(!ConsumerMode.QUEUE.name().equalsIgnoreCase(consumerMode) && !ConsumerMode.ZSET.name().equalsIgnoreCase(consumerMode)){
            return;
        }
        String timeoutInfo = String.format("运行已超时%s分", String.valueOf(nodeRunTimeoutMilliseconds / 60000));
        long maxUpdateAt = DateTime.now().getMillis() - nodeRunTimeoutMilliseconds;
        long minUpdateAt = new DateTime(maxUpdateAt).plusHours(-2).getMillis();
        Map<String, String> params = Maps.newHashMap();
        params.put("status", WorkflowRunStatus.RUNNING.name());
        params.put("workflowRunType", WorkflowRunType.RUN.name());
        params.put("mode", WorkflowRunMode.RUNTIME.name());
        params.put("runAt", String.join(":", "range", String.valueOf(minUpdateAt), String.valueOf(maxUpdateAt)));
        DBQuery<Long> query = new DBQuery<>();
        DBQueryItem nodeTypeQuery = new DBQueryItem();
        nodeTypeQuery.setOperation(DBOperation.NIN);
        nodeTypeQuery.setField("nodeType");
        nodeTypeQuery.setValues(Lists.newArrayList(WorkflowNodeType.sub_workflow.name(),WorkflowNodeType.sub_workflow_list.name()));
        List<DBQueryItem> and = Lists.newArrayList(nodeTypeQuery);
        DBUtil.fillDBQueryItem(and,params,null,null);
        query.setAndQuery(and);
        query.setCurrentPage(1);
        Map<String, String> includeFieldsMap = Maps.newHashMap();
        List<String> includeFields = Lists.newArrayList( "id","nodeType","workflowRunId","workflowId","updateAt","nodeQueueName");
        includeFields.forEach(i -> {
            includeFieldsMap.put(i, i);
        });
        query.setIncludeFields(includeFieldsMap);
        query.setFetch(Boolean.FALSE);
        query.setPageSize(Integer.MAX_VALUE);
        try {
            List<WorkflowRunResult> workflowRunResults = workflowRunResultClient.query(query).getData().getList();
            if(CollectionUtils.isEmpty(workflowRunResults)){
                if(LOG.isInfoEnabled()) {
                    LOG.info("疑似节点运行超时记录数=0 查询条件={}", new String(JSON.toJSONBytes(query)));
                }
                return;
            }
            long l1 = DateTime.now().getMillis();
            int queryCount = workflowRunResults.size();
            LOG.warn("疑似节点运行超时记录数={} 查询条件={}",queryCount , new String(JSON.toJSONBytes(query)));
            int dealCount = 0;
            for (WorkflowRunResult workflowRunResult : workflowRunResults) {
                if (StringUtils.isBlank(workflowRunResult.getNodeQueueName())) {
                    continue;
                }
                String nodeType = workflowRunResult.getNodeType();
                String className = StrUtil.toCamelCase(nodeType);
                Map<String, BaseConsumer> consumerMap = SpringBeanUtil.getBeansOfType(BaseConsumer.class);
                BaseConsumer consumer = consumerMap.get(className);
                if (null == consumer || !consumer.available()) {
                    WorkflowRunResult result = workflowRunResultClient.getById(workflowRunResult.getId()).getData();
                    if(null != result) {
                        workflowRepository.updateWorkflowRunStatusByFailure(result,"该组件已废弃,请更换组件",DateTime.now().getMillis());
                        dealCount+=1;
                        LOG.warn("{} 已废弃更新失败状态 工作流节点运行id={} 工作流运行id={} 工作流id={} 节点名称={}", result.getNodeType(), result.getId(), result.getWorkflowRunId(), result.getWorkflowId(), result.getNodeName());
                    }
                    continue;
                }
                dealCount+=run(workflowRunResult, timeoutInfo);
            }
            LOG.warn("疑似节点运行超时记录数={} 处理数={} 处理耗时={} 查询条件={}",queryCount,dealCount,(DateTime.now().getMillis()-l1) , new String(JSON.toJSONBytes(query)));
        } catch (Exception e) {
            LOG.error("疑似节点运行超时记录查询出错 查询条件={}", new String(JSON.toJSONBytes(query)), e);
        }
    }

    private int run(WorkflowRunResult workflowRunResult, String timeoutInfo) {
        if (StringUtils.isBlank(workflowRunResult.getNodeQueueName())) {
            return 0;
        }
        try {
            if(workflowTaskExecution) {
                Map<String, String> taskParam = Maps.newHashMap();
                taskParam.put("workflowRunId", workflowRunResult.getWorkflowRunId().toString());
                try {
                    Response<PageResponse<TaskExecution>> taskExecution = taskExecutionClient.find(1, 0, taskParam);
                    if (MsgUtil.isValidMessage(taskExecution) && null != taskExecution.getData() && taskExecution.getData().getTotalCount() > 0L) {
                        LOG.warn("关联定时任务不做超时处理 工作流节点运行id={} 工作流运行id={} 节点类型={}", workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getNodeType());
                        return 0;
                    }
                } catch (Exception e) {
                    LOG.error("定时任务运行列表访问失败 taskParam={}", taskParam, e);
                }
            }
            Map<String, String> runParams = Maps.newHashMap();
            runParams.put("id", String.valueOf(workflowRunResult.getWorkflowRunId()));
            runParams.put("includeFields", "id,status");
            List<WorkflowRun> workflowRuns = workflowRunClient.find(1, 1, runParams).getData().getList();
            if (CollectionUtils.isEmpty(workflowRuns)) {
                return 0;
            }
            if (WorkflowRunStatus.FAILED.name().equals(workflowRuns.getFirst().getStatus()) || WorkflowRunStatus.FINISHED.name().equals(workflowRuns.getFirst().getStatus())) {
                workflowRepository.updateWorkflowRunResult(Lists.newArrayList(workflowRunResult.getId()), DateTime.now().getMillis(), WorkflowRunStatus.NOT_STARTED.name(), StringUtils.EMPTY, Boolean.TRUE);
                return 1;
            }
            String id = String.valueOf(workflowRunResult.getId());
            String queueName = workflowRunResult.getNodeQueueName();
            Long index = null;
            if (ConsumerMode.QUEUE.name().equals(consumerMode)) {
                String indexResult = redisQueueClient.index(queueName, QueueOperate.INDEX_OF, id).getData();
                if (NumberUtils.isDigits(indexResult)) {
                    index = Long.valueOf(indexResult);
                }
            } else if (ConsumerMode.ZSET.name().equals(consumerMode)) {
                index = redisZsetClient.index(queueName, id).getData();
            }
            if (null != index && index >= 0L) {
//                LOG.warn("运行超时忽略 工作流节点运行id={} 工作流运行id={} 节点类型={}  队列({})索引位置={}", workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getNodeType(), workflowRunResult.getNodeQueueName(), index);
                return 0;
            }
            WorkflowRunResult result = workflowRunResultClient.getById(workflowRunResult.getId()).getData();
            if (!WorkflowRunStatus.RUNNING.name().equals(result.getStatus()) || result.getUpdateAt() > (DateTime.now().getMillis() - nodeRunTimeoutMilliseconds)) {
                return 0;
            }
            workflowRepository.updateWorkflowRunStatusByFailure(result, timeoutInfo,DateTime.now().getMillis());
            LOG.warn("运行超时 更新节点运行状态为失败 工作流节点运行id={} 工作流运行id={} 工作流id={} 节点名称={} 节点类型={}", result.getId(), result.getWorkflowRunId(), result.getWorkflowId(), result.getNodeName(), result.getNodeType());
            return 1;
        } catch (Throwable e) {
            LOG.error("运行超时处理出错 工作流节点运行id={} 工作流运行id={} 节点类型={}", workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getNodeType(), e);
        }
        return 0;
    }



    public Long getIntervalMilliseconds() {
        return intervalMilliseconds + RandomUtil.generateInRange(0,20000);
    }

}

