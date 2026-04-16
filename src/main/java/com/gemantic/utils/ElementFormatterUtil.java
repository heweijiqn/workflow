package com.gemantic.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.gemantic.gpt.client.CmsNodeServiceClient;
import com.gemantic.gpt.support.workflow.EquityPenetrationImageParam;
import com.gemantic.gpt.support.workflow.MarkDownToWordParam;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author 冉龙柯
 * @version 1.0
 * @description: TODO
 * @date 2024-10-17 18:33
 */

enum ElementType {
    TEXT("text"),
    IMAGE("image"),
    TABLE("table"),
    HYPERLINK("hyperlink"),
    SUPERSCRIPT("superscript"),
    SUBSCRIPT("subscript"),
    SEPARATOR("separator"),
    PAGE_BREAK("pageBreak"),
    CONTROL("control"),
    CHECKBOX("checkbox"),
    RADIO("radio"),
    LATEX("latex"),
    TAB("tab"),
    DATE("date"),
    BLOCK("block"),
    TITLE("title"),
    LIST("list"),
    CHART("chart");

    private final String type;

    ElementType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}

@Slf4j
public class ElementFormatterUtil {
    private static final List<String> TEXTLIKE_ELEMENT_TYPE = Lists.newArrayList(
            ElementType.TEXT.getType(),
            ElementType.HYPERLINK.getType(),
            ElementType.SUBSCRIPT.getType(),
            ElementType.SUPERSCRIPT.getType(),
            ElementType.CONTROL.getType(),
            ElementType.DATE.getType()
    );

    // 正则表达式模式，用于匹配 "{{key#value}}" 格式的字符串
    // private static final Pattern PATTERN = Pattern.compile("\\{\\{(.*?)#(.*?)#(.*?)#(.*?)\\}\\}");
    private static final Pattern PATTERN = Pattern.compile("\\{\\{(.*?)#(.*?)(?:#(.*?)(?:#(.*?))?)?\\}\\}");

    // Q三兄弟需要markdown转json的节点类型
    private static final List<String> MARKDOWN_TO_JSON_Q = ListUtil.of("q_db","q_data","q_doc");

    public static JSONArray formatElementNonTableList(JSONArray elementList, JSONObject parameters) {
        for (int i = 0; i < elementList.size(); i++){
            JSONObject el = elementList.getJSONObject(i);
            if (StrUtil.equals(el.getString("type"), ElementType.TITLE.getType()) || StrUtil.equals(el.getString("type"), ElementType.LIST.getType())) {
                JSONArray valueList = Optional.of(el.getJSONArray("valueList")).orElse(new JSONArray());
                formatElementNonTableList(valueList, parameters);
            } else if (StrUtil.equals(el.getString("type"), ElementType.TABLE.getType())) {
                continue;
                // todo 跳过表格类型，后面有需要再添加
/*                if (el.getJSONArray("trList") != null) {
                    for (Object tr : el.getJSONArray("trList")) {
                        if (tr instanceof JSONObject) {
                            JSONArray tdArray = ((JSONObject) tr).getJSONArray("tdList");
                            for (Object tdObj : tdArray) {
                                if (tdObj instanceof JSONObject) {
                                    JSONObject td = (JSONObject) tdObj;
                                    JSONArray valueArray = td.getJSONArray("value");
                                    formatElementNonTableList(valueArray);
                                }
                            }
                        }
                    }
                }*/
            } else if (StrUtil.equals(el.getString("type"), ElementType.HYPERLINK.getType()) || StrUtil.equals(el.getString("type"), ElementType.DATE.getType())) {
                JSONArray valueList = Optional.of(el.getJSONArray("valueList")).orElse(new JSONArray());
                formatElementNonTableList(valueList, parameters);
            }

            String isTemplate = el.getString("isTemplate");
            String type = el.getString("type");

            if (StrUtil.equals(type, ElementType.IMAGE.getType())) {
                String groupIds = "";
                JSONObject attrs = el.getJSONObject("attrs");
                if (attrs != null) {
                    String imageValueRaw = attrs.getString("chartValue");
                    if (StrUtil.isBlank(imageValueRaw)) {
                        imageValueRaw = attrs.getString("src");
                    }
                    if (StrUtil.isNotBlank(imageValueRaw)) {
                        String key = imageValueRaw;
                        if (imageValueRaw.startsWith("{{") && imageValueRaw.endsWith("}}")) {
                            key = imageValueRaw.replaceFirst("^\\{\\{([^\\{].*[^\\}])\\}\\}$", "$1");
                        }
                        if (StrUtil.isNotBlank(key) && parameters.containsKey(key)) {
                            groupIds = Optional.ofNullable(parameters.getJSONObject(key))
                                    .map(obj -> obj.getString("source_workflow_run_result_id"))
                                    .orElse("");
                        }
                    }
                }
                String position = IdUtil.simpleUUID().replace("-", "");
                Pair<JSONArray, String> jsonArrayBooleanPair = otherTypeRenderData(el, parameters, groupIds, position);
                if (ObjectUtil.isNull(jsonArrayBooleanPair)) {
                    continue;
                }
                JSONArray newSubTemplates = jsonArrayBooleanPair.getLeft();
                String resultType = jsonArrayBooleanPair.getRight();

                if ("delete".equals(resultType)) {
                    elementList.remove(i);
                    i--;
                } else if(CollectionUtil.isNotEmpty(newSubTemplates)){
                    if(newSubTemplates.size() == 1){
                        elementList.set(i, newSubTemplates.getJSONObject(0));
                    }else {
                        elementList.remove(i);
                        elementList.addAll(i, newSubTemplates);
                        i = i + newSubTemplates.size() - 1;
                    }
                }
            } else if(StrUtil.isNotEmpty(isTemplate)){
//                String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(el.getString("value"))))
//                        .map(obj -> obj.getString("source_workflow_run_result_id"))
//                        .map(Object::toString)
//                        .orElse("");
                String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(el.getString("value"))))
                        .map(obj -> obj.getString("source_workflow_run_result_id"))
                        .orElse("");
                String position = IdUtil.simpleUUID().replace("-", "");
                Pair<JSONArray, String> jsonArrayBooleanPair = otherTypeRenderData(el, parameters, groupIds, position);
                if (ObjectUtil.isNull(jsonArrayBooleanPair)) {
                    continue;
                }
                JSONArray newSubTemplates = jsonArrayBooleanPair.getLeft();
                String resultType = jsonArrayBooleanPair.getRight();

                // 特殊处理：如果是 delete 标识，删除当前元素
                if ("delete".equals(resultType)) {
                    elementList.remove(i);
                    i--; // 调整索引，因为删除了元素
                } else if(CollectionUtil.isNotEmpty(newSubTemplates)){
/*                    if(StrUtil.equals(jsonArrayBooleanPair.getRight(), "other")){
                        for (Object newSubTemplate : newSubTemplates) {
                            JSONObject newSubTemplateJson = (JSONObject) newSubTemplate;
                            newSubTemplateJson.put("groupIds", JSONArray.of(groupIds));
                        }
                    }*/
                    if(newSubTemplates.size() == 1){
                        elementList.set(i, newSubTemplates.getJSONObject(0));
                    }else {
                        //删除原来的元素，并将新元素插入原来的位置
                        elementList.remove(i);
                        elementList.addAll(i, newSubTemplates);
                        i = i + newSubTemplates.size() - 1;
                    }
                }
            }
        }

        return elementList;
    }

    /**
     * 处理非表格类型的数据,左侧数据，右侧是节点类型、包含子工作流、循环子工作流和其他
     */
    public static Pair<JSONArray, String> otherTypeRenderData(JSONObject templateItem, JSONObject parameters, String groupIds, String position) {
        String isTemplate = templateItem.getString("isTemplate");
        // 处理复选框和单选框
        if(StrUtil.equals("checkbox",isTemplate) || StrUtil.equals("radio",isTemplate)){
            JSONObject checkboxJsonObj = formatCheckbox(templateItem, parameters, groupIds, position);
            return Pair.of(JSONArray.of(checkboxJsonObj), "other");
        }

        // 处理图表
        if (StrUtil.equals("chart", isTemplate)) {
            templateItem.remove("isTemplate");
            String chartValueRaw = templateItem.getString("chartValue");
            String key = chartValueRaw;
            if (StrUtil.isNotEmpty(chartValueRaw)) {
                key = chartValueRaw.replaceFirst("^\\{\\{([^\\{].*[^\\}])\\}\\}$", "$1");
            }
            JSONObject paramObj = parameters.getJSONObject(key); // 图表#输出
            if (paramObj != null && paramObj.containsKey("value")) {
                Object value = paramObj.get("value");
                templateItem.put("chartConfig",value);
                templateItem.put("position", position);
                if (paramObj.containsKey("source_workflow_run_result_id")){
                    templateItem.put("groupIds", JSONArray.of(paramObj.get("source_workflow_run_result_id")));
                }else{
                    templateItem.put("groupIds", JSONArray.of(groupIds));
                }

                if (value instanceof JSONObject) {
                    JSONObject valueObj = (JSONObject) value;
                    String type = valueObj.getString("type");

                    Object valueListObj = valueObj.get("valueList");
                    boolean shouldDelete = false;
                    
                    if (valueListObj == null || 
                        (valueListObj instanceof List && ((List<?>) valueListObj).isEmpty()) ||
                        (valueListObj instanceof JSONArray && ((JSONArray) valueListObj).isEmpty())) {
                        shouldDelete = true;
                    }

                    if (shouldDelete) {
                        return Pair.of(null, "delete");
                    }

                    if ("chart_equity_penetration".equals(type)) {
                        // 获取legend
                        Object legendObj = valueObj.get("legend");

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> valueList = (List<Map<String, Object>>) valueListObj;

                        // 处理legend参数
                        List<String> legend = new ArrayList<>();
                        if (legendObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> legendList = (List<String>) legendObj;
                            legend = legendList;
                        }

                        // 调用股权穿透图接口
                        String imageUrl = equityPenetrationImage(valueList, legend);
                        if (StrUtil.isNotEmpty(imageUrl)) {
                            templateItem.put("value", imageUrl);
                        }else{
                            return Pair.of(null, "delete"); // 使用特殊标识表示需要删除
                        }
                    }
                }

                return Pair.of(JSONArray.of(templateItem), "other");
            } else {
                return Pair.of(null, "delete");
            }
        }

        String elementType = templateItem.getString("type");

        if (StrUtil.equals(elementType, "image")) {
            JSONObject attrs = templateItem.getJSONObject("attrs");
            if (attrs == null) {
                attrs = new JSONObject();
                templateItem.put("attrs", attrs);
            }

            String imageValueRaw = attrs.getString("chartValue");
            if (StrUtil.isBlank(imageValueRaw)) {
                imageValueRaw = attrs.getString("src");
            }

            boolean isPlaceholder = false;
            String key = null;
            if (StrUtil.isNotBlank(imageValueRaw) && imageValueRaw.startsWith("{{") && imageValueRaw.endsWith("}}")) {
                isPlaceholder = true;
                key = imageValueRaw.replaceFirst("^\\{\\{([^\\{].*[^\\}])\\}\\}$", "$1");
            }

            if (!isPlaceholder || !parameters.containsKey(key)) {
                cleanImageAttrs(attrs);
                ensureImageAttrsFields(attrs);
                return Pair.of(JSONArray.of(templateItem), "other");
            }

            JSONObject paramObj = parameters.getJSONObject(key);
            if (paramObj != null && paramObj.containsKey("value")) {
                Object value = paramObj.get("value");

                templateItem.put("config", value);
                attrs.put("chartConfig", value);

                if (!fillImageAttrs(attrs, value)) {
                    return Pair.of(null, "delete");
                }

                cleanImageAttrs(attrs);

                copyIfPresent(attrs, paramObj, "alt", "title", "width", "height", "id");
                if (!attrs.containsKey("src") && paramObj.containsKey("src")) {
                    attrs.put("src", paramObj.get("src"));
                }

                if (attrs.get("position") == null) {
                    attrs.put("position", position);
                }
                if (paramObj.containsKey("source_workflow_run_result_id")) {
                    attrs.put("sourceId", paramObj.get("source_workflow_run_result_id"));
                } else if (StrUtil.isNotEmpty(groupIds)) {
                    attrs.put("sourceId", groupIds);
                }

                ensureImageAttrsFields(attrs);

                return Pair.of(JSONArray.of(templateItem), "other");
            } else {
                cleanImageAttrs(attrs);
                ensureImageAttrsFields(attrs);
                return Pair.of(JSONArray.of(templateItem), "other");
            }
        }

        // 处理文本
        if (StrUtil.equals("text", isTemplate)) {
            templateItem.remove("isTemplate");
            String valueName = templateItem.getString ("value");
            NodeData nodeData = getValueByVariableName(parameters, valueName);
            if (nodeData == null || ObjectUtil.isEmpty(nodeData.getValue())) {
                templateItem.put("value", "");
                return Pair.of(JSONArray.of(templateItem), "other");
            }
            // log.info(">>>>>处理数据，节点类型为：{}", nodeData.getType());
            //判断是直接替换还是需要markdown转Word处理
            // todo 调用永奂方法丢失source_node_handle，等待永奂修改后再还原
            // if(MARKDOWN_TO_JSON_Q.contains(nodeData.getType()) && StrUtil.equals(nodeData.getSourceNodeHandle(), "output_content") ||
            //         StrUtil.equals("simple_prompt", nodeData.getType()) && StrUtil.equals(nodeData.getSourceNodeHandle(), "output")){
            if(MARKDOWN_TO_JSON_Q.contains(nodeData.getType()) && StrUtil.equals(nodeData.getValueType(), "str") ||
                    StrUtil.equals("simple_prompt", nodeData.getType()) && StrUtil.equals(nodeData.getValueType(), "str")){
                nodeData.setSize(templateItem.getString("size"));
                nodeData.setColor(templateItem.getString("color"));
                nodeData.setFont(templateItem.getString("font"));
                // 获取UUID取调中划线
                nodeData.setPosition(position);
                nodeData.setGroupIds(groupIds);
                return Pair.of(markDownToWord(nodeData), "other");
            }else if(StrUtil.equals("sub_workflow", nodeData.getType())){
                // JSONObject value = JSONObject.parseObject(nodeData.getValue().toString());
                JSONObject value = JsonUtils.parseJsonObjectWithArm(JsonUtils.toJSONStringWithArm(nodeData.getValue()));
                Object eval = JSONPath.eval(value, "$.docOutput.data.main");
                if(ObjectUtil.isNotNull(eval) && eval instanceof JSONArray){
                    return Pair.of((JSONArray) eval, "sub_workflow");
                }
            } else if (StrUtil.equals("sub_workflow_list", nodeData.getType())) {
                // JSONArray value = JSONArray.parseArray(nodeData.getValue().toString());
                // 优化：直接进行类型转换，避免不必要的序列化/反序列化
                JSONArray value;
                Object nodeValue = nodeData.getValue();
                if (nodeValue instanceof JSONArray) {
                    value = (JSONArray) nodeValue;
                } else if (nodeValue instanceof String) {
                    // 只有当值是字符串时才进行解析
                    value = JsonUtils.parseArrayWithArm((String) nodeValue);
                } else {
                    // 其他类型尝试转换为JSONArray
                    value = JsonUtils.parseArrayWithArm(JsonUtils.toJSONStringWithArm(nodeValue));
                }
                JSONArray result = value.stream().filter(obj -> {
                    Object eval = JSONPath.eval(obj, "$.docOutput.data.main");
                    return ObjectUtil.isNotNull(eval) && eval instanceof JSONArray;
                }).flatMap(obj -> {
                    Object eval = JSONPath.eval(obj, "$.docOutput.data.main");
                    JSONArray array = (JSONArray) eval;
                    // Add {"value":"\n"} at the end of each eval
                    array.add(new JSONObject().fluentPut("value", "\n"));
                    return array.stream();
                }).collect(Collectors.toCollection(JSONArray::new));
                // log.info(">>>>>>处理循环子工作流，处理结果为：{}",result);
                return Pair.of(result, "sub_workflow_list");
            } else if (valueName != null && valueName.startsWith("{{开始输入#")) {
                // 如果是开始输入，则代表是变量，则无需填充溯源信息
                templateItem.put("value", nodeData.getValue() == null ? "" : JsonUtils.toJSONStringWithArm(nodeData.getValue()));
                templateItem.remove("position");
                templateItem.remove("groupIds");
                return Pair.of(JSONArray.of(templateItem), "other");
            }else{
                templateItem.put("value", nodeData.getValue() == null ? "" : JsonUtils.toJSONStringWithArm(nodeData.getValue()));
                if (ObjectUtil.isNotEmpty(nodeData.getValue())) {
                    templateItem.put("position", position);
                    templateItem.put("groupIds", JSONArray.of(groupIds));
                }
                return Pair.of(JSONArray.of(templateItem), "other");
            }
        }

        return null;
    }





    public static JSONObject formatCheckbox(JSONObject templateItem, JSONObject parameters, String groupIds, String position) {
        // log.info(">>>>>处理复选框和单选框，templateItem为：{}", templateItem);
        String isTemplate = templateItem.getString("isTemplate");
        // 处理复选框和单选框
        if(StrUtil.equals("checkbox",isTemplate) || StrUtil.equals("radio",isTemplate)){
            NodeData etlValue = getValueByVariableName(parameters, templateItem.getString("value"));
            if (etlValue == null) {
                return templateItem;
            }
            if(StrUtil.equals("checkbox",isTemplate) && ObjectUtil.equals(etlValue.getValue(), true)){
                // templateItem = JSONObject.parseObject("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":true}}");
                templateItem = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":true}}");
            } else if (StrUtil.equals("checkbox", isTemplate) && ObjectUtil.notEqual(etlValue.getValue(), true)) {
                // templateItem = JSONObject.parseObject("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":false}}");
                templateItem = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":false}}");
            }
            if (StrUtil.equals("radio",isTemplate) && ObjectUtil.equals(etlValue.getValue(), true)) {
                // templateItem = JSONObject.parseObject("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"radio\":{\"value\":true}}");
                templateItem = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"radio\":{\"value\":true}}");
            } else if (StrUtil.equals("radio",isTemplate) && ObjectUtil.notEqual(etlValue.getValue(), true)) {
                // templateItem = JSONObject.parseObject("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"radio\":{\"value\":false}}");
                templateItem = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"radio\":{\"value\":false}}");
            }
            if (ObjectUtil.isNotEmpty(etlValue.getValue())) {
                templateItem.put("groupIds", JSONArray.of(groupIds));
                templateItem.put("position", position);
            }
        }
        return templateItem;
    }

    /**
     * @description: 根据etl变量名称获取值
     * @param parameters imput参数
     * @param etlVariableName etl变量名称
     * @return: Object
     * @author: 冉龙柯
     * @date: 2024-10-22 16:30
     */
    public static Object getValueByEtlName(JSONObject parameters, String etlVariableName) {
        if (parameters == null || parameters.isEmpty() || StrUtil.isEmpty(etlVariableName)) {
            return null;
        }

        try {
            Matcher matcher = PATTERN.matcher(etlVariableName);
            String loopKey = null;
            String jsonPath = null;
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                loopKey = key + "#" + value;

                int index = Integer.parseInt(matcher.group(3).substring(1)) - 1;
                String field = matcher.group(4);
                jsonPath = String.format("$[%d].%s", index, field);
            }
            if(StrUtil.isEmpty(loopKey) || StrUtil.isEmpty(jsonPath)){
                return null;
            }
            JSONArray parameterValue = parameters.getJSONObject(loopKey).getJSONArray("value");
            return JSONPath.eval(parameterValue, jsonPath);
        } catch (NumberFormatException e) {
            log.error(">>>>>getValueByEtlName方法报错", e);
            return null;
        }
    }



    /**
     * @description: 根据变量名获取名称获取值
     * @param parameters imput参数
     * @param variableName 变量名称
     * @return: Object
     * @author: 冉龙柯
     * @date: 2024-10-22 16:30
     */
    public static NodeData getValueByVariableName(JSONObject parameters, String variableName) {
        if (parameters == null || parameters.isEmpty() || StrUtil.isEmpty(variableName)) {
            return null;
        }
        NodeData nodeData = new NodeData();
        try {
            Matcher matcher = PATTERN.matcher(variableName);
            String loopKey = null;
            String jsonPath = null;
            String valueName = null;
            while (matcher.find()) {
                String key = matcher.group(1);
                valueName = matcher.group(2);
                loopKey = key + "#" + valueName;
                if(StrUtil.isNotEmpty(matcher.group(3))){
                    int index = Integer.parseInt(matcher.group(3).substring(1)) - 1;
                    String field = matcher.group(4);
                    jsonPath = String.format("$[%d].%s", index, field);
                }
            }
            // 如果是开始输入的变量，代表是变量名
            if(variableName.startsWith("{{开始输入#")){
                loopKey = valueName;
            }
            JSONObject parameter = parameters.getJSONObject(loopKey);
            // JSONObject parameter = parameters.getJSONObject("content") != null && parameters.getJSONObject(loopKey) ==null ? parameters.getJSONObject("content").getJSONObject(loopKey) : parameters.getJSONObject(loopKey);
            if(ObjectUtil.isNull(parameter)){
                return null;
            }
            // log.info(">>>>>getValueByVariableName方法，参数为：{}", parameter.toString());
            nodeData.setType(parameter.getString("node_type"));
            nodeData.setSourceNodeHandle(parameter.getString("source_node_handle"));
            nodeData.setValueType(parameter.getString("type"));
            if(StrUtil.isNotEmpty(loopKey) && StrUtil.isEmpty(jsonPath)){
                nodeData.setValue(parameter.getString("value"));
            }else if(StrUtil.isNotEmpty(loopKey) &&  StrUtil.isNotEmpty(jsonPath)){
                JSONArray parameterValue = parameter.getJSONArray("value");
                nodeData.setValue(JSONPath.eval(parameterValue, jsonPath.replace("-", ".")));
            }else{
                return null;
            }
            return nodeData;
        } catch (NumberFormatException e) {
            log.error(">>>>>getValueByEtlName方法报错，变量名为：{}，报错为",variableName, e);
            return null;
        }catch (Exception e){
            log.error(">>>>>getValueByEtlName方法报错，变量名为：{}，报错为",variableName, e);
            throw e;
        }
    }

    public static String getloopKeyByVariableName(String variableName) {
        try {
            Matcher matcher = PATTERN.matcher(variableName);
            String loopKey = null;
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                loopKey = key + "#" + value;
            }
            return loopKey;
        } catch (NumberFormatException e) {
            log.error(">>>>>getloopKeyByVariableName方法报错，变量名为：{}，报错为",variableName, e);
            return null;
        }catch (Exception e){
            log.error(">>>>>getloopKeyByVariableName方法报错，变量名为：{}，报错为",variableName, e);
            throw e;
        }
    }

    /**
     * 文本内容markDown格式转Wrod格式
     */
    public static JSONArray markDownToWord(NodeData nodeData) {
        MarkDownToWordParam markDownToWordParam = new MarkDownToWordParam();
        markDownToWordParam.setMarkdown(JsonUtils.toJSONStringWithArm(nodeData.getValue()));
        markDownToWordParam.setSize(nodeData.getSize());
        markDownToWordParam.setColor(nodeData.getColor());
        markDownToWordParam.setFont(nodeData.getFont());
        if (StringUtils.isNotBlank(JsonUtils.toJSONStringWithArm(nodeData.getValue()))) {
            markDownToWordParam.setGroupIds(nodeData.getGroupIds());
            markDownToWordParam.setPosition(nodeData.getPosition());
        }
        // log.info(">>>>>文本内容markDown格式转Wrod格式，调用参数为：{}", markDownToWordParam);
        JSONObject wordResult = SpringUtil.getBean(CmsNodeServiceClient.class).markdownToWord(markDownToWordParam);
        // log.info(">>>>>文本内容markDown格式转Wrod格式，调用结果为：{}", wordResult);
        if (ObjectUtil.isNull(wordResult) || wordResult.getIntValue("code") != 0) {
            log.error(">>>>>文本内容markDown格式转Wrod格式，调用失败，失败原因：{}", wordResult.getString("message"));
            return null;
        }
        return wordResult.getJSONObject("data").getJSONArray("result");
    }

    /**
     * 生成股权穿透图
     *
     * @param valueList 数据列表
     * @param legend 图例列表
     * @return 返回图片URL字符串，失败时返回null
     */
    public static String equityPenetrationImage(List<Map<String, Object>> valueList, List<String> legend) {
        try {
            EquityPenetrationImageParam imageParam = new EquityPenetrationImageParam();
            imageParam.setValueList(valueList);
            imageParam.setLegend(legend);

//            log.info(">>>>>开始生成股权穿透图，调用参数为：{}", imageParam);
            JSONObject result = SpringUtil.getBean(CmsNodeServiceClient.class).equityPenetrationImage(imageParam);
            log.info(">>>>>股权穿透图生成完成，调用结果为：{}", result);
            if (ObjectUtil.isNull(result) || result.getIntValue("code") != 0) {
                log.error(">>>>>股权穿透图生成失败，失败原因：{}",
                        result != null ? result.getString("message") : "返回结果为空");
                return null;
            }

            String imageUrl = result.getString("data");
            log.info(">>>>>股权穿透图生成成功，图片URL：{}", imageUrl);
            return imageUrl;

        } catch (Exception e) {
            log.error(">>>>>股权穿透图生成异常：{}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 填充图片属性
     * @param attrs 图片属性对象
     * @param value 参数值
     * @return 是否填充成功
     */
    private static boolean fillImageAttrs(JSONObject attrs, Object value) {
        log.info(">>>>>填充图片属性，参数为：{}", value);
        try {
            if (value instanceof JSONObject) {
                JSONObject valueObj = (JSONObject) value;
                String type = valueObj.getString("type");

                if (StrUtil.equals("chart_equity_penetration", type)) {
                    Object valueListObj = valueObj.get("valueList");
                    Object legendObj = valueObj.get("legend");
                    if (valueListObj instanceof List && !((List<?>) valueListObj).isEmpty()) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> valueList = (List<Map<String, Object>>) valueListObj;
                        List<String> legend = new ArrayList<>();
                        if (legendObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> legendList = (List<String>) legendObj;
                            legend = legendList;
                        }
                        String imageUrl = equityPenetrationImage(valueList, legend);
                        if (StrUtil.isNotEmpty(imageUrl)) {
                            attrs.put("src", imageUrl);
                            attrs.put("config", value);
                            return true;
                        }
                        log.warn(">>>>>股权穿透图生成失败，将删除该图表元素");
                        return false;
                    }
                    log.warn(">>>>>股权穿透图数据为空，将删除该图表元素");
                    return false;
                }

                if (StrUtil.isNotBlank(type) && type.startsWith("chart_")) {
                    Object valueListObj = valueObj.get("valueList");
                    if (valueListObj == null || 
                        (valueListObj instanceof List && ((List<?>) valueListObj).isEmpty()) ||
                        (valueListObj instanceof JSONArray && ((JSONArray) valueListObj).isEmpty())) {
                        log.warn(">>>>>图表 valueList 为空，将删除该图表元素，类型: {}", type);
                        attrs.remove("src");
                        return false;
                    }
                    attrs.put("config", value);
                    attrs.put("chartConfig", value);
                    return true;
                }

                valueObj.forEach(attrs::put);
                if (!attrs.containsKey("config")) {
                    attrs.put("config", value);
                }
                return true;
            } else if (value instanceof String) {
                attrs.put("src", value);
                if (!attrs.containsKey("config")) {
                    attrs.put("config", value);
                }
                return StrUtil.isNotEmpty((String) value);
            } else if (value != null) {
                attrs.put("config", value);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error(">>>>>填充图片属性时发生异常，将删除该图表元素，错误：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从源对象复制指定键到目标对象（如果源对象中存在且不为null）
     * @param target 目标对象
     * @param source 源对象
     * @param keys 要复制的键
     */
    private static void copyIfPresent(JSONObject target, JSONObject source, String... keys) {
        log.info(">>>>>从源对象复制指定键到目标对象，目标对象为：{}，源对象为：{}，要复制的键为：{}", target, source, keys);
        if (target == null || source == null || keys == null) {
            log.error(">>>>>从源对象复制指定键到目标对象，目标对象为null或者源对象为null或者keys为null");
            return;
        }
        for (String key : keys) {
            if (source.containsKey(key) && source.get(key) != null) {
                target.put(key, source.get(key));
            }
        }
    }

    /**
     * 清理图片 attrs 中不应该有的字段（图表配置相关字段应该只在 chartConfig 中）
     * @param attrs 图片属性对象
     */
    private static void cleanImageAttrs(JSONObject attrs) {
        if (attrs == null) {
            return;
        }
        Set<String> allowedFields = new HashSet<>(Arrays.asList(
                "id", "src", "alt", "title", "width", "height",
                "chartConfig", "position", "sourceId",
                "align", "data-placeholder-id", "diffStatus"
        ));

        List<String> keysToRemove = new ArrayList<>();
        for (String key : attrs.keySet()) {
            if (!allowedFields.contains(key)) {
                keysToRemove.add(key);
            }
        }
        for (String key : keysToRemove) {
            attrs.remove(key);
        }
    }

    /**
     * 确保图片 attrs 中包含所有必需字段（即使为 null）
     * @param attrs 图片属性对象
     */
    private static void ensureImageAttrsFields(JSONObject attrs) {
        log.info(">>>>>确保图片 attrs 中包含所有必需字段（即使为 null），attrs为：{}", attrs);
        if (attrs == null) {
            log.error(">>>>>确保图片 attrs 中包含所有必需字段（即使为 null），attrs为null");
            return;
        }
        if (!attrs.containsKey("align")) {
            attrs.put("align", null);
        }
        if (!attrs.containsKey("chartConfig")) {
            attrs.put("chartConfig", null);
        }
        if (!attrs.containsKey("data-placeholder-id")) {
            attrs.put("data-placeholder-id", null);
        }
        if (!attrs.containsKey("diffStatus")) {
            attrs.put("diffStatus", null);
        }
        if (!attrs.containsKey("position")) {
            attrs.put("position", null);
        }
        if (!attrs.containsKey("sourceId")) {
            attrs.put("sourceId", null);
        }
        log.info(">>>>>确保字段后，attrs为：{}", attrs);
    }

    /**
     * 替换模板中
     * @param jsonObject
     */
    public static void processReplaceTemplate(JSONObject jsonObject, String valuePattern, String replacement) {
        if (StringUtils.isBlank(valuePattern)) {
            valuePattern = "\\{\\{(.*?)#(.*?)#(.*?)#(.*?)\\}\\}";
        }
        if (StringUtils.isBlank(replacement)) {
            replacement = "";
        }
        // 遍历所有键值对
        for (String key : jsonObject.keySet()) {

            Object value = jsonObject.get(key);
            if (value instanceof String) {
                if (!"value".equals(key)) {
                    continue;
                }
                // 检查字符串是否匹配特定模式
                String strValue = (String) value;
                if (strValue.matches(valuePattern)) {
                    jsonObject.put(key, replacement); // 替换为空字符串
                    log.warn("processReplaceTemplate {} replace {}", strValue, replacement);
                }
            } else if (value instanceof JSONArray) {
                // 如果值是JSONArray，递归处理每个元素
                JSONArray array = (JSONArray) value;
                for (int j = 0; j < array.size(); j++) {
                    Object arrayValue = array.get(j);
                    if (arrayValue instanceof JSONObject) {
                        processReplaceTemplate((JSONObject) arrayValue, valuePattern, replacement);
                    }
                }
            } else if (value instanceof JSONObject) {
                // 如果值是JSONObject，递归处理
                processReplaceTemplate((JSONObject) value, valuePattern, replacement);
            }
        }
    }


    @Data
    public class Element {
        ElementType type;
        String value;
        List<Element> valueList;
        List<TableRow> trList;
        // Constructor and getters/setters
    }

    @Data
    class TableRow {
        List<TableCell> tdList;

        // Constructor and getters/setters
    }

    @Data
    class TableCell {
        List<Element> value;

        // Constructor and getters/setters
    }

    //节点数据
    @Data
    public static class NodeData{
        // 节点value
        private Object value;
        // 节点类型
        private String type;
        // 数据类型
        private String valueType;
        // 数据来源类型
        private String sourceNodeHandle;
        // 溯源
        private String groupIds;
        // 颜色
        private String color;
        // 字体
        private String font;
        // 字体大小
        private String size;
        // 溯源ID
        private String position;
    }
}