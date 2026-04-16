package com.gemantic.workflow.job;

import com.alibaba.fastjson2.JSON;
import com.gemantic.db.batch.DBDataBatch;
import com.gemantic.db.support.DBQuery;
import com.gemantic.db.support.DBQueryItem;
import com.gemantic.db.util.DBUtil;
import com.gemantic.gpt.client.TaskExecutionClient;
import com.gemantic.gpt.client.WorkflowRunClient;
import com.gemantic.gpt.client.WorkflowRunResultClient;
import com.gemantic.gpt.constant.ConsumerMode;
import com.gemantic.gpt.constant.WorkflowRunMode;
import com.gemantic.gpt.constant.WorkflowRunStatus;
import com.gemantic.gpt.constant.WorkflowRunType;
import com.gemantic.gpt.model.WorkflowRun;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.redisearch.client.RedisQueueClient;
import com.gemantic.redisearch.client.RedisZsetClient;
import com.gemantic.redisearch.constants.QueueOperate;
import com.gemantic.springcloud.utils.RandomUtil;
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
public class QueueTimeoutJob implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(QueueTimeoutJob.class);

    @Resource
    protected WorkflowRunResultClient workflowRunResultClient;

    @Resource
    protected WorkflowRunClient workflowRunClient;

    @Resource
    protected WorkflowRepository workflowRepository;

    @Value("${consumer.mode:QUEUE}")
    private String consumerMode;


    @Value("${node.run.retry.milliseconds:300000}")
    protected Long nodeRunRetryMilliseconds;

    @Value("${node.retry.job.milliseconds:300000}")
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
        runRetry();
    }

    private void runRetry(){
        if(!ConsumerMode.QUEUE.name().equalsIgnoreCase(consumerMode) && !ConsumerMode.ZSET.name().equalsIgnoreCase(consumerMode)){
            return;
        }
        long maxUpdateAt = DateTime.now().getMillis() - nodeRunRetryMilliseconds;
        long minUpdateAt = new DateTime(maxUpdateAt).plusDays(-1).getMillis();
        Map<String, String> params = Maps.newHashMap();
        params.put("status", WorkflowRunStatus.QUEUED.name());
        params.put("workflowRunType", WorkflowRunType.RUN.name());
        params.put("mode", WorkflowRunMode.RUNTIME.name());
        params.put("updateAt", String.join(":", "range", String.valueOf(minUpdateAt), String.valueOf(maxUpdateAt)));
        DBQuery<Long> query = new DBQuery<>();
        List<DBQueryItem> and = Lists.newArrayList();
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
        query.setPageSize(100);
        try {
            DBDataBatch.execute(query,batch->{
                for(WorkflowRunResult b : batch){
                    if(StringUtils.isBlank(b.getNodeQueueName())){
                        continue;
                    }
                    try {
                        String id = String.valueOf(b.getId());
                        String queueName = b.getNodeQueueName();
                        Long index = null;

                        if (ConsumerMode.QUEUE.name().equals(consumerMode)) {
                            String indexResult = redisQueueClient.index(queueName, QueueOperate.INDEX_OF, id).getData();
                            if (NumberUtils.isDigits(indexResult)) {
                                index = Long.valueOf(indexResult);
                            }
                        } else {
                            index = redisZsetClient.index(queueName, id).getData();
                        }
                        if(null != index && index>=0L){
                            continue;
                        }
                        Map<String, String> runParams = Maps.newHashMap();
                        runParams.put("id", String.valueOf(b.getWorkflowRunId()));
                        runParams.put("includeFields", "id,status,updateAt");
                        List<WorkflowRun> workflowRuns = workflowRunClient.find(1, 1, runParams).getData().getList();
                        if (CollectionUtils.isEmpty(workflowRuns)) {
                            continue;
                        }
                        if (WorkflowRunStatus.FAILED.name().equals(workflowRuns.getFirst().getStatus()) || WorkflowRunStatus.FINISHED.name().equals(workflowRuns.getFirst().getStatus())) {
                            continue;
                        }
                        WorkflowRunResult workflowRunResult = workflowRunResultClient.getById(b.getId()).getData();
                        if(null == workflowRunResult){
                            continue;
                        }
                        if(!WorkflowRunStatus.QUEUED.name().equalsIgnoreCase(workflowRunResult.getStatus()) || DateTime.now().getMillis()-workflowRunResult.getUpdateAt()<nodeRunRetryMilliseconds){
                            continue;
                        }
                        workflowRepository.pushWorkFlowRunResult(Lists.newArrayList(workflowRunResult),Boolean.TRUE);
                        LOG.warn("排队超时{}分 重新推送队列={} 工作流节点运行id={} 工作流运行id={} 工作流id={} 节点名称={} 节点类型={}",nodeRunRetryMilliseconds / 60000,workflowRunResult.getNodeQueueName(), workflowRunResult.getId(), workflowRunResult.getWorkflowRunId(), workflowRunResult.getWorkflowId(), workflowRunResult.getNodeName(), workflowRunResult.getNodeType());
                    }catch (Exception e){
                        LOG.error("排队超时处理出错 工作流节点运行id={} 工作流运行id={} 节点类型={}", b.getId(), b.getWorkflowRunId(), b.getNodeType(), e);
                    }
                }
                return Lists.newArrayList();
            },workflowRunResultClient);

        } catch (Exception e) {
            LOG.error("排队超时记录查询出错 查询条件={}", new String(JSON.toJSONBytes(query)), e);
        }
    }


    public Long getIntervalMilliseconds() {
        return intervalMilliseconds + RandomUtil.generateInRange(1000,25000);
    }
}

