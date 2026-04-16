package com.gemantic.workflow.consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.MoreObjects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.google.common.collect.Lists;

/**
 * 图表
 */
@Component("chartSetup")
public class ChartSetup extends BaseConsumer {

    @Value("${CONSUMER_CHART_SETUP_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize(){
        return MoreObjects.firstNonNull(consumerBatchSize,super.getConsumerBatchSize());
    }

    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        JSONObject inputs = workflowRunResult.getInputs();
        JSONObject originalInputs = WorkflowUtil.getInputsByTemplate(inputs);

        // 对 table_data 进行数据解析处理
        processTableData(originalInputs);

        JSONObject parameters = DataParametersUtil.mergeParameters(originalInputs, inputs);
        List<JSONObject> outputs = Lists.newArrayList();

        // 获取处理后的图表数据
        JSONObject tableDataParam = originalInputs.getJSONObject("table_data");
        Object chartDataValue = null;
        if (tableDataParam != null) {
            chartDataValue = tableDataParam.get("value");
        }
        // 直接从 originalInputs 获取 table_config
        JSONObject tableConfigParam = originalInputs.getJSONObject("table_config");
        JSONObject tableConfigValueObj = null;
        if (tableConfigParam != null) {
            Object configVal = tableConfigParam.get("value");
            if (configVal instanceof JSONObject) {
                tableConfigValueObj = (JSONObject) configVal;
            } else if (configVal instanceof Map) {
                tableConfigValueObj = new JSONObject((Map) configVal);
            }
        }

        // 获取 legend 和 valueList
        List<String> legend = null;
        List<Map<String, Object>> valueList = null;
        if (chartDataValue instanceof Map) {
            Map<String, Object> chartData = (Map<String, Object>) chartDataValue;
            legend = (List<String>) chartData.get("legend");
            valueList = (List<Map<String, Object>>) chartData.get("valueList");
        }

        // 只在 tableConfigValueObj 里添加 legend 和 valueList
        if (tableConfigValueObj != null) {
            tableConfigValueObj.put("legend", legend != null ? legend : Collections.emptyList());
            tableConfigValueObj.put("valueList", valueList != null ? valueList : Collections.emptyList());
        }

        // 用 tableConfigValueObj 作为 outputData
        Map<String, Object> outputData = tableConfigValueObj != null ? tableConfigValueObj : new LinkedHashMap<>();

        // 创建 output 输出（使用正确的 dict 类型）
        JSONObject chartDataOutput = WorkflowUtil.createOutput("output", WorkflowJSONKey.dict.name(), outputData);
        outputs.add(chartDataOutput);

        // 不设置workflowRunResult.setOutputs()，让框架直接使用我们返回的outputs列表
        return Response.ok(outputs);
    }

    @Override
    public Boolean successUpdateFinish() {
        // 返回true，让框架正常处理，但确保我们的outputs不包含模板定义的字段
        return Boolean.TRUE;
    }

    /**
     * 处理 table_data 数据转换
     */
    private void processTableData(JSONObject originalInputs) {
        if (originalInputs == null || originalInputs.isEmpty()) {
            return;
        }

        JSONObject tableDataParam = originalInputs.getJSONObject("table_data");
        if (tableDataParam == null) {
            return;
        }

        Object valueObj = tableDataParam.get("value");
        if (valueObj == null) {
            return;
        }

        // 检查是否为 chart_equity_penetration 类型
        JSONObject tableConfigParam = originalInputs.getJSONObject("table_config");
        boolean isEquityPenetration = false;
        if (tableConfigParam != null) {
            Object configVal = tableConfigParam.get("value");
            if (configVal instanceof JSONObject) {
                JSONObject configObj = (JSONObject) configVal;
                String type = configObj.getString("type");
                if ("chart_equity_penetration".equals(type)) {
                    isEquityPenetration = true;
                }
            } else if (configVal instanceof Map) {
                Map<String, Object> configMap = (Map<String, Object>) configVal;
                String type = (String) configMap.get("type");
                if ("chart_equity_penetration".equals(type)) {
                    isEquityPenetration = true;
                }
            }
        }

        // 如果是 chart_equity_penetration 类型，使用特殊处理逻辑
        if (isEquityPenetration) {
            processEquityPenetrationData(tableDataParam, valueObj);
            return;
        }

        // 转换为 List<Map<String, Object>>
        List<Map<String, Object>> rawList = null;
        if (valueObj instanceof List) {
            rawList = (List<Map<String, Object>>) valueObj;
        } else if (valueObj instanceof Map) {
            // 如果是空对象 {} 或其他 Map 类型，设置为空输出
            Map<String, Object> mapValue = (Map<String, Object>) valueObj;
            if (mapValue.isEmpty()) {
                setEmptyOutput(tableDataParam);
                return;
            } else {
                // 非空 Map 对象不是期望的格式，设置为空输出
                setEmptyOutput(tableDataParam);
                return;
            }
        } else {
            // 其他类型都不支持，设置为空输出
            setEmptyOutput(tableDataParam);
            return;
        }

        // 安全判断：value 为 null 或空，直接输出空结构
        if (rawList == null || rawList.isEmpty()) {
            setEmptyOutput(tableDataParam);
            return;
        }

        // 获取第一个对象并校验是否为空对象
        Map<String, Object> firstRow = rawList.getFirst();
        if (firstRow == null || firstRow.isEmpty()) {
            setEmptyOutput(tableDataParam);
            return;
        }

        // 用 LinkedHashMap 保持字段顺序，拿出 x轴 和 legend 字段
        LinkedHashMap<String, Object> orderedRow = new LinkedHashMap<>(firstRow);
        Iterator<String> iterator = orderedRow.keySet().iterator();

        if (!iterator.hasNext()) {
            setEmptyOutput(tableDataParam);
            return;
        }

        String xAxisField = iterator.next(); // 第一个字段是 x 轴字段
        List<String> legends = new ArrayList<>();
        while (iterator.hasNext()) {
            legends.add(iterator.next());
        }

        List<Map<String, Object>> resultValues = new ArrayList<>();

        for (Map<String, Object> row : rawList) {
            Map<String, Object> item = new LinkedHashMap<>();

            // name 字段，即 x 轴值
            Object nameObj = row.get(xAxisField);
            item.put("name", nameObj == null ? "" : String.valueOf(nameObj));

            // value 字段
            List<Double> values = new ArrayList<>();
            for (String legend : legends) {
                Object val = row.get(legend);
                Double numVal = null;

                if (val instanceof Number) {
                    numVal = ((Number) val).doubleValue();
                } else if (val instanceof String) {
                    try {
                        numVal = Double.parseDouble(((String) val).trim());
                    } catch (Exception e) {
                        numVal = null;
                    }
                } else {
                    numVal = null;
                }

                values.add(numVal);
            }

            item.put("value", values);
            resultValues.add(item);
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("legend", legends);
        output.put("valueList", resultValues);

        // 更新 table_data 的 value
        tableDataParam.put("value", output);
    }

    /**
     * 处理 chart_equity_penetration 类型的数据
     * legend 正常提取，valueList 直接复制 table_data 中的原始值
     */
    private void processEquityPenetrationData(JSONObject tableDataParam, Object valueObj) {
        // 转换为 List<Map<String, Object>>
        List<Map<String, Object>> rawList = null;
        if (valueObj instanceof List) {
            rawList = (List<Map<String, Object>>) valueObj;
        } else {
            // 其他类型不支持，设置为空输出
            setEmptyOutput(tableDataParam);
            return;
        }

        // 安全判断：value 为 null 或空，直接输出空结构
        if (rawList == null || rawList.isEmpty()) {
            setEmptyOutput(tableDataParam);
            return;
        }

        // 获取第一个对象并校验是否为空对象
        Map<String, Object> firstRow = rawList.getFirst();
        if (firstRow == null || firstRow.isEmpty()) {
            setEmptyOutput(tableDataParam);
            return;
        }

        // 用 LinkedHashMap 保持字段顺序，提取所有字段作为 legend
        LinkedHashMap<String, Object> orderedRow = new LinkedHashMap<>(firstRow);
        List<String> legends = new ArrayList<>(orderedRow.keySet());

        // 直接复制原始数据作为 valueList
        List<Map<String, Object>> valueList = new ArrayList<>(rawList);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("legend", legends);
        output.put("valueList", valueList);

        // 更新 table_data 的 value
        tableDataParam.put("value", output);
    }

    /**
     * 设置空输出
     */
    private void setEmptyOutput(JSONObject tableDataParam) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("legend", Collections.emptyList());
        output.put("valueList", Collections.emptyList());
        tableDataParam.put("value", output);
    }
}
