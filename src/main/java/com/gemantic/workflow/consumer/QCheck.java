package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.gemantic.dfs.support.DfsInfo;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.FileUpload;
import com.gemantic.gpt.support.workflow.Template;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.workflow.client.QCheckExtractApiClient;
import com.gemantic.workflow.support.FormatKnowledge;
import com.google.common.base.MoreObjects;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * QCheck
 */
@Component("qCheck")
public class QCheck extends BaseConsumer {

    @Resource
    private QCheckExtractApiClient qCheckExtractApiClient;

    @Value("${CONSUMER_Q_CHECK_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize(){
        return MoreObjects.firstNonNull(consumerBatchSize,super.getConsumerBatchSize());
    }

    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        LOG.info("执行qCheck组件");
        Template template = DataParametersUtil.getValueByTemplate("file", workflowRunResult.getInputs(), Template.class, null);
        String file_display_name = DataParametersUtil.getDisplayNameByTemplate("file", workflowRunResult.getInputs());
        Response<List<JSONObject>> errorResponse = Response.error(null);
        if (null == template) {
            String error = String.format("（%s）组件中%s为空", workflowRunResult.getNodeName()
                    , file_display_name);
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
//            throw new WorkflowException(error);
        }
        String fileInputKey = template.getName();
        if (!workflowRunResult.getInputs().containsKey(fileInputKey)) {
            String error = String.format("（%s）组件中%s的\"%s\"不存在", workflowRunResult.getNodeName()
                    , file_display_name, fileInputKey);
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
//            throw new WorkflowException(error);
        }
        FileUpload meta = DataParametersUtil.getMetaByParameter(workflowRunResult.getInputs().getJSONObject(fileInputKey), FileUpload.class);
        if (null == meta || StringUtils.isBlank(meta.getAppId()) || StringUtils.isBlank(meta.getAppFileTypeId())) {
            String error = String.format("（%s）组件中%s的\"%s\"没有配置识别场景,请配置后重试", workflowRunResult.getNodeName()
                    , file_display_name, fileInputKey);
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
//            throw new WorkflowException(error);
        }
        List<DfsInfo> files = DataParametersUtil.getValuesByTemplate(fileInputKey, workflowRunResult.getInputs(), DfsInfo.class, Lists.newArrayList());
        if (CollectionUtils.isEmpty(files)) {
            String error = String.format("（%s）组件中%s的\"%s\"为空", workflowRunResult.getNodeName()
                    , file_display_name, fileInputKey);
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
//            throw new WorkflowException(error);
        }
        String evidenceId = files.stream().filter(f -> MapUtils.isNotEmpty(f.getMeta()) && StringUtils.isNotBlank(MapUtils.getString(f.getMeta(), WorkflowJSONKey.docId.name()))).map(f -> MapUtils.getString(f.getMeta(), WorkflowJSONKey.docId.name())).distinct().collect(Collectors.joining(","));
        if (StringUtils.isBlank(evidenceId)) {
            String error = String.format("（%s）组件中%s的\"%s\"缺失docId", workflowRunResult.getNodeName()
                    , file_display_name, fileInputKey);
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
//            throw new WorkflowException(error);
        }
        String extractTaskId = files.stream().filter(f -> MapUtils.isNotEmpty(f.getMeta()) && StringUtils.isNotBlank(MapUtils.getString(f.getMeta(), WorkflowJSONKey.taskId.name()))).map(f -> MapUtils.getString(f.getMeta(), WorkflowJSONKey.taskId.name())).distinct().collect(Collectors.joining(","));

        List<JSONObject> labels = DataParametersUtil.getValuesByTemplate("labels", workflowRunResult.getInputs(), JSONObject.class, Lists.newArrayList());
        String labels_display_name = DataParametersUtil.getDisplayNameByTemplate("labels", workflowRunResult.getInputs());
        if (CollectionUtils.isEmpty(labels)) {
            String error = String.format("（%s）组件中%s为空，请选择后重试", workflowRunResult.getNodeName()
                    , labels_display_name);
            errorResponse.getMessage().setMessage(error);
            return errorResponse;
//            throw new WorkflowException(error);
        }
        List<JSONObject> outputs = Lists.newArrayList();
        for (JSONObject label : labels) {
            String appFileTypeLabelId = label.getString(WorkflowJSONKey.id.name());
            String name = label.getString(WorkflowJSONKey.name.name());
            List<FormatKnowledge> knowledges = null;
            try {
                knowledges = qCheckExtractApiClient.getExtractResult(evidenceId, extractTaskId, appFileTypeLabelId).getData();
            } catch (Exception e) {
                LOG.error("组件={}({}) 查询结果报错 标签={}({}) docId={}", workflowRunResult.getNodeName(), workflowRunResult.getId(), name, appFileTypeLabelId, e);
                if (workflowRunResult.getIgnoreError() == 0) {
                    String error = String.format("（%s）组件中输出\"\"结果报错", workflowRunResult.getNodeName()
                            , name);
                    errorResponse.getMessage().setMessage(error);
                    return errorResponse;
//                    throw new WorkflowException(error);
                }
            }
            if (CollectionUtils.isEmpty(knowledges)) {
                knowledges = Lists.newArrayList();
            }
            if(LOG.isDebugEnabled()) {
                LOG.debug("组件={}({}) 标签={}({}) docId={} result={}", workflowRunResult.getNodeName(), workflowRunResult.getId(), name, appFileTypeLabelId, knowledges);
            }
            JSONObject output = WorkflowUtil.createOutput(String.join("_", WorkflowJSONKey.output.name(), name), WorkflowJSONKey.qcheck_result.name(), JSON.toJSON(knowledges,DataParametersUtil.JSONWRITER_TOJSON_FEATURE));
            outputs.add(output);
        }
        return Response.ok(outputs);
    }

}
