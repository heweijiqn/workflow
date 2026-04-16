package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.workflow.Node;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 数据输出
 */
@Component("jsonOutput")
public class JsonOutput extends BaseConsumer {

    private static final List<String> EXCLUDE_TYPES = Lists.newArrayList(WorkflowJSONKey.chart_setup.name(), WorkflowJSONKey.content_output.name());

    @Value("${CONSUMER_JSON_OUTPUT_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize(){
        return MoreObjects.firstNonNull(consumerBatchSize,super.getConsumerBatchSize());
    }


    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        JSONObject inputs = WorkflowUtil.getInputsByTemplate(workflowRunResult.getInputs());
        JSONObject nodeInputs = null;
        if(MapUtils.isNotEmpty(workflowRunResult.getData()) && null != workflowRunResult.getData().get(WorkflowJSONKey.nodesHandle.name()) && CollectionUtils.isNotEmpty(workflowRunResult.getData().getList(WorkflowJSONKey.nodesHandle.name(), Node.class))){
            nodeInputs = WorkflowUtil.getInputsByTemplate(workflowRunResult.getData().getList(WorkflowJSONKey.nodesHandle.name(), Node.class).getFirst().getData().getTemplate());
        }else {
            nodeInputs = new JSONObject();
            for (Map.Entry<String, Object> entry : inputs.entrySet()) {
                JSONObject parameter = DataParametersUtil.getParameterByTemplate(entry.getKey(), inputs);
                if(!parameter.getBooleanValue("is_start",Boolean.FALSE)){
                    nodeInputs.put(entry.getKey(),entry.getValue());
                }
            }
        }
        JSONObject result = new JSONObject();
        List<String> mappingSource = Lists.newArrayList();
        List<JSONObject> parameters_mapping = DataParametersUtil.getValuesByTemplate(WorkflowJSONKey.parameters_mapping.name(), nodeInputs, JSONObject.class, Lists.newArrayList());
        for (JSONObject parameter_mapping : parameters_mapping) {
            String source = parameter_mapping.getString(WorkflowJSONKey.source.name());
            if (StringUtils.isBlank(source)) {
                continue;
            }
            JSONObject parameter = DataParametersUtil.getParameterByTemplate(source, inputs);
            String type = DataParametersUtil.getTypeByParameter(parameter);
            if (EXCLUDE_TYPES.contains(type)) {
                continue;
            }
            String target = parameter_mapping.getString(WorkflowJSONKey.target.name());
            if (StringUtils.isBlank(target)) {
                continue;
            }
            mappingSource.add(source);
            result.put(target, DataParametersUtil.getValueByParameter(parameter, null));
        }

        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            JSONObject parameter = DataParametersUtil.getParameterByTemplate(entry.getKey(), inputs);
            String name = DataParametersUtil.getNameByParameter(parameter);
            if (mappingSource.contains(name)) {
                continue;
            }
            if (!nodeInputs.containsKey(name) || WorkflowJSONKey.parameters_mapping.name().equalsIgnoreCase(name)) {
                continue;
            }
            String type = DataParametersUtil.getTypeByParameter(parameter);
            if (EXCLUDE_TYPES.contains(type)) {
                continue;
            }
            result.put(entry.getKey(), DataParametersUtil.getValueByParameter(parameter, null));
        }


        List<JSONObject> outputs = Lists.newArrayList();
        JSONObject output = WorkflowUtil.createOutput(WorkflowJSONKey.output.name(), WorkflowJSONKey.json_output.name(), result);
        outputs.add(output);
        return Response.ok(outputs);
    }


}
