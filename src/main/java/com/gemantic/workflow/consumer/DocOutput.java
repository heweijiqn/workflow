package com.gemantic.workflow.consumer;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gemantic.workflow.repository.TiptapDocumentProcessor;
import com.gemantic.workflow.repository.impl.TiptapDocumentProcessorImpl;
import com.google.common.base.MoreObjects;
import jakarta.annotation.Resource;

import java.util.concurrent.ExecutorService;

import com.gemantic.gpt.support.workflow.Node;
import com.gemantic.utils.JsonUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONWriter;
import com.gemantic.gpt.client.CmsNodeServiceClient;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.workflow.MarkDownToWordParam;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.utils.ElementFormatterUtil;
import com.google.common.collect.Lists;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import static com.gemantic.utils.ElementFormatterUtil.getValueByVariableName;
import static com.gemantic.utils.ElementFormatterUtil.getloopKeyByVariableName;
import static com.gemantic.utils.ElementFormatterUtil.processReplaceTemplate;

/**
 * 文档输出
 */
@Slf4j
@Component("docOutput")
public class DocOutput extends BaseConsumer {

    @Resource
    private CmsNodeServiceClient cmsNodeServiceClient;
    @Resource
    private TiptapDocumentProcessor tiptapDocumentProcessor;

//    @Resource(name = "docOutputExecutorService")
//    private ExecutorService docOutputExecutorService;
//
//    @Override
//    protected ExecutorService getExecutorService() {
//        return docOutputExecutorService != null ? docOutputExecutorService : super.getExecutorService();
//    }

    @Value("${CONSUMER_DOC_OUTPUT_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize(){
        return MoreObjects.firstNonNull(consumerBatchSize,super.getConsumerBatchSize());
    }

    private static final String VALUE_PATTERN = "\\{\\{(.*?)#(.*?)#(.*?)#(.*?)\\}\\}";
    // 模块变量，如 Doc 组件、大模型组件，输出变量为 普通提示#输出 格式
    private static final String MODULE_VALUE_PATTERN_STR = "\\{\\{(.*?)#(.*?)\\}\\}";

    // 正则表达式模式，用于匹配 "{{key#value}}" 格式的字符串
    private static final Pattern PATTERN = Pattern.compile(VALUE_PATTERN);
    private static final Pattern MODULE_VALUE_PATTERN = Pattern.compile(MODULE_VALUE_PATTERN_STR);
    private static final Pattern REPLACE_PATTERN = Pattern.compile("\"value\":\\{\\{(.*?)#(.*?)#(.*?)#(.*?)\\}\\}");
    private static final Pattern START_INPUT_PATTERN = Pattern.compile("\\{\\{开始输入#(.*?)\\}\\}");
    private static final Pattern START_INPUT_VALUE_PATTERN = Pattern.compile("\"value\":\\{\\{开始输入#(.*?)\\}\\}");

    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {
        LOG.info("执行docOutput组件 workflowRunResult 【{}】", JSONObject.toJSONString(workflowRunResult,
                JSONWriter.Feature.WriteNulls, JSONWriter.Feature.LargeObject, JSONWriter.Feature.WriteBigDecimalAsPlain));
        workflowRunResult.setIgnoreError(1);
        JSONObject inputs = workflowRunResult.getInputs();
        LOG.info("主要参数inputs是{}", inputs);
        JSONObject originalInputs = WorkflowUtil.getInputsByTemplate(inputs);
        if (MapUtils.isNotEmpty(workflowRunResult.getData()) && CollectionUtils.isNotEmpty(workflowRunResult.getData().getJSONArray(WorkflowJSONKey.nodesHandle.name()))) {
            originalInputs = WorkflowUtil.getInputsByTemplate(workflowRunResult.getData().getList(WorkflowJSONKey.nodesHandle.name(), Node.class).getFirst().getData().getTemplate());
        }
        LOG.warn(">>>>>>DocOutput  JSON.copy 前的inputs{}", JsonUtils.toJSONStringWithArm(inputs));
        //上游节点的所有输出结果和文档输出组件的定义变量合并后的参数
        JSONObject parameters = DataParametersUtil.mergeParameters(originalInputs, inputs);
        LOG.warn(">>>>>>DocOutput  JSON.copy 后的parameters{}",  JsonUtils.toJSONStringWithArm(parameters));
        // 重要内容：文档呈现组件不能在界面上 debug 操作，因为不能拿到正常的结果，需要在最外层运行整个工作流。
        // template 为 json 模板
        JSONObject template = parameters.getJSONObject("content").getJSONObject("value");
        // 判断文档呈现类型是不是doc(新版本)
        // LOG.info("template init {}", template);
        // JSONObject outTemplate = JSONObject.parseObject(template.toString());
        // 判断文档呈现类型是不是doc(新版本)
        JSONObject dataObj = template.getJSONObject("data");
        if (dataObj != null && StrUtil.equals(dataObj.getString("type"), "doc")) {
            // 按照新版本的逻辑处理
            List<JSONObject> process = tiptapDocumentProcessor.process(parameters);
            // 用处理后的结果替换原始 content
            if (process != null && !process.isEmpty()) {
                dataObj.put("content", new JSONArray(process));
            }
            // 处理 annotations 数组中的模版变量替换
            processAnnotationsTemplate(template, parameters);
            byte[] jsonTemplate = JSON.toJSONBytes(template, JSONWriter.Feature.WriteNulls, JSONWriter.Feature.LargeObject, JSONWriter.Feature.WriteBigDecimalAsPlain);
            parameters.put("docOutput", JSON.parseObject(jsonTemplate));
            // parameters.put("fileUrl", "http://10.0.0.22:10105/workflow-editor?workflowId="+workflowRunResult.getWorkflowId()+"&workflowRunId="+workflowRunResult.getWorkflowRunId());
            List<JSONObject> outputs = Lists.newArrayList();
            JSONObject output = WorkflowUtil.createOutput(WorkflowJSONKey.output.name(), WorkflowJSONKey.doc_output.name(), parameters);
            outputs.add(output);
            return Response.ok(outputs);
        }


        //生成doc/html需要的数据
        // 测试模版
        // String insertJsonTemplate = "insert-json-template.json";
        // String balanceDataContent = ResourceUtil.readUtf8Str(insertJsonTemplate);
        // JSONObject result = JSONObject.parseObject(balanceDataContent);
        // 解析 JSON 字符串 (template) + 渲染数据
        // 获取 "main" 数组
        // 解析 JSON 字符串 (template) + 渲染数据
        // 获取 main 数组（兼容旧版 data.main，新版 content）
        JSONArray mainArray = null;
        if (template.containsKey("data")
                && template.getJSONObject("data").containsKey("main")) {
            mainArray = template.getJSONObject("data").getJSONArray("main");

        }
        else if (template.containsKey("data")
                && template.getJSONObject("data").containsKey("content")) {
            mainArray = template.getJSONObject("data").getJSONArray("content");
        }
        else if (template.containsKey("content")) {
            mainArray = template.getJSONArray("content");
        }

        if (mainArray == null) {
            throw new RuntimeException("模板格式不正确：未找到 mainArray（data.main/data.content/content）");
        }


        // LOG.info("template mainArray {}", mainArray);

        // key 为 模版中配置的标签，value 为对应上游输入的真实数据
        Map<String, JSONObject> keyDataMap = new HashMap<>();
        // 遍历 "main" 数组查找 type 为 "table" 的对象
        for (int i = 0; i < mainArray.size(); i++) {
            JSONObject item = mainArray.getJSONObject(i);
            if ("table".equals(item.getString("type"))) {
                processTable(item, keyDataMap, parameters);
            } else if ((item.getJSONArray("valueList") != null && !item.getJSONArray("valueList").isEmpty())) {
                for (Object itemValue : item.getJSONArray("valueList")) {
                    processTable((JSONObject) itemValue, keyDataMap, parameters);
                }
            }
        }

        // 处理表格外其他类型的数据
        ElementFormatterUtil.formatElementNonTableList(mainArray, parameters);

        // 解析表达式
        List<RenderElement> renderElements = conditionParserComplex(mainArray);
        LOG.info("conditionParserComplex result: {}", renderElements);

        for (RenderElement renderElement : renderElements) {
            // 变量替换
            changeOfVariable(renderElement.getResult(), parameters);
            // 运行表达式
            List<String> renderResult = processExpression(renderElement.getResult());

            JSONArray jsonArray = new JSONArray();
            boolean lastLineBreak = false;
            for (String s : renderResult) {
                s = s.trim();
                if (s.contains("{") && s.contains("}")) {
                    s = s.replaceAll("}\n+", "}");
                    s = s.substring(1, s.length() - 1);
                    String[] split = s.split("}\\{");
                    for (int i = 0; i < split.length; i++) {
                        String sp = split[i].trim();
                        try {
                            sp = "{" + split[i] + "}";
                            // 修复JSON字符串中的转义字符问题
                            sp = fixJsonEscapeCharacters(sp);
                            JSONObject jsonObject = JsonUtils.parseJsonObjectWithArm(sp);
                            // 删除 isTemplate 和 isCondition 属性
                            if ("condition".equals(jsonObject.getString("isTemplate"))) {
                                jsonObject.remove("isTemplate");
                            }
                            if (jsonObject.containsKey("isCondition")) {
                                jsonObject.remove("isCondition");
                            }
                            // 避免误删表格单元格等结构化节点
                            if (jsonObject.containsKey("type")) {
                                jsonArray.add(jsonObject);
                                lastLineBreak = false;
                            } else if (isLineBreakNode(jsonObject)) {
                                // 控制换行数量，避免连续空行：如果是连续换行，则跳过（continue）不添加到结果中
                                if (lastLineBreak) {
                                    continue;  // 跳过：不添加到jsonArray
                                }
                                lastLineBreak = true;
                                jsonArray.add(jsonObject);
                            } else {
                                lastLineBreak = false;
                                jsonArray.add(jsonObject);
                            }
                        } catch (JSONException e) {
                            LOG.error("parseObject sp {} error : ", sp, e);
                        }
                    }
                } else {
                    if (StringUtils.isBlank(s)) {
                        continue;
                    }
                    JSONObject textObject = new JSONObject();
                    textObject.put("value", s);
                    textObject.put("type", "text");
                    if (isLineBreakNode(textObject)) {
                        if (lastLineBreak) {
                            continue;
                        }
                        lastLineBreak = true;
                        jsonArray.add(textObject);
                    } else {
                        lastLineBreak = false;
                        jsonArray.add(textObject);
                    }
                }

            }
            if (!jsonArray.isEmpty()) {
                LOG.info("element set text index {} content {} size {}", renderElement.getIndex(), jsonArray, jsonArray.size());
                if (jsonArray.size() == 1) {
                    mainArray.set(renderElement.getIndex(), jsonArray.getJSONObject(0));
                } else {
                    // mainArray.remove(renderElement.getIndex());
                    mainArray.addAll(renderElement.getIndex(), jsonArray);
                }
            }
        }
        for (RenderElement renderElement : renderElements) {
            LOG.info("prepareRemoveObjects result: {}", renderElement.getPrepareRemoveObjects());

            Collection<JSONObject> values = renderElement.getPrepareRemoveObjects().values();
            Iterator<Object> iterator = mainArray.iterator();
            while (iterator.hasNext()) {
                // Map.Entry<Integer, JSONObject> entry = iterator.next();
                Object next = iterator.next();
                if (next instanceof JSONObject && containsByReference(values, next)) {
                    iterator.remove(); // 使用迭代器的remove方法删除元素
                }

            }
        }

        // 移除所有未被替换的模版变量，并打印日志记录
        for (int i = 0; i < mainArray.size(); i++) {
            JSONObject jsonObject = mainArray.getJSONObject(i);
            if (jsonObject == null) {
                continue;
            }
            processReplaceTemplate(jsonObject, "\\{\\{(.*?)(#(.*?)){1,}\\}\\}", "");
        }

        // 压缩条件模板产生的连续换行，避免条件渲染产生多余空行
        // 收集所有条件块渲染的位置
        Set<Integer> conditionRenderIndices = new HashSet<>();
        for (RenderElement renderElement : renderElements) {
            conditionRenderIndices.add(renderElement.getIndex());
        }
        collapseConsecutiveLineBreaks(mainArray, conditionRenderIndices);


        // parameters.clear();
        parameters.remove("content");
        for (String key : parameters.keySet()) {
            Object o = parameters.get(key);
            if (o instanceof JSONObject) {
                JSONObject obj = (JSONObject) o;
                String nodeType = obj.getString("node_type");
                // source_node_name
                // source_node_type
                // node_type
                // source_workflow_run_result_id
                // sub_workflow_list
                // sub_workflow
                if (StringUtils.isNotEmpty(nodeType) && !("sub_workflow_list".equals(nodeType) || "sub_workflow".equals(nodeType))) {
                    String sourceNodeName = obj.getString("source_node_name");
                    String sourceWorkflowRunResultId = obj.getString("source_workflow_run_result_id");
                    String sourceNodeType = obj.getString("source_node_type");
                    obj.clear();
                    obj.put("source_node_name", sourceNodeName);
                    obj.put("source_workflow_run_result_id", sourceWorkflowRunResultId);
                    obj.put("source_node_type", sourceNodeType);
                    obj.put("node_type", nodeType);
                } else if (StringUtils.isNotEmpty(nodeType) && "sub_workflow_list".equals(nodeType)) {
                    JSONArray jsonArray = obj.getJSONArray("value");
                    if (jsonArray != null) {
                        for (Object object : jsonArray) {
                            if (object instanceof JSONObject) {
                                JSONObject docOutput = ((JSONObject) object).getJSONObject("docOutput");
                                if (docOutput != null) {
                                    docOutput.remove("options");
                                }
                            }
                        }
                    }
                } else if (StringUtils.isNotEmpty(nodeType) && "sub_workflow".equals(nodeType)) {
                    JSONObject jsonObject = obj.getJSONObject("value");
                    if (jsonObject != null) {
                        JSONObject docOutput = jsonObject.getJSONObject("docOutput");
                        if (docOutput != null) {
                            docOutput.remove("options");
                        }
                    }
                }
            }
        }
        // 去除 \n 节点，在测试的时候，发现生成的文档中换行太多
        // String jsonTemplate = JSON.toJSONString(template, JSONWriter.Feature.WriteNulls, JSONWriter.Feature.LargeObject);
        // String jsonTemplate = new String(JSON.toJSONBytes(template, JSONWriter.Feature.WriteNulls, JSONWriter.Feature.LargeObject, JSONWriter.Feature.WriteBigDecimalAsPlain));
        byte[] jsonTemplate = JSON.toJSONBytes(template, JSONWriter.Feature.WriteNulls, JSONWriter.Feature.LargeObject, JSONWriter.Feature.WriteBigDecimalAsPlain);
        // jsonTemplate = jsonTemplate.replaceAll("\\{\\\"value\\\":\\\"\\\\n\\\"\\},", "");
        // jsonTemplate = jsonTemplate.replaceAll("(\\{\\\"value\\\":\\\"\\\\n\\\"\\},)+", "\\{\\\"value\\\":\\\"\\\\n\\\"\\},");
        // JSONArray fianlMainArray = template.getJSONObject("data").getJSONArray("main");
        // LOG.info("template fianlMainArray {}", fianlMainArray);
        // LOG.info("jsonTemplate result: {}", jsonTemplate);
        parameters.put("docOutput", JSON.parseObject(jsonTemplate));
        // parameters.put("fileUrl", "http://10.0.0.22:10105/workflow-editor?workflowId="+workflowRunResult.getWorkflowId()+"&workflowRunId="+workflowRunResult.getWorkflowRunId());
        List<JSONObject> outputs = Lists.newArrayList();
        JSONObject output = WorkflowUtil.createOutput(WorkflowJSONKey.output.name(), WorkflowJSONKey.doc_output.name(), parameters);
        outputs.add(output);
        return Response.ok(outputs);
    }

    private boolean isLineBreakNode(JSONObject jsonObject) {
        if (jsonObject == null) {
            return false;
        }
        String value = jsonObject.getString("value");
        return "\n".equals(value);
    }

    /**
     * 压缩条件模板产生的连续换行节点，避免条件渲染产生多余空行
     * 只压缩条件块渲染位置附近的连续换行符，保留普通文档中的换行符
     * 使用新建数组然后替换的方式，避免直接删除导致的索引变化问题
     *
     * @param mainArray 主数组
     * @param conditionRenderIndices 条件块渲染的位置索引集合
     */
    private void collapseConsecutiveLineBreaks(JSONArray mainArray, Set<Integer> conditionRenderIndices) {
        if (mainArray == null || mainArray.isEmpty() || conditionRenderIndices == null || conditionRenderIndices.isEmpty()) {
            return;
        }
        // 使用新建数组的方式，避免直接删除导致索引变化
        JSONArray cleaned = new JSONArray();
        boolean lastWasLineBreak = false;
        boolean lastWasInConditionArea = false;
        
        for (int i = 0; i < mainArray.size(); i++) {
            Object item = mainArray.get(i);
            if (!(item instanceof JSONObject)) {
                cleaned.add(item);
                lastWasLineBreak = false;
                lastWasInConditionArea = false;
                continue;
            }
            JSONObject obj = (JSONObject) item;
            
            // 如果节点有type属性，直接保留（保护结构化节点如tableCell等，与插入节点时的逻辑一致）
            if (obj.containsKey("type")) {
                cleaned.add(obj);
                lastWasLineBreak = false;
                lastWasInConditionArea = false;
                continue;
            }
            
            // 检查当前节点是否在条件块渲染位置附近（前后一定范围内）
            boolean isInConditionArea = false;
            for (Integer conditionIndex : conditionRenderIndices) {
                // 检查是否在条件块渲染位置附近（前后10个节点范围内）
                if (Math.abs(i - conditionIndex) <= 10) {
                    isInConditionArea = true;
                    break;
                }
            }
            
            // 检查是否是换行符节点（只检查没有type属性的节点）
            boolean isLineBreak = isLineBreakNode(obj);
            
            if (isLineBreak && isInConditionArea) {
                // 只压缩条件块渲染位置附近的连续换行符
                if (!lastWasLineBreak || !lastWasInConditionArea) {
                    cleaned.add(obj);
                    lastWasLineBreak = true;
                    lastWasInConditionArea = true;
                }
                // 注意：如果 lastWasLineBreak 和 lastWasInConditionArea 都为 true，这里不添加到cleaned数组，相当于删除了连续换行
            } else {
                // 非换行符节点，或者不在条件块渲染位置附近的换行符，直接保留
                cleaned.add(obj);
                lastWasLineBreak = isLineBreak;
                lastWasInConditionArea = isInConditionArea;
            }
        }
        // 清空原数组并添加清理后的元素，避免索引变化问题
        mainArray.clear();
        mainArray.addAll(cleaned);
    }

    private void processTable(JSONObject item, Map<String, JSONObject> keyDataMap, JSONObject parameters) {
        JSONArray content = item.getJSONArray("content");
        if (content != null) {
            processNewFormatTable(item, keyDataMap, parameters);
            return;
        }
        JSONArray trList = item.getJSONArray("trList");
        if (trList == null) {
            return;
        }
        for (int j = 0; j < trList.size(); j++) {
            JSONObject tr = trList.getJSONObject(j);
            // tr 标签上有 "loop": true, 说明该行是循环操作
            // 当有 loop 时，并且如果有 startLoopIndex 和 endLoopIndex 时，为一行中连续的列进行循环，同行的其他表格为合并单元格
            String loop = tr.getString("loop");
            int startLoopIndex = tr.getIntValue("startLoopIndex", -1);
            int endLoopIndex = tr.getIntValue("endLoopIndex", -1);
            JSONArray tdList = tr.getJSONArray("tdList");
            if (StringUtils.isNotBlank(loop) && "true".equals(loop)) {
                // System.out.println("loop " + loop);
                // 记录位置，最后统一修改表格信息，避免同时遍历并修改表格行数
                // 找到这一行所有的模板标签的 Key，通过该 key 去 inputs 中获取数据对象
                String loopKey = "";
                for (int g = 0; g < tdList.size(); g++) {
                    JSONObject td = tdList.getJSONObject(g);
                    JSONArray ele = td.getJSONArray("value");
                    if (ele.isEmpty()) {
                        continue;
                    }
                    for (int i = 0; i < ele.size(); i++) {
                        JSONObject eleValue = ele.getJSONObject(i);
                        Matcher matcher = PATTERN.matcher(eleValue.getString("value"));
                        while (matcher.find()) {
                            String key = matcher.group(1);
                            String value = matcher.group(2);
                            // LOG.info(eleValue.getString("value") + " Found loop key-value pair: " + key + "#" + value);
                            loopKey = key + "#" + value;
                            keyDataMap.put(key + "#" + value, parameters.getJSONObject("content"));
                        }
                    }

                }
                if (StringUtils.isBlank(loopKey)) {
                    continue;
                }
                boolean isLastRow = j == trList.size() - 1;
                // trList.remove(tr);
                // LOG.info("loopkey: {}", loopKey);
                JSONObject value = parameters.getJSONObject(loopKey).getJSONObject("value");
                if (value != null) {
                    loopRowTableRenderData(parameters, trList, tr, value, isLastRow, j, startLoopIndex, endLoopIndex, item.getJSONArray("colgroup").size());
                } else {
                    loopRowTableRenderDataByJSONArray(parameters, trList, tr, parameters.getJSONObject(loopKey).getJSONArray("value"), isLastRow, j, startLoopIndex, endLoopIndex, item.getJSONArray("colgroup").size());
                }

                // 设置溯源信息
                item.put("groupIds", JSONArray.of(parameters.getJSONObject(loopKey).getString("source_workflow_run_result_id")));

            } else {
                // tr 标签上没有 "loop": true, 判断 td 是否有  "markInsert": true, 如果有，进行数据插入替换
                for (int g = 0; g < tdList.size(); g++) {
                    JSONObject td = tdList.getJSONObject(g);
                    String markInsert = td.getString("markInsert");
                    String rowMerged = td.getString("rowMerged");
                    td.remove("backgroundColor");
                    JSONArray ele = td.getJSONArray("value");
                    for (int k = 0; k < ele.size(); k++) {
                        JSONObject eleValue = ele.getJSONObject(k);
                        String isTemplate = eleValue.getString("isTemplate");
                        if(StrUtil.equals("checkbox", isTemplate) || StrUtil.equals("radio", isTemplate)){
                            // LOG.info("checkbox or radio {}", eleValue);
                            ElementFormatterUtil.NodeData value = getValueByVariableName(parameters, eleValue.getString("value"));
                            if (value == null) {
                                continue;
                            }
//                            String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(eleValue.getString("value"))))
//                                    .map(obj -> obj.getString("source_workflow_run_result_id"))
//                                    .map(Object::toString)
//                                    .orElse("");
                            String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(eleValue.getString("value"))))
                                    .map(obj -> obj.getString("source_workflow_run_result_id"))
                                    .orElse("");

                            if(StrUtil.equals("checkbox",isTemplate) && ObjectUtil.equals(value.getValue(), true)){
                                // eleValue = JSONObject.parseObject("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":true}}");
                                eleValue = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":true}}");
                            } else if (StrUtil.equals("checkbox", isTemplate) && ObjectUtil.notEqual(value.getValue(), true)) {
                                // eleValue = JSONObject.parseObject("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":false}}");
                                eleValue = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":false}}");
                            }
                            if (StrUtil.equals("radio",isTemplate) && ObjectUtil.equals(value.getValue(), true)) {
                                // eleValue = JSONObject.parseObject("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"radio\":{\"value\":true}}");
                                eleValue = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"radio\":{\"value\":true}}");
                            } else if (StrUtil.equals("radio",isTemplate) && ObjectUtil.notEqual(value.getValue(), true)) {
                                // eleValue = JSONObject.parseObject("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"radio\":{\"value\":false}}");
                                eleValue = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"radio\":{\"value\":false}}");
                            }

                            if (ObjectUtil.isNotEmpty(value.getValue())) {
                                eleValue.put("groupIds", JSONArray.of(groupIds));
                                eleValue.put("position", IdUtil.simpleUUID().replace("-", ""));
                            }
                            ele.set(k, eleValue);
                        }else if (StrUtil.equals("chart", isTemplate)) { // 处理图表
//                            String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(eleValue.getString("value"))))
//                                    .map(obj -> obj.getString("source_workflow_run_result_id"))
//                                    .map(Object::toString)
//                                    .orElse("");
                            String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(eleValue.getString("value"))))
                                    .map(obj -> obj.getString("source_workflow_run_result_id"))
                                    .orElse("");

                            eleValue.remove("isTemplate");

                            String chartValueRaw = eleValue.getString("chartValue");
                            String key = chartValueRaw;
                            if (StrUtil.isNotEmpty(chartValueRaw)) {
                                key = chartValueRaw.replaceFirst("^\\{\\{([^\\{].*[^\\}])\\}\\}$", "$1");
                            }
                            JSONObject paramObj = parameters.getJSONObject(key); // 图表#输出
                            if (paramObj != null && paramObj.containsKey("value")) {
                                Object value = paramObj.get("value");
                                eleValue.put("chartConfig",value);

                                if (paramObj.containsKey("source_workflow_run_result_id")){
                                    eleValue.put("groupIds", JSONArray.of(paramObj.get("source_workflow_run_result_id")));
                                }else{
                                    eleValue.put("groupIds", JSONArray.of(groupIds));
                                }

                                // 检查图表类型，根据 valueList 判断是否需要删除
                                if (value instanceof JSONObject) {
                                    JSONObject valueObj = (JSONObject) value;
                                    String type = valueObj.getString("type");

                                    // 检查 valueList 是否为空
                                    Object valueListObj = valueObj.get("valueList");
                                    boolean shouldDelete = false;

                                    if (valueListObj == null ||
                                            (valueListObj instanceof List && ((List<?>) valueListObj).isEmpty()) ||
                                            (valueListObj instanceof JSONArray && ((JSONArray) valueListObj).isEmpty())) {
                                        shouldDelete = true;
                                    }

                                    if (shouldDelete) {
                                        // 如果 valueList 为空，删除当前元素
                                        ele.remove(k);
                                        k--; // 调整索引，因为删除了元素
                                        continue;
                                    }

                                    // 处理股权穿透图类型
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
                                        String imageUrl = ElementFormatterUtil.equityPenetrationImage(valueList, legend);
                                        if (StrUtil.isNotEmpty(imageUrl)) {
                                            eleValue.put("value", imageUrl);
                                        } else {
                                            // 如果图片生成失败，删除当前元素
                                            ele.remove(k);
                                            k--; // 调整索引，因为删除了元素
                                        }
                                    }
                                }
                            } else {
                                // 如果没有值，删除当前元素
                                ele.remove(k);
                                k--; // 调整索引，因为删除了元素
                            }
                        }
                        eleValue.remove("isTemplate");
                    }

                    if ((StringUtils.isNotBlank(markInsert) && "true".equals(markInsert)) || (StringUtils.isNotBlank(rowMerged) && "true".equals(rowMerged))) {
                        td.put("markInsert", "false");
                        // td.remove("backgroundColor");
                        JSONArray eleArray = td.getJSONArray("value");
                        for (int k = 0; k < eleArray.size(); k++) {
                            JSONObject eleValue = eleArray.getJSONObject(k);
                            textRender(eleValue, parameters, "table");
                        }
                    }

                }

            }
        }
    }


    /**
     * 处理新格式的表格（有content数组的格式）
     * 新格式: {type: "table", attrs: {id}, content: [tableRow...]}
     */
    private void processNewFormatTable(JSONObject item, Map<String, JSONObject> keyDataMap, JSONObject parameters) {
        JSONArray content = item.getJSONArray("content");
        if (content == null) {
            return;
        }

        for (int j = 0; j < content.size(); j++) {
            JSONObject tableRow = content.getJSONObject(j);
            if (tableRow == null || !"tableRow".equals(tableRow.getString("type"))) {
                continue;
            }

            // 获取行的 attrs
            JSONObject rowAttrs = tableRow.getJSONObject("attrs");
            if (rowAttrs == null) {
                rowAttrs = new JSONObject();
                tableRow.put("attrs", rowAttrs);
            }

            // 处理 loop 属性
            String loop = rowAttrs.getString("loop");
            Object startLoopIndexObj = rowAttrs.get("startLoopIndex");
            Object endLoopIndexObj = rowAttrs.get("endLoopIndex");
            int startLoopIndex = (startLoopIndexObj instanceof Number) ? ((Number) startLoopIndexObj).intValue() : -1;
            int endLoopIndex = (endLoopIndexObj instanceof Number) ? ((Number) endLoopIndexObj).intValue() : -1;

            // 获取单元格列表
            JSONArray cellContent = tableRow.getJSONArray("content");
            if (cellContent == null) {
                continue;
            }

            if (StringUtils.isNotBlank(loop) && "true".equals(loop)) {
                // 处理循环行
                String loopKey = "";
                for (int g = 0; g < cellContent.size(); g++) {
                    JSONObject cell = cellContent.getJSONObject(g);
                    if (cell == null) {
                        continue;
                    }

                    // 只从循环单元格（inLoopRange: true）中查找循环变量，跳过 markInsert 单元格
                    JSONObject cellAttrs = cell.getJSONObject("attrs");
                    if (cellAttrs != null) {
                        String markInsert = cellAttrs.getString("markInsert");
                        if (StringUtils.isNotBlank(markInsert) && "true".equals(markInsert)) {
                            // 跳过 markInsert 单元格，不用于确定循环数据源
                            continue;
                        }
                    }

                    // 从单元格的 content 中查找模板变量
                    JSONArray cellContentArray = cell.getJSONArray("content");
                    if (cellContentArray != null) {
                        for (Object paraObj : cellContentArray) {
                            if (paraObj instanceof JSONObject) {
                                JSONObject paragraph = (JSONObject) paraObj;
                                if (!"paragraph".equals(paragraph. getString("type"))) {
                                    continue;
                                }

                                JSONArray paraContent = paragraph.getJSONArray("content");
                                if (paraContent != null) {
                                    for (int k = 0; k < paraContent.size(); k++) {
                                        JSONObject textNode = paraContent.getJSONObject(k);
                                        if (textNode != null) {
                                            String nodeType = textNode.getString("type");
                                            String value = null;

                                            // 新格式使用 text 字段，旧格式使用 value 字段
                                            if ("inlineTemplate".equals(nodeType)) {
                                                // 处理 inlineTemplate，查找内部的 text 节点
                                                JSONArray inlineContent = textNode.getJSONArray("content");
                                                if (inlineContent != null) {
                                                    for (int m = 0; m < inlineContent.size(); m++) {
                                                        JSONObject innerNode = inlineContent.getJSONObject(m);
                                                        if (innerNode != null && "text".equals(innerNode.getString("type"))) {
                                                            value = innerNode.getString("text");
                                                            break;
                                                        }
                                                    }
                                                }
                                            } else if ("text".equals(nodeType)) {
                                                value = textNode.getString("text");
                                            } else {
                                                // 兼容旧格式
                                                value = textNode.getString("value");
                                            }

                                            if (StringUtils.isNotBlank(value)) {
                                                Matcher matcher = PATTERN.matcher(value);
                                                while (matcher.find()) {
                                                    String key = matcher.group(1);
                                                    String valueKey = matcher.group(2);
                                                    loopKey = key + "#" + valueKey;
                                                    keyDataMap.put(loopKey, parameters.getJSONObject("content"));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (StringUtils.isBlank(loopKey)) {
                    continue;
                }

                boolean isLastRow = j == content.size() - 1;
                JSONObject loopKeyObj = parameters.getJSONObject(loopKey);
                if (loopKeyObj != null) {
                    JSONObject value = loopKeyObj.getJSONObject("value");
                    if (value != null) {
                        loopRowTableRenderDataNewFormat(parameters, content, tableRow, value, isLastRow, j, startLoopIndex, endLoopIndex, cellContent.size());
                    } else {
                        JSONArray valueArray = loopKeyObj.getJSONArray("value");
                        if (valueArray != null) {
                            loopRowTableRenderDataByJSONArrayNewFormat(parameters, content, tableRow, valueArray, isLastRow, j, startLoopIndex, endLoopIndex, cellContent.size());
                        }
                    }

                    // 设置溯源信息
                    item.put("sourceId", loopKeyObj.getString("source_workflow_run_result_id"));
                }
            } else {
                // 处理非循环行：处理 markInsert 和 rowMerged
                for (int g = 0; g < cellContent.size(); g++) {
                    JSONObject cell = cellContent.getJSONObject(g);
                    if (cell == null) {
                        continue;
                    }

                    JSONObject cellAttrs = cell.getJSONObject("attrs");
                    if (cellAttrs == null) {
                        cellAttrs = new JSONObject();
                        cell.put("attrs", cellAttrs);
                    }

                    String markInsert = cellAttrs.getString("markInsert");
                    String rowMerged = cellAttrs.getString("rowMerged");

                    // 处理单元格内容
                    JSONArray cellContentArray = cell.getJSONArray("content");
                    if (cellContentArray != null) {
                        for (int paraIdx = 0; paraIdx < cellContentArray.size(); paraIdx++) {
                            JSONObject paragraph = cellContentArray.getJSONObject(paraIdx);
                            if (paragraph == null) {
                                continue;
                            }
                            if (isImageNode(paragraph)) {
                                paraIdx = handleTableImageNode(cellContentArray, paraIdx, parameters);
                                continue;
                            }
                            if (!"paragraph".equals(paragraph.getString("type"))) {
                                continue;
                            }

                            JSONArray paraContent = paragraph.getJSONArray("content");
                            if (paraContent != null) {
                                for (int k = 0; k < paraContent.size(); k++) {
                                    JSONObject textNode = paraContent.getJSONObject(k);
                                    if (textNode != null) {
                                        if (isImageNode(textNode)) {
                                            k = handleTableImageNode(paraContent, k, parameters);
                                            continue;
                                        }
                                        String isTemplate = textNode.getString("isTemplate");

                                        // 处理 checkbox 和 radio
                                        if (StrUtil.equals("checkbox", isTemplate) || StrUtil.equals("radio", isTemplate)) {
                                            ElementFormatterUtil.NodeData value = getValueByVariableName(parameters, textNode.getString("value"));
                                            if (value == null) {
                                                continue;
                                            }

                                            String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(textNode.getString("value"))))
                                                    .map(obj -> obj.getString("source_workflow_run_result_id"))
                                                    .orElse("");

                                            if (StrUtil.equals("checkbox", isTemplate) && ObjectUtil.equals(value.getValue(), true)) {
                                                textNode = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":true}}");
                                            } else if (StrUtil.equals("checkbox", isTemplate) && ObjectUtil.notEqual(value.getValue(), true)) {
                                                textNode = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":false}}");
                                            }
                                            if (StrUtil.equals("radio", isTemplate) && ObjectUtil.equals(value.getValue(), true)) {
                                                textNode = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"radio\":{\"value\":true}}");
                                            } else if (StrUtil.equals("radio", isTemplate) && ObjectUtil.notEqual(value.getValue(), true)) {
                                                textNode = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"radio\":{\"value\":false}}");
                                            }

                                            if (ObjectUtil.isNotEmpty(value.getValue())) {
                                                textNode.put("sourceId", JSONArray.of(groupIds));
                                                textNode.put("position", IdUtil.simpleUUID().replace("-", ""));
                                            }
                                            paraContent.set(k, textNode);
                                        } else if (StrUtil.equals("chart", isTemplate)) {
                                            // 处理图表
                                            String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(textNode.getString("value"))))
                                                    .map(obj -> obj.getString("source_workflow_run_result_id"))
                                                    .orElse("");

                                            textNode.remove("isTemplate");

                                            String chartValueRaw = textNode.getString("chartValue");
                                            String key = chartValueRaw;
                                            if (StrUtil.isNotEmpty(chartValueRaw)) {
                                                key = chartValueRaw.replaceFirst("^\\{\\{([^\\{].*[^\\}])\\}\\}$", "$1");
                                            }
                                            JSONObject paramObj = parameters.getJSONObject(key);
                                            if (paramObj != null && paramObj.containsKey("value")) {
                                                Object value = paramObj.get("value");
                                                textNode.put("chartConfig", value);

                                                if (paramObj.containsKey("source_workflow_run_result_id")) {
                                                    textNode.put("sourceId", JSONArray.of(paramObj.get("source_workflow_run_result_id")));
                                                } else {
                                                    textNode.put("sourceId", JSONArray.of(groupIds));
                                                }

                                                // 检查图表类型，根据 valueList 判断是否需要删除
                                                if (value instanceof JSONObject) {
                                                    JSONObject valueObj = (JSONObject) value;
                                                    String type = valueObj.getString("type");

                                                    // 检查 valueList 是否为空
                                                    Object valueListObj = valueObj.get("valueList");
                                                    boolean shouldDelete = false;

                                                    if (valueListObj == null ||
                                                            (valueListObj instanceof List && ((List<?>) valueListObj).isEmpty()) ||
                                                            (valueListObj instanceof JSONArray && ((JSONArray) valueListObj).isEmpty())) {
                                                        shouldDelete = true;
                                                    }

                                                    if (shouldDelete) {
                                                        // 如果 valueList 为空，删除当前元素
                                                        paraContent.remove(k);
                                                        k--;
                                                        continue;
                                                    }

                                                    // 处理股权穿透图类型
                                                    if ("chart_equity_penetration".equals(type)) {
                                                        // 获取legend
                                                        Object legendObj = valueObj.get("legend");

                                                        @SuppressWarnings("unchecked")
                                                        List<Map<String, Object>> valueList = (List<Map<String, Object>>) valueListObj;

                                                        List<String> legend = new ArrayList<>();
                                                        if (legendObj instanceof List) {
                                                            @SuppressWarnings("unchecked")
                                                            List<String> legendList = (List<String>) legendObj;
                                                            legend = legendList;
                                                        }

                                                        String imageUrl = ElementFormatterUtil.equityPenetrationImage(valueList, legend);
                                                        if (StrUtil.isNotEmpty(imageUrl)) {
                                                            textNode.put("value", imageUrl);
                                                        } else {
                                                            paraContent.remove(k);
                                                            k--;
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        textNode.remove("isTemplate");

                                        // 如果 markInsert 或 rowMerged 为 true，进行文本渲染
                                        if ((StringUtils.isNotBlank(markInsert) && "true".equals(markInsert)) ||
                                                (StringUtils.isNotBlank(rowMerged) && "true".equals(rowMerged))) {
                                            cellAttrs.put("markInsert", null);
                                            // 使用新格式的文本渲染方法
                                            textRenderNewFormat(paraContent, k, parameters, "table");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 新格式表格行循环渲染（JSONObject数据）
     */
    private void loopRowTableRenderDataNewFormat(JSONObject parameters, JSONArray root, JSONObject tableRowTemplate,
                                                 JSONObject dataObject, boolean isLastRow, int rowIndex,
                                                 int startLoopIndex, int endLoopIndex, int cellCount) {

        JSONArray cellContent = tableRowTemplate.getJSONArray("content");
        if (cellContent == null) {
            return;
        }

        // 计算模板行中最大的 rowspan 值，用于确定需要移除的占位符行数量
        int placeholderRowCount = calculatePlaceholderRowCount(cellContent);
        int size = 0;
        Map<String, Object> cache = new HashMap<>();

        for (int g = 0; g < cellContent.size(); g++) {
            JSONObject cell = cellContent.getJSONObject(g);
            if (cell == null) {
                continue;
            }

            // 跳过 markInsert 单元格，不用于计算循环次数
            JSONObject cellAttrs = cell.getJSONObject("attrs");
            if (cellAttrs != null) {
                String markInsert = cellAttrs.getString("markInsert");
                if (StringUtils.isNotBlank(markInsert) && "true".equals(markInsert)) {
                    continue;
                }
            }

            JSONArray cellContentArray = cell.getJSONArray("content");
            if (cellContentArray == null) {
                continue;
            }

            for (Object paraObj : cellContentArray) {
                if (!(paraObj instanceof JSONObject)) {
                    continue;
                }
                JSONObject paragraph = (JSONObject) paraObj;
                if (!"paragraph".equals(paragraph.getString("type"))) {
                    continue;
                }

                JSONArray paraContent = paragraph.getJSONArray("content");
                if (paraContent == null) {
                    continue;
                }

                for (int i = 0; i < paraContent.size(); i++) {
                    JSONObject textNode = paraContent.getJSONObject(i);
                    if (textNode == null) {
                        continue;
                    }

                    String nodeType = textNode.getString("type");
                    String eleValueStr = null;


                    if ("inlineTemplate".equals(nodeType)) {
                        JSONArray inlineContent = textNode.getJSONArray("content");
                        if (inlineContent != null) {
                            for (int m = 0; m < inlineContent.size(); m++) {
                                JSONObject innerNode = inlineContent.getJSONObject(m);
                                if (innerNode != null && "text".equals(innerNode.getString("type"))) {
                                    eleValueStr = innerNode.getString("text");
                                    break;
                                }
                            }
                        }
                    } else if ("text".equals(nodeType)) {
                        eleValueStr = textNode.getString("text");
                    } else {
                        // 兼容旧格式
                        eleValueStr = textNode.getString("value");
                    }

                    if (StringUtils.isBlank(eleValueStr)) {
                        continue;
                    }

                    Matcher matcher = PATTERN.matcher(eleValueStr);
                    while (matcher.find()) {
                        String key = matcher.group(1);
                        String value = matcher.group(2);
                        String arrayIndexRef = matcher.group(3); // $1, $2, etc.
                        String fieldName = matcher.group(4); // 字段名，如 项目

                        // $1 表示使用参数中的数组，需要从 dataObject 中找到数组
                        // dataObject 是 JSONObject，通常数组在 value 字段中
                        String arrayPath = "$.value";
                        Object eval = null;

                        // 首先尝试 $.value 路径（最常见的情况）
                        eval = JSONPath.eval(JsonUtils.toJSONStringWithArm(dataObject), arrayPath);

                        // 如果 $.value 不是数组，尝试根路径 $
                        if (eval == null || !(eval instanceof JSONArray)) {
                            arrayPath = "$";
                            eval = JSONPath.eval(JsonUtils.toJSONStringWithArm(dataObject), arrayPath);
                            // 如果根路径的结果也不是数组，尝试查找 dataObject 中的第一个数组字段
                            if (eval == null || !(eval instanceof JSONArray)) {
                                // 遍历 dataObject 的字段，查找第一个 JSONArray
                                for (String field : dataObject.keySet()) {
                                    Object fieldValue = dataObject.get(field);
                                    if (fieldValue instanceof JSONArray) {
                                        arrayPath = "$." + field;
                                        eval = fieldValue;
                                        break;
                                    }
                                }
                            }
                        }
                        if (eval == null || !(eval instanceof JSONArray)) {
                            LOG.warn("Failed to find array in dataObject for template: {}, path: {}", eleValueStr, arrayPath);
                            continue;
                        }

                        JSONArray array = (JSONArray) eval;
                        if (size == 0) {
                            size = array.size();
                        }
                        // 使用 key#value 作为 cache key，确保同一个参数的数组只缓存一次
                        String cacheKey = String.format("%s#%s", key, value);
                        cache.putIfAbsent(cacheKey, array);
                    }

                    setGlobalVariable(textNode, parameters, "loopRowTableRenderDataNewFormat");
                }
            }
        }


        List<JSONObject> newRowList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            JSONObject newRow = JsonUtils.parseJsonObjectWithArm(JsonUtils.toJSONStringWithArm(tableRowTemplate));
            JSONObject newRowAttrs = newRow.getJSONObject("attrs");
            if (newRowAttrs == null) {
                newRowAttrs = new JSONObject();
                newRow.put("attrs", newRowAttrs);
            }
            newRowAttrs.put("loop", null);

            JSONArray newCellContent = newRow.getJSONArray("content");
            if (newCellContent != null) {
                // 记录需要移除的 markInsert 单元格索引（用于后续行）
                List<Integer> markInsertIndicesToRemove = new ArrayList<>();

                for (int g = 0; g < newCellContent.size(); g++) {
                    JSONObject cell = newCellContent.getJSONObject(g);
                    if (cell == null) {
                        continue;
                    }

                    JSONObject cellAttrs = cell.getJSONObject("attrs");
                    if (cellAttrs == null) {
                        cellAttrs = new JSONObject();
                        cell.put("attrs", cellAttrs);
                    }

                    String markInsert = cellAttrs.getString("markInsert");
                    boolean isMarkInsert = StringUtils.isNotBlank(markInsert) && "true".equals(markInsert);

                    JSONArray cellContentArray = cell.getJSONArray("content");
                    if (cellContentArray == null) {
                        continue;
                    }

                    // 如果是 markInsert 单元格，使用 textRenderNewFormat 处理（参考普通表格的处理方式）
                    if (isMarkInsert) {
                        if (i == 0) {
                            // 第一行：渲染内容并设置 rowspan
                            for (int paraIdx = 0; paraIdx < cellContentArray.size(); paraIdx++) {
                                JSONObject paragraph = cellContentArray.getJSONObject(paraIdx);
                                if (paragraph == null) {
                                    continue;
                                }
                                if (isImageNode(paragraph)) {
                                    paraIdx = handleTableImageNode(cellContentArray, paraIdx, parameters);
                                    continue;
                                }
                                if (!"paragraph".equals(paragraph.getString("type"))) {
                                    continue;
                                }

                                JSONArray paraContent = paragraph.getJSONArray("content");
                                if (paraContent != null) {
                                    for (int k = 0; k < paraContent.size(); k++) {
                                        JSONObject textNode = paraContent.getJSONObject(k);
                                        if (textNode != null) {
                                            if (isImageNode(textNode)) {
                                                k = handleTableImageNode(paraContent, k, parameters);
                                                continue;
                                            }
                                            // 处理 checkbox、radio、chart 等（参考普通表格的处理）
                                            String isTemplate = textNode.getString("isTemplate");

                                            if (StrUtil.equals("checkbox", isTemplate) || StrUtil.equals("radio", isTemplate)) {
                                                ElementFormatterUtil.NodeData value = getValueByVariableName(parameters, textNode.getString("value"));
                                                if (value == null) {
                                                    continue;
                                                }

                                                String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(textNode.getString("value"))))
                                                        .map(obj -> obj.getString("source_workflow_run_result_id"))
                                                        .orElse("");

                                                if (StrUtil.equals("checkbox", isTemplate) && ObjectUtil.equals(value.getValue(), true)) {
                                                    textNode = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":true}}");
                                                } else if (StrUtil.equals("checkbox", isTemplate) && ObjectUtil.notEqual(value.getValue(), true)) {
                                                    textNode = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":false}}");
                                                }
                                                if (StrUtil.equals("radio", isTemplate) && ObjectUtil.equals(value.getValue(), true)) {
                                                    textNode = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"radio\":{\"value\":true}}");
                                                } else if (StrUtil.equals("radio", isTemplate) && ObjectUtil.notEqual(value.getValue(), true)) {
                                                    textNode = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"radio\":{\"value\":false}}");
                                                }

                                                if (ObjectUtil.isNotEmpty(value.getValue())) {
                                                    textNode.put("sourceId", JSONArray.of(groupIds));
                                                    textNode.put("position", IdUtil.simpleUUID().replace("-", ""));
                                                }
                                                paraContent.set(k, textNode);
                                            } else if (StrUtil.equals("chart", isTemplate)) {
                                                String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(textNode.getString("value"))))
                                                        .map(obj -> obj.getString("source_workflow_run_result_id"))
                                                        .orElse("");

                                                textNode.remove("isTemplate");

                                                String chartValueRaw = textNode.getString("chartValue");
                                                String key = chartValueRaw;
                                                if (StrUtil.isNotEmpty(chartValueRaw)) {
                                                    key = chartValueRaw.replaceFirst("^\\{\\{([^\\{].*[^\\}])\\}\\}$", "$1");
                                                }
                                                JSONObject paramObj = parameters.getJSONObject(key);
                                                if (paramObj != null && paramObj.containsKey("value")) {
                                                    Object value = paramObj.get("value");
                                                    textNode.put("chartConfig", value);

                                                    if (paramObj.containsKey("source_workflow_run_result_id")) {
                                                        textNode.put("sourceId", JSONArray.of(paramObj.get("source_workflow_run_result_id")));
                                                    } else {
                                                        textNode.put("sourceId", JSONArray.of(groupIds));
                                                    }

                                                    if (value instanceof JSONObject) {
                                                        JSONObject valueObj = (JSONObject) value;
                                                        String type = valueObj.getString("type");

                                                        // 检查 valueList 是否为空
                                                        Object valueListObj = valueObj.get("valueList");
                                                        boolean shouldDelete = false;

                                                        if (valueListObj == null ||
                                                                (valueListObj instanceof List && ((List<?>) valueListObj).isEmpty()) ||
                                                                (valueListObj instanceof JSONArray && ((JSONArray) valueListObj).isEmpty())) {
                                                            shouldDelete = true;
                                                        }

                                                        if (shouldDelete) {
                                                            // 如果 valueList 为空，删除当前元素
                                                            paraContent.remove(k);
                                                            k--;
                                                            continue;
                                                        }

                                                        // 处理股权穿透图类型
                                                        if ("chart_equity_penetration".equals(type)) {
                                                            // 获取legend
                                                            Object legendObj = valueObj.get("legend");

                                                            @SuppressWarnings("unchecked")
                                                            List<Map<String, Object>> valueList = (List<Map<String, Object>>) valueListObj;

                                                            List<String> legend = new ArrayList<>();
                                                            if (legendObj instanceof List) {
                                                                @SuppressWarnings("unchecked")
                                                                List<String> legendList = (List<String>) legendObj;
                                                                legend = legendList;
                                                            }

                                                            String imageUrl = ElementFormatterUtil.equityPenetrationImage(valueList, legend);
                                                            if (StrUtil.isNotEmpty(imageUrl)) {
                                                                textNode.put("value", imageUrl);
                                                            } else {
                                                                paraContent.remove(k);
                                                                k--;
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                // 对于非 checkbox/radio/chart 的节点，使用 textRenderNewFormat 渲染
                                                // textRenderNewFormat 会处理 inlineTemplate 并移除 isTemplate
                                                textRenderNewFormat(paraContent, k, parameters, "table");
                                            }
                                        }
                                    }
                                }
                            }

                            // 设置 rowspan 并清除 markInsert 标记
                            // 如果模板中已经设置了 rowspan，需要将其扩展（使用加法而不是乘法）
                            // 公式：最终 rowspan = 模板 rowspan + 循环生成的额外行数
                            // 因为模板行会被第一个循环行替换，所以额外行数 = size - 1
                            Object templateRowspanObj = cellAttrs.get("rowspan");
                            int templateRowspan = 1;
                            if (templateRowspanObj instanceof Number) {
                                templateRowspan = ((Number) templateRowspanObj).intValue();
                            }
                            // 最终的 rowspan = 模板 rowspan + (循环次数 - 1)
                            cellAttrs.put("rowspan", templateRowspan + size - 1);
                            cellAttrs.put("markInsert", null);
                        } else {
                            // 后续行：记录需要移除的单元格索引
                            markInsertIndicesToRemove.add(g);
                        }
                        continue; // 跳过循环数据替换
                    }

                    // 检查单元格是否在循环范围内
                    Object inLoopRangeObj = cellAttrs.get("inLoopRange");
                    boolean inLoopRange = false;
                    if (inLoopRangeObj instanceof Boolean) {
                        inLoopRange = (Boolean) inLoopRangeObj;
                    } else if (inLoopRangeObj instanceof String) {
                        inLoopRange = "true".equals(inLoopRangeObj);
                    }

                    // 如果不在循环范围内，跳过循环处理，让 mergeRowspanNewFormat 处理
                    if (!inLoopRange) {
                        continue;
                    }

                    // 非 markInsert 单元格的正常循环处理（只在循环范围内的单元格）
                    for (int paraIdx = 0; paraIdx < cellContentArray.size(); paraIdx++) {
                        JSONObject paragraph = cellContentArray.getJSONObject(paraIdx);
                        if (paragraph == null) {
                            continue;
                        }
                        if (isImageNode(paragraph)) {
                            paraIdx = handleTableImageNode(cellContentArray, paraIdx, parameters);
                            continue;
                        }
                        if (!"paragraph".equals(paragraph.getString("type"))) {
                            continue;
                        }

                        JSONArray paraContent = paragraph.getJSONArray("content");
                        if (paraContent == null) {
                            continue;
                        }

                        for (int i1 = 0; i1 < paraContent.size(); i1++) {
                            JSONObject textNode = paraContent.getJSONObject(i1);
                            if (textNode == null) {
                                continue;
                            }
                            if (isImageNode(textNode)) {
                                i1 = handleTableImageNode(paraContent, i1, parameters);
                                continue;
                            }

                            textNode.remove("isTemplate");
                            String nodeType = textNode.getString("type");
                            String eleValueStr = null;
                            String textField = null;
                            JSONObject originalInlineTemplate = null;


                            JSONObject targetTextNode = null; // 用于存储实际要修改的 text 节点

                            if ("inlineTemplate".equals(nodeType)) {
                                originalInlineTemplate = textNode;
                                JSONArray inlineContent = textNode.getJSONArray("content");
                                if (inlineContent != null) {
                                    for (int m = 0; m < inlineContent.size(); m++) {
                                        JSONObject innerNode = inlineContent.getJSONObject(m);
                                        if (innerNode != null && "text".equals(innerNode.getString("type"))) {
                                            eleValueStr = innerNode.getString("text");
                                            textField = "text";
                                            targetTextNode = innerNode; // 保存引用，但不替换 textNode
                                            break;
                                        }
                                    }
                                }
                            } else if ("text".equals(nodeType)) {
                                eleValueStr = textNode.getString("text");
                                textField = "text";
                                targetTextNode = textNode;
                            } else {
                                // 兼容旧格式
                                eleValueStr = textNode.getString("value");
                                textField = "value";
                                targetTextNode = textNode;
                            }

                            if (StringUtils.isBlank(eleValueStr) || targetTextNode == null) {
                                continue;
                            }

                            Matcher matcher = PATTERN.matcher(eleValueStr);
                            LinkedHashSet<String> groupIds = new LinkedHashSet<>();
                            boolean hasReplacement = false;

                            while (matcher.find()) {
                                try {
                                    String key = matcher.group(1);
                                    String value = matcher.group(2);
                                    String arrayIndexRef = matcher.group(3); // $1, $2, etc.
                                    String fieldName = matcher.group(4); // 字段名，如 项目

                                    // 收集 groupIds（与新插入的处理方式一致）
                                    addGroupIdTable(parameters, groupIds, key, value);

                                    // 使用 key#value 作为 cache key 获取数组
                                    String cacheKey = String.format("%s#%s", key, value);
                                    Object o = cache.get(cacheKey);
                                    if (o == null) {
                                        LOG.warn("{} not found object data by cache key {}", eleValueStr, cacheKey);
                                        continue;
                                    }

                                    if (!(o instanceof JSONArray)) {
                                        LOG.warn("{} cache data is not JSONArray: {}", eleValueStr, cacheKey);
                                        continue;
                                    }

                                    JSONArray array = (JSONArray) o;
                                    if (i >= array.size()) {
                                        LOG.warn("{} array index {} out of bounds, array size: {}", eleValueStr, i, array.size());
                                        continue;
                                    }

                                    Object item = array.get(i);
                                    String v;

                                    if (fieldName != null && !fieldName.isEmpty() && item instanceof JSONObject) {
                                        // 获取对象的指定字段值
                                        Object fieldValue = ((JSONObject) item).get(fieldName);
                                        if (fieldValue == null) {
                                            v = "";
                                        } else if (fieldValue instanceof String) {
                                            v = (String) fieldValue;
                                        } else {
                                            v = JsonUtils.toJSONStringWithArm(fieldValue);
                                        }
                                    } else {
                                        // 没有字段名，返回整个对象
                                        v = JsonUtils.toJSONStringWithArm(item);
                                    }
                                    // 直接修改 targetTextNode，而不是 textNode
                                    targetTextNode.put(textField, v);
                                    hasReplacement = true;
                                } catch (Exception e) {
                                    LOG.error("loopRowTableRenderDataNewFormat tr loop render data error", e);
                                }
                            }

                            // 如果有替换，设置 sourceId 和 position（与新插入的处理方式一致）
                            if (hasReplacement && CollectionUtils.isNotEmpty(groupIds)) {
                                applySourceIdToMarksTable(targetTextNode, groupIds);
                            }


                            if (originalInlineTemplate != null) {
                                JSONArray inlineContent = originalInlineTemplate.getJSONArray("content");
                                if (inlineContent != null
                                        && inlineContent.size() == 1
                                        && "text".equals(inlineContent.getJSONObject(0).getString("type"))) {
                                    JSONObject innerTextNode = inlineContent.getJSONObject(0);
                                    paraContent.set(i1, innerTextNode);
                                }
                            }
                        }
                    }
                }

                // 从后续行中移除 markInsert 单元格（从后往前移除，避免索引变化）
                if (i > 0 && CollectionUtils.isNotEmpty(markInsertIndicesToRemove)) {
                    for (int idx = markInsertIndicesToRemove.size() - 1; idx >= 0; idx--) {
                        int cellIndex = markInsertIndicesToRemove.get(idx);
                        if (cellIndex < newCellContent.size()) {
                            newCellContent.remove(cellIndex);
                        }
                    }
                }
            }

            newRowList.add(newRow);
        }

        if (CollectionUtils.isNotEmpty(newRowList)) {
            // 先移除占位符行（在 mergeRowspanNewFormat 之前，避免影响 rowspan 计算）
            removePlaceholderTemplates(root, rowIndex, placeholderRowCount);
            int trSize = newRowList.size();
            mergeRowspanNewFormat(parameters, root, rowIndex, startLoopIndex, endLoopIndex, newRowList, trSize, cellCount);

            if (isLastRow) {
                root.remove(tableRowTemplate);
                root.addAll(newRowList);
            } else {
                root.addAll(rowIndex, newRowList);
                root.remove(rowIndex + trSize);
            }
        }
    }

    /**
     * 新格式表格行循环渲染（JSONArray数据）
     */
    private void loopRowTableRenderDataByJSONArrayNewFormat(JSONObject parameters, JSONArray root, JSONObject tableRowTemplate,
                                                            JSONArray dataObject, boolean isLastRow, int rowIndex,
                                                            int startLoopIndex, int endLoopIndex, int cellCount) {

        JSONArray cellContent = tableRowTemplate.getJSONArray("content");
        if (cellContent == null) {
            return;
        }

        // 计算模板行中最大的 rowspan 值，用于确定需要移除的占位符行数量
        int placeholderRowCount = calculatePlaceholderRowCount(cellContent);
        int size = 0;
        Map<String, Object> cache = new HashMap<>();

        if (dataObject != null) {
            size = dataObject.size();
        }


        List<JSONObject> newRowList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            JSONObject newRow = JsonUtils.parseJsonObjectWithArm(JsonUtils.toJSONStringWithArm(tableRowTemplate));
            JSONObject newRowAttrs = newRow.getJSONObject("attrs");
            if (newRowAttrs == null) {
                newRowAttrs = new JSONObject();
                newRow.put("attrs", newRowAttrs);
            }
            newRowAttrs.put("loop", null);

            JSONArray newCellContent = newRow.getJSONArray("content");
            if (newCellContent != null) {
                // 记录需要移除的 markInsert 单元格索引（用于后续行）
                List<Integer> markInsertIndicesToRemove = new ArrayList<>();

                for (int g = 0; g < newCellContent.size(); g++) {
                    JSONObject cell = newCellContent.getJSONObject(g);
                    if (cell == null) {
                        continue;
                    }

                    JSONObject cellAttrs = cell.getJSONObject("attrs");
                    if (cellAttrs == null) {
                        cellAttrs = new JSONObject();
                        cell.put("attrs", cellAttrs);
                    }

                    String markInsert = cellAttrs.getString("markInsert");
                    boolean isMarkInsert = StringUtils.isNotBlank(markInsert) && "true".equals(markInsert);

                    JSONArray cellContentArray = cell.getJSONArray("content");
                    if (cellContentArray == null) {
                        continue;
                    }

                    // 如果是 markInsert 单元格，使用 textRenderNewFormat 处理（参考普通表格的处理方式）
                    if (isMarkInsert) {
                        if (i == 0) {
                            // 第一行：渲染内容并设置 rowspan
                            for (int paraIdx = 0; paraIdx < cellContentArray.size(); paraIdx++) {
                                JSONObject paragraph = cellContentArray.getJSONObject(paraIdx);
                                if (paragraph == null) {
                                    continue;
                                }
                                if (isImageNode(paragraph)) {
                                    paraIdx = handleTableImageNode(cellContentArray, paraIdx, parameters);
                                    continue;
                                }
                                if (!"paragraph".equals(paragraph.getString("type"))) {
                                    continue;
                                }

                                JSONArray paraContent = paragraph.getJSONArray("content");
                                if (paraContent != null) {
                                    for (int k = 0; k < paraContent.size(); k++) {
                                        JSONObject textNode = paraContent.getJSONObject(k);
                                        if (textNode != null) {
                                            if (isImageNode(textNode)) {
                                                k = handleTableImageNode(paraContent, k, parameters);
                                                continue;
                                            }
                                            // 处理 checkbox、radio、chart 等（参考普通表格的处理）
                                            String isTemplate = textNode.getString("isTemplate");

                                            if (StrUtil.equals("checkbox", isTemplate) || StrUtil.equals("radio", isTemplate)) {
                                                ElementFormatterUtil.NodeData value = getValueByVariableName(parameters, textNode.getString("value"));
                                                if (value == null) {
                                                    continue;
                                                }

                                                String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(textNode.getString("value"))))
                                                        .map(obj -> obj.getString("source_workflow_run_result_id"))
                                                        .orElse("");

                                                if (StrUtil.equals("checkbox", isTemplate) && ObjectUtil.equals(value.getValue(), true)) {
                                                    textNode = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":true}}");
                                                } else if (StrUtil.equals("checkbox", isTemplate) && ObjectUtil.notEqual(value.getValue(), true)) {
                                                    textNode = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":false}}");
                                                }
                                                if (StrUtil.equals("radio", isTemplate) && ObjectUtil.equals(value.getValue(), true)) {
                                                    textNode = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"radio\":{\"value\":true}}");
                                                } else if (StrUtil.equals("radio", isTemplate) && ObjectUtil.notEqual(value.getValue(), true)) {
                                                    textNode = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"radio\":{\"value\":false}}");
                                                }

                                                if (ObjectUtil.isNotEmpty(value.getValue())) {
                                                    textNode.put("sourceId", JSONArray.of(groupIds));
                                                    textNode.put("position", IdUtil.simpleUUID().replace("-", ""));
                                                }
                                                paraContent.set(k, textNode);
                                            } else if (StrUtil.equals("chart", isTemplate)) {
                                                String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(textNode.getString("value"))))
                                                        .map(obj -> obj.getString("source_workflow_run_result_id"))
                                                        .orElse("");

                                                textNode.remove("isTemplate");

                                                String chartValueRaw = textNode.getString("chartValue");
                                                String key = chartValueRaw;
                                                if (StrUtil.isNotEmpty(chartValueRaw)) {
                                                    key = chartValueRaw.replaceFirst("^\\{\\{([^\\{].*[^\\}])\\}\\}$", "$1");
                                                }
                                                JSONObject paramObj = parameters.getJSONObject(key);
                                                if (paramObj != null && paramObj.containsKey("value")) {
                                                    Object value = paramObj.get("value");
                                                    textNode.put("chartConfig", value);

                                                    if (paramObj.containsKey("source_workflow_run_result_id")) {
                                                        textNode.put("sourceId", JSONArray.of(paramObj.get("source_workflow_run_result_id")));
                                                    } else {
                                                        textNode.put("sourceId", JSONArray.of(groupIds));
                                                    }

                                                    if (value instanceof JSONObject) {
                                                        JSONObject valueObj = (JSONObject) value;
                                                        String type = valueObj.getString("type");

                                                        // 检查 valueList 是否为空
                                                        Object valueListObj = valueObj.get("valueList");
                                                        boolean shouldDelete = false;

                                                        if (valueListObj == null ||
                                                                (valueListObj instanceof List && ((List<?>) valueListObj).isEmpty()) ||
                                                                (valueListObj instanceof JSONArray && ((JSONArray) valueListObj).isEmpty())) {
                                                            shouldDelete = true;
                                                        }

                                                        if (shouldDelete) {
                                                            // 如果 valueList 为空，删除当前元素
                                                            paraContent.remove(k);
                                                            k--;
                                                            continue;
                                                        }

                                                        // 处理股权穿透图类型
                                                        if ("chart_equity_penetration".equals(type)) {
                                                            // 获取legend
                                                            Object legendObj = valueObj.get("legend");

                                                            @SuppressWarnings("unchecked")
                                                            List<Map<String, Object>> valueList = (List<Map<String, Object>>) valueListObj;

                                                            List<String> legend = new ArrayList<>();
                                                            if (legendObj instanceof List) {
                                                                @SuppressWarnings("unchecked")
                                                                List<String> legendList = (List<String>) legendObj;
                                                                legend = legendList;
                                                            }

                                                            String imageUrl = ElementFormatterUtil.equityPenetrationImage(valueList, legend);
                                                            if (StrUtil.isNotEmpty(imageUrl)) {
                                                                textNode.put("value", imageUrl);
                                                            } else {
                                                                paraContent.remove(k);
                                                                k--;
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                // 对于非 checkbox/radio/chart 的节点，使用 textRenderNewFormat 渲染
                                                // textRenderNewFormat 会处理 inlineTemplate 并移除 isTemplate
                                                textRenderNewFormat(paraContent, k, parameters, "table");
                                            }
                                        }
                                    }
                                }
                            }

                            // 设置 rowspan 并清除 markInsert 标记
                            // 如果模板中已经设置了 rowspan，需要将其扩展（使用加法而不是乘法）
                            // 公式：最终 rowspan = 模板 rowspan + 循环生成的额外行数
                            // 因为模板行会被第一个循环行替换，所以额外行数 = size - 1
                            Object templateRowspanObj = cellAttrs.get("rowspan");
                            int templateRowspan = 1;
                            if (templateRowspanObj instanceof Number) {
                                templateRowspan = ((Number) templateRowspanObj).intValue();
                            }
                            // 最终的 rowspan = 模板 rowspan + (循环次数 - 1)
                            cellAttrs.put("rowspan", templateRowspan + size - 1);
                            cellAttrs.put("markInsert", null);
                        } else {
                            // 后续行：记录需要移除的单元格索引
                            markInsertIndicesToRemove.add(g);
                        }
                        continue; // 跳过循环数据替换
                    }

                    // 检查单元格是否在循环范围内
                    Object inLoopRangeObj = cellAttrs.get("inLoopRange");
                    boolean inLoopRange = false;
                    if (inLoopRangeObj instanceof Boolean) {
                        inLoopRange = (Boolean) inLoopRangeObj;
                    } else if (inLoopRangeObj instanceof String) {
                        inLoopRange = "true".equals(inLoopRangeObj);
                    }

                    // 如果不在循环范围内，跳过循环处理，让 mergeRowspanNewFormat 处理
                    if (!inLoopRange) {
                        continue;
                    }

                    // 非 markInsert 单元格的正常循环处理（只在循环范围内的单元格）
                    for (int paraIdx = 0; paraIdx < cellContentArray.size(); paraIdx++) {
                        JSONObject paragraph = cellContentArray.getJSONObject(paraIdx);
                        if (paragraph == null) {
                            continue;
                        }
                        if (isImageNode(paragraph)) {
                            paraIdx = handleTableImageNode(cellContentArray, paraIdx, parameters);
                            continue;
                        }
                        if (!"paragraph".equals(paragraph.getString("type"))) {
                            continue;
                        }

                        JSONArray paraContent = paragraph.getJSONArray("content");
                        if (paraContent == null) {
                            continue;
                        }

                        for (int i1 = 0; i1 < paraContent.size(); i1++) {
                            JSONObject textNode = paraContent.getJSONObject(i1);
                            if (textNode == null) {
                                continue;
                            }
                            if (isImageNode(textNode)) {
                                i1 = handleTableImageNode(paraContent, i1, parameters);
                                continue;
                            }

                            textNode.remove("isTemplate");
                            String nodeType = textNode.getString("type");
                            String eleValueStr = null;
                            String textField = null;
                            JSONObject originalInlineTemplate = null;


                            JSONObject targetTextNode = null; // 用于存储实际要修改的 text 节点

                            if ("inlineTemplate".equals(nodeType)) {
                                originalInlineTemplate = textNode;
                                JSONArray inlineContent = textNode.getJSONArray("content");
                                if (inlineContent != null) {
                                    for (int m = 0; m < inlineContent.size(); m++) {
                                        JSONObject innerNode = inlineContent.getJSONObject(m);
                                        if (innerNode != null && "text".equals(innerNode.getString("type"))) {
                                            eleValueStr = innerNode.getString("text");
                                            textField = "text";
                                            targetTextNode = innerNode; // 保存引用，但不替换 textNode
                                            break;
                                        }
                                    }
                                }
                            } else if ("text".equals(nodeType)) {
                                eleValueStr = textNode.getString("text");
                                textField = "text";
                                targetTextNode = textNode;
                            } else {
                                // 兼容旧格式
                                eleValueStr = textNode.getString("value");
                                textField = "value";
                                targetTextNode = textNode;
                            }

                            if (StringUtils.isBlank(eleValueStr) || targetTextNode == null) {
                                continue;
                            }

                            Matcher matcher = PATTERN.matcher(eleValueStr);
                            LinkedHashSet<String> groupIds = new LinkedHashSet<>();
                            boolean hasReplacement = false;

                            while (matcher.find()) {
                                try {
                                    String key = matcher.group(1);
                                    String value = matcher.group(2);
                                    String arrayIndexRef = matcher.group(3); // $1, $2, etc.
                                    String fieldName = matcher.group(4); // 字段名，如 项目

                                    // 收集 groupIds（与新插入的处理方式一致）
                                    addGroupIdTable(parameters, groupIds, key, value);

                                    if (i >= dataObject.size()) {
                                        LOG.warn("{} array index {} out of bounds, array size: {}", eleValueStr, i, dataObject.size());
                                        continue;
                                    }

                                    Object item = dataObject.get(i);
                                    String v;
                                    if (fieldName != null && !fieldName.isEmpty() && item instanceof JSONObject) {
                                        // 获取对象的指定字段值
                                        Object fieldValue = ((JSONObject) item).get(fieldName);
                                        if (fieldValue == null) {
                                            v = "";
                                        } else if (fieldValue instanceof String) {
                                            v = (String) fieldValue;
                                        } else {
                                            v = JsonUtils.toJSONStringWithArm(fieldValue);
                                        }
                                    } else {
                                        // 没有字段名，返回整个对象
                                        v = JsonUtils.toJSONStringWithArm(item);
                                    }
                                    // 直接修改 targetTextNode，而不是 textNode
                                    targetTextNode.put(textField, v);
                                    hasReplacement = true;
                                } catch (Exception e) {
                                    LOG.error("loopRowTableRenderDataByJSONArrayNewFormat tr loop render data error", e);
                                }
                            }

                            // 如果有替换，设置 sourceId 和 position（与新插入的处理方式一致）
                            if (hasReplacement && CollectionUtils.isNotEmpty(groupIds)) {
                                applySourceIdToMarksTable(targetTextNode, groupIds);
                            }

                            if (originalInlineTemplate != null) {
                                JSONArray inlineContent = originalInlineTemplate.getJSONArray("content");
                                if (inlineContent != null
                                        && inlineContent.size() == 1
                                        && "text".equals(inlineContent.getJSONObject(0).getString("type"))) {
                                    JSONObject innerTextNode = inlineContent.getJSONObject(0);
                                    paraContent.set(i1, innerTextNode);
                                }
                            }
                        }
                    }
                }

                // 从后续行中移除 markInsert 单元格（从后往前移除，避免索引变化）
                if (i > 0 && CollectionUtils.isNotEmpty(markInsertIndicesToRemove)) {
                    for (int idx = markInsertIndicesToRemove.size() - 1; idx >= 0; idx--) {
                        int cellIndex = markInsertIndicesToRemove.get(idx);
                        if (cellIndex < newCellContent.size()) {
                            newCellContent.remove(cellIndex);
                        }
                    }
                }
            }

            newRowList.add(newRow);
        }

        if (CollectionUtils.isNotEmpty(newRowList)) {
            // 先移除占位符行（在 mergeRowspanNewFormat 之前，避免影响 rowspan 计算）
            removePlaceholderTemplates(root, rowIndex, placeholderRowCount);
            int trSize = newRowList.size();
            mergeRowspanNewFormat(parameters, root, rowIndex, startLoopIndex, endLoopIndex, newRowList, trSize, cellCount);

            if (isLastRow) {
                root.remove(tableRowTemplate);
                root.addAll(newRowList);
            } else {
                root.addAll(rowIndex, newRowList);
                root.remove(rowIndex + trSize);
            }
        }
    }

    /**
     * 计算模板行中需要移除的占位符行数量
     * 当模板行中有 rowspan 的单元格时，会生成占位符行，这些行需要在循环渲染时被移除
     * 注意：只计算模板中预先存在的 rowspan（在模板定义时就有的），不包括循环渲染后设置的 rowspan
     * 但是，为了避免影响普通单元格的 rowspan 合并，我们只在确实存在占位符行时才移除它们
     */
    private int calculatePlaceholderRowCount(JSONArray cellContent) {
        if (cellContent == null || cellContent.isEmpty()) {
            return 0;
        }

        // 暂时返回 0，避免影响普通单元格的 rowspan 合并
        // 如果需要移除占位符行，应该在模板中明确标记，或者通过其他方式处理
        return 0;
    }

    /**
     * 解析 rowspan 值
     */
    private int parseRowspanValue(Object rowspanObj) {
        if (rowspanObj instanceof Number) {
            return Math.max(1, ((Number) rowspanObj).intValue());
        }
        if (rowspanObj instanceof String) {
            try {
                return Math.max(1, Integer.parseInt(((String) rowspanObj).trim()));
            } catch (NumberFormatException ignore) {
                return 1;
            }
        }
        return 1;
    }

    /**
     * 移除占位符行
     */
    private void removePlaceholderTemplates(JSONArray root, int rowIndex, int placeholderCount) {
        if (placeholderCount <= 0 || root == null) {
            return;
        }

        // 从模板行之后开始移除占位符行
        for (int i = 0; i < placeholderCount; i++) {
            int removeIndex = rowIndex + 1;
            if (removeIndex >= root.size()) {
                break;
            }
            root.remove(removeIndex);
        }
    }

    /**
     * 新格式表格合并行跨度
     */
    private void mergeRowspanNewFormat(JSONObject parameters, JSONArray root, int rowIndex, int startLoopIndex,
                                       int endLoopIndex, List<JSONObject> newRowList, int trSize, int cellCount) {
        try {
            JSONArray firstRowCells = newRowList.get(0).getJSONArray("content");
            if (firstRowCells != null) {
                List<Integer> cellsToRemove = new ArrayList<>();

                // 统一使用 inLoopRange 属性判断，不再使用可能失效的 startLoopIndex 和 endLoopIndex
                for (int j = 0; j < firstRowCells.size(); j++) {
                    JSONObject cell = firstRowCells.getJSONObject(j);
                    if (cell == null) {
                        continue;
                    }

                    JSONObject cellAttrs = cell.getJSONObject("attrs");
                    if (cellAttrs == null) {
                        cellAttrs = new JSONObject();
                        cell.put("attrs", cellAttrs);
                    }

                    // 检查是否是 markInsert 单元格（已经在循环处理中设置了 rowspan）
                    String markInsert = cellAttrs.getString("markInsert");
                    if (StringUtils.isNotBlank(markInsert) && "true".equals(markInsert)) {
                        continue;
                    }

                    // 检查是否在循环范围内
                    Object inLoopRangeObj = cellAttrs.get("inLoopRange");
                    boolean inLoopRange = false;
                    if (inLoopRangeObj instanceof Boolean) {
                        inLoopRange = (Boolean) inLoopRangeObj;
                    } else if (inLoopRangeObj instanceof String) {
                        inLoopRange = "true".equals(inLoopRangeObj);
                    }

                    // 如果不在循环范围内，设置 rowspan
                    if (!inLoopRange) {
                        // 检查单元格是否已有 rowspan（可能是模板中已有的合并单元格）
                        Object existingRowspanObj = cellAttrs.get("rowspan");
                        int existingRowspan = 1;
                        if (existingRowspanObj instanceof Number) {
                            existingRowspan = ((Number) existingRowspanObj).intValue();
                        }

                        // 如果单元格已有 rowspan，需要根据循环次数调整
                        // 原 rowspan 跨的行在循环后变成了 trSize 行
                        // 所以新 rowspan = 原 rowspan * (trSize / 原 rowspan 跨的行数)
                        // 但更简单的方式是：如果原 rowspan > 1，说明它跨了多行，这些行在循环后变成了 trSize 行
                        // 所以新 rowspan = trSize
                        // 但如果原 rowspan 跨的行数少于循环后的总行数，需要按比例调整
                        // 实际上，由于原 rowspan 跨的行都被循环了，新 rowspan 应该等于 trSize
                        // 但如果原 rowspan 跨的行中只有部分被循环，情况会更复杂
                        // 为了简化，我们假设：如果单元格不在循环范围内，它应该跨所有新生成的行
                        // 但如果原 rowspan > 1，说明它原本跨了多行，这些行在循环后变成了 trSize 行
                        // 所以新 rowspan = trSize（覆盖所有新生成的行）
                        cellAttrs.put("rowspan", trSize);
                        cellsToRemove.add(j);
                    }
                }

                // 移除后续行中不在循环范围内的单元格
                // 统一使用 inLoopRange 属性判断
                for (int i = 1; i < trSize; i++) {
                    JSONArray rowCells = newRowList.get(i).getJSONArray("content");
                    if (rowCells != null) {
                        // 从后往前遍历并移除不在循环范围内的单元格
                        for (int j = rowCells.size() - 1; j >= 0; j--) {
                            JSONObject cell = rowCells.getJSONObject(j);
                            if (cell != null) {
                                JSONObject cellAttrs = cell.getJSONObject("attrs");
                                if (cellAttrs != null) {
                                    Object inLoopRangeObj = cellAttrs.get("inLoopRange");
                                    boolean inLoopRange = false;
                                    if (inLoopRangeObj instanceof Boolean) {
                                        inLoopRange = (Boolean) inLoopRangeObj;
                                    } else if (inLoopRangeObj instanceof String) {
                                        inLoopRange = "true".equals(inLoopRangeObj);
                                    }
                                    // 如果不在循环范围内，移除该单元格
                                    if (!inLoopRange) {
                                        rowCells.remove(j);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 向前遍历所有之前的行，查找跨越到当前行的合并单元格
            // 这样可以处理合并单元格跨越多个循环行的情况
            if (rowIndex > 0) {
                // 从 rowIndex - 1 向前遍历到第 0 行
                for (int prevIdx = rowIndex - 1; prevIdx >= 0; prevIdx--) {
                    JSONObject prevRowObject = root.getJSONObject(prevIdx);
                    if (prevRowObject == null) {
                        continue;
                    }

                    JSONArray prevCellContent = prevRowObject.getJSONArray("content");
                    if (prevCellContent == null) {
                        continue;
                    }

                    for (int i = 0; i < prevCellContent.size(); i++) {
                        JSONObject cell = prevCellContent.getJSONObject(i);
                        if (cell == null) {
                            continue;
                        }

                        JSONObject cellAttrs = cell.getJSONObject("attrs");
                        if (cellAttrs == null) {
                            continue;
                        }

                        // 检查是否标记为 rowMerged 或者 rowspan > 1
                        boolean isRowMerged = "true".equals(cellAttrs.getString("rowMerged"));
                        Object rowspanObj = cellAttrs.get("rowspan");
                        int currentRowspan = (rowspanObj instanceof Number) ? ((Number) rowspanObj).intValue() : 1;

                        // 只处理有合并的单元格
                        if (!isRowMerged && currentRowspan <= 1) {
                            continue;
                        }

                        // 判断该合并单元格是否跨越到当前行
                        // prevIdx + currentRowspan > rowIndex 表示该单元格跨越到当前行
                        if (prevIdx + currentRowspan > rowIndex) {
                            // 扩展 rowspan：增加当前循环展开的额外行数（trSize - 1）
                            int newRowspan = currentRowspan + trSize - 1;
                            cellAttrs.put("rowspan", newRowspan);

                            LOG.info("扩展合并单元格 rowspan: prevIdx={}, 原 rowspan={}, 新 rowspan={}, 循环展开行数={}",
                                    prevIdx, currentRowspan, newRowspan, trSize);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * textRenderNewFormat方法
     * replaceTemplateVariableNewFormat方法
     * addGroupId方法
     * applySourceIdToMarks方法
     * ReplacementResult类
     * 为新表格的插入实现
     */

    /**
     * 新格式表格的文本渲染方法
     * 处理新格式的文本节点（使用text字段）和inlineTemplate类型
     */
    private void textRenderNewFormat(JSONArray paraContent, int nodeIndex, JSONObject parameters, String logPerfix) {

        if (StringUtils.isEmpty(logPerfix)) {
            logPerfix = "";
        }

        if (paraContent == null || nodeIndex < 0 || nodeIndex >= paraContent.size()) {
            return;
        }

        JSONObject node = paraContent.getJSONObject(nodeIndex);
        if (node == null) {
            return;
        }

        String nodeType = node.getString("type");

        if ("inlineTemplate".equals(nodeType)) {
            JSONArray inlineContent = node.getJSONArray("content");
            if (inlineContent != null) {
                for (int i = 0; i < inlineContent.size(); i++) {
                    JSONObject innerNode = inlineContent.getJSONObject(i);

                    if (innerNode != null && "text".equals(innerNode.getString("type"))) {
                        String textValue = innerNode.getString("text");

                        if (StringUtils.isNotBlank(textValue)) {
                            ReplacementResultTable replacement = replaceTemplateVariableNewFormat(textValue, parameters, logPerfix);
                            if (replacement != null) {
                                innerNode.put("text", replacement.getText());

                                JSONObject attrs = innerNode.getJSONObject("attrs");
                                if (attrs != null) {
                                    attrs.remove("isTemplate");
                                }

                                JSONObject na = node.getJSONObject("attrs");
                                if (na != null) {
                                    na.remove("isTemplate");
                                }
                                applySourceIdToMarksTable(innerNode, replacement.getGroupIds());
                            }
                        }
                    }
                }
            }

            if (inlineContent != null
                    && inlineContent.size() == 1
                    && "text".equals(inlineContent.getJSONObject(0).getString("type"))) {
                JSONObject innerTextNode = inlineContent.getJSONObject(0);
                paraContent.set(nodeIndex, innerTextNode);
            }

            return;
        }

        if ("text".equals(nodeType)) {
            String textValue = node.getString("text");
            if (StringUtils.isNotBlank(textValue)) {
                ReplacementResultTable replacement = replaceTemplateVariableNewFormat(textValue, parameters, logPerfix);
                if (replacement != null) {
                    node.put("text", replacement.getText());
                    node.remove("isTemplate");
                    applySourceIdToMarksTable(node, replacement.getGroupIds());
                }
            }
        }
    }

    /**
     * 判断节点是否为图片节点
     */
    private boolean isImageNode(JSONObject node) {
        log.info("判断节点是否为图片节点:{} ", node.getString("type"));
        return node != null && "image".equals(node.getString("type"));
    }

    /**
     * 处理表格中的图片节点，复用 ElementFormatterUtil 的图片填充逻辑
     * @return 处理后应该继续遍历的索引位置
     */
    private int handleTableImageNode(JSONArray parentArray, int index, JSONObject parameters) {
        if (parentArray == null || index < 0 || index >= parentArray.size()) {
            return index;
        }
        JSONObject imageNode = parentArray.getJSONObject(index);
        if (!isImageNode(imageNode)) {
            return index;
        }
        String position = IdUtil.simpleUUID().replace("-", "");
        Pair<JSONArray, String> result = ElementFormatterUtil.otherTypeRenderData(imageNode, parameters, "", position);
        parentArray.remove(index);
        if (result == null || "delete".equals(result.getRight())) {
            return index - 1;
        }
        JSONArray newNodes = result.getLeft();
        if (newNodes == null || newNodes.isEmpty()) {
            return index - 1;
        }
        for (int i = newNodes.size() - 1; i >= 0; i--) {
            parentArray.add(index, newNodes.getJSONObject(i));
        }
        return index + newNodes.size() - 1;
    }


    /**
     * 替换新格式中的模板变量
     */
    private ReplacementResultTable replaceTemplateVariableNewFormat(String textValue, JSONObject parameters, String logPerfix) {
        if (StringUtils.isBlank(textValue)) {
            return null;
        }

        Matcher matcher = PATTERN.matcher(textValue);
        String result = textValue;
        boolean hasReplacement = false;
        LinkedHashSet<String> groupIds = new LinkedHashSet<>();

        while (matcher.find()) {
            hasReplacement = true;
            try {
                String key = matcher.group(1);
                String value = matcher.group(2);
                String jsonDataPath = matcher.group(4);
                String path = "";

                if (jsonDataPath.contains("-")) {
                    path = jsonDataPath.replace("-", ".");
                } else {
                    path = jsonDataPath;
                }
                path = String.format("$.%s", path);

                String replaceKey = "{{" + matcher.group(1) + "#" + matcher.group(2) + "#" + matcher.group(3) + "#" + matcher.group(4) + "}}";

                try {
                    Object eval = JSONPath.eval(parameters.getJSONObject(key + "#" + value).getJSONObject("value"), path);
                    String v = eval == null ? "" : JsonUtils.toJSONStringWithArm(eval);
                    result = result.replace(replaceKey, v);
                    addGroupIdTable(parameters, groupIds, key, value);
                } catch (Exception e) {
                    String pos = matcher.group(3).replace("$", "");
                    int index = 0;
                    if (StringUtils.isNotBlank(pos)) {
                        index = Integer.parseInt(pos) - 1;
                    }
                    try {
                        Object eval = JSONPath.eval(parameters.getJSONObject(key + "#" + value).getJSONArray("value"), path);
                        String v = JsonUtils.toJSONStringWithArm(((JSONArray) eval).get(index));
                        result = result.replace(replaceKey, v);
                        addGroupIdTable(parameters, groupIds, key, value);
                    } catch (Exception e2) {
                        path = path.replace("$.", "$[" + index + "].");
                        Object eval = JSONPath.eval(parameters.getJSONObject(key + "#" + value).getJSONArray("value"), path);
                        String v = eval == null ? "" : JsonUtils.toJSONStringWithArm(eval);
                        result = result.replace(replaceKey, v);
                        addGroupIdTable(parameters, groupIds, key, value);
                    }
                }
            } catch (Exception e) {
                LOG.error(logPerfix + "文本插入渲染数据错误", e);
            }
        }


        Matcher startInputMatcher = START_INPUT_PATTERN.matcher(result);
        while (startInputMatcher.find()) {
            hasReplacement = true;
            try {
                String value = parameters.getJSONObject(startInputMatcher.group(1)).getString("value");
                result = result.replace("{{开始输入#" + startInputMatcher.group(1) + "}}", value);
            } catch (Exception e) {
                LOG.error(logPerfix + "文本渲染开始输入渲染数据错误", e);
            }
        }

        Matcher moduleValueMatcher = MODULE_VALUE_PATTERN.matcher(result);
        while (moduleValueMatcher.find()) {
            hasReplacement = true;
            try {
                String moduleKey = moduleValueMatcher.group(1) + "#" + moduleValueMatcher.group(2);
                String value = parameters.getJSONObject(moduleKey).getString("value");
                result = result.replace("{{" + moduleValueMatcher.group(1) + "#" + moduleValueMatcher.group(2) + "}}", value);
            } catch (Exception e) {
                LOG.error(logPerfix + "文本渲染模块值渲染数据错误", e);
            }
        }

        return hasReplacement ? new ReplacementResultTable(result, groupIds) : null;
    }
    /**
     * 添加分组ID
     */
    private void addGroupIdTable(JSONObject parameters, LinkedHashSet<String> groupIds, String key, String value) {
        JSONObject paramObj = parameters.getJSONObject(key + "#" + value);
        if (paramObj != null) {
            String id = paramObj.getString("source_workflow_run_result_id");
            if (StringUtils.isNotBlank(id)) {
                groupIds.add(id);
            }
        }
    }

    /**
     * 处理 annotations 数组中的模版变量替换
     * @param template 文档模板 JSON 对象
     * @param parameters 工作流参数对象
     */
    private void processAnnotationsTemplate(JSONObject template, JSONObject parameters) {
        if (template == null || parameters == null) {
            return;
        }

        // 检查 annotations 数组是否存在
        JSONArray annotations = template.getJSONArray("annotations");
        if (annotations == null || annotations.isEmpty()) {
            LOG.debug("模板中不存在 annotations 数组或数组为空，跳过处理");
            return;
        }

        LOG.debug("开始处理 annotations 数组，共 {} 个 annotation", annotations.size());

        // 遍历每个 annotation
        for (int i = 0; i < annotations.size(); i++) {
            try {
                JSONObject annotation = annotations.getJSONObject(i);
                if (annotation == null) {
                    LOG.debug("annotation[{}] 为 null，跳过", i);
                    continue;
                }

                // 获取 content 字段
                String content = annotation.getString("content");
                if (StringUtils.isBlank(content)) {
                    LOG.debug("annotation[{}] 的 content 为空，跳过", i);
                    continue;
                }

                LOG.debug("处理 annotation[{}] content 替换前: {}", i, content);

                // 执行文本替换
                String replacedContent = replaceAnnotationContent(content, parameters);

                // 写回替换后的内容
                if (replacedContent != null && !replacedContent.equals(content)) {
                    annotation.put("content", replacedContent);
                    LOG.debug("annotation[{}] content 替换后: {}", i, replacedContent);
                } else {
                    LOG.debug("annotation[{}] content 未发生替换", i);
                }
            } catch (Exception e) {
                LOG.warn("处理 annotation[{}] 时发生错误，继续处理下一个", i, e);
                // 继续处理下一个，不中断流程
            }
        }

        LOG.debug("annotations 数组处理完成");
    }

    /**
     * 替换 annotation content 中的模版变量（仅文本替换）
     * @param content 原始 content 文本
     * @param parameters 工作流参数对象
     * @return 替换后的文本，如果无替换则返回原文本
     */
    private String replaceAnnotationContent(String content, JSONObject parameters) {
        if (StringUtils.isBlank(content)) {
            return content;
        }

        // 使用现有的替换方法，但只返回文本部分
        ReplacementResultTable result = replaceTemplateVariableNewFormat(content, parameters, "[Annotation]");
        if (result != null && result.getText() != null) {
            return result.getText();
        }

        return content;
    }

    /**
     * 应用 sourceId 和 position 到标记（使用 textStyle 类型）
     */
    private void applySourceIdToMarksTable(JSONObject node, LinkedHashSet<String> groupIds) {
        if (node == null || CollectionUtils.isEmpty(groupIds)) {
            return;
        }

        if (!"text".equals(node.getString("type"))) {
            return;
        }


        JSONArray marks = node.getJSONArray("marks");
        if (marks == null || marks.isEmpty()) {
            marks = new JSONArray();
            JSONObject mark = new JSONObject();
            mark.put("type", "textStyle");
            mark.put("attrs", new JSONObject());
            marks.add(mark);
            node.put("marks", marks);
        }


        Object sourceValue;
        if (groupIds.size() == 1) {
            sourceValue = groupIds.iterator().next();
        } else {
            JSONArray ids = new JSONArray();
            ids.addAll(groupIds);
            sourceValue = ids;
        }

        for (int j = 0; j < marks.size(); j++) {
            JSONObject mark = marks.getJSONObject(j);
            if (!"textStyle".equals(mark.getString("type"))) {
                continue;
            }
            JSONObject attrs = mark.getJSONObject("attrs");
            if (attrs == null) {
                attrs = new JSONObject();
                mark.put("attrs", attrs);
            }
            attrs.put("sourceId", sourceValue);
            attrs.put("position", IdUtil.simpleUUID().replace("-", ""));
        }
    }

    /**
     * 替换文本中的变量
     */
    @Data
    private static class ReplacementResultTable {
        private final String text;
        private final LinkedHashSet<String> groupIds;
    }
    /**
     * 执行条件判断表达式
     * @param conds
     * @return
     */
    private List<String> processExpression(List<List<String>> conds) {
        Configuration cfg = new Configuration(new Version(2, 3, 31));
        cfg.setDefaultEncoding("UTF-8");
        // 设置更宽松的语法配置
        cfg.setClassicCompatible(true);
        cfg.setWhitespaceStripping(true);

        Map<String, Object> dataModel = new HashMap<>();
        List<String> renderResult = new ArrayList<>();
        for (List<String> cond : conds) {
            String finalString = String.join("", cond).replace("↪", "").replace("↩", "");
            // 兼容 <#else if> 格式，转换为 <#elseif>
            String normalized = finalString.replaceAll("<#else\\s+if", "<#elseif");
            if (!StringUtils.equals(finalString, normalized)) {
                LOG.info("将条件表达式中的 <#else if> 兼容为 <#elseif>");
                finalString = normalized;
            }
            LOG.info("finalString: {}", finalString);
            try {
                Template freemarkerTemplate = new Template("templateName", finalString, cfg);

                // 渲染模板到StringWriter
                StringWriter out = new StringWriter();
                freemarkerTemplate.process(dataModel, out);

                // 获取渲染结果的字符串内容
                String renderContent = out.toString();
                LOG.info("render result: {}", renderContent);

                // 直接添加字符串内容，而不是序列化StringWriter对象
                renderResult.add(renderContent);
            } catch (TemplateException | IOException e) {
                LOG.error("condition replace render data error", e);
            }
        }
        return renderResult;
    }
    /**
     * 替换标签值
     * @param conds
     * @param parameters
     */
    private void changeOfVariable(List<List<String>> conds, JSONObject parameters) {
        for (List<String> cond : conds) {
            for (int i = 0; i < cond.size(); i++) {
                String c = cond.get(i);
                LOG.info("changeOfVariable c: {}", c);
                Matcher matcher = PATTERN.matcher(c);
                while (matcher.find()) {
                    try {
                        String key = matcher.group(1);
                        String value = matcher.group(2);
                        // LOG.info(c + " Found key-value pair: " + key + "#" + value);
                        String jsonDataPath = matcher.group(4);
                        String path = "";
                        // if (jsonDataPath.contains("_") && !jsonDataPath.contains("-")) {
                        //     path = jsonDataPath.replace("_", ".");
                        // } else
                        if (jsonDataPath.contains("-")) {
                            path = jsonDataPath.replace("-", ".");
                        } else {
                            path = jsonDataPath;
                        }
                        List<String> keyList = new ArrayList<>();
                        int groupCount = matcher.groupCount();
                        for (int j = 1; j <= groupCount; j++) {
                            keyList.add(matcher.group(j));
                        }
                        String replaceKey = "{{" + String.join("#", keyList) + "}}";
                        path = String.format("$.%s", path);
                        LOG.info("condition dataKey " + key + "#" + value + " replaceKey " + replaceKey + " path " + path);
                        try {
                            Object eval = JSONPath.eval(parameters.getJSONObject(key + "#" + value).getJSONObject("value"), path);
                            String v = eval == null ? "" : JsonUtils.toJSONStringWithArm(eval);
                            cond.set(i, c.replace(replaceKey, v));
                        } catch (Exception e) {
                            String pos = matcher.group(3).replace("$", "");
                            int index = 0;
                            if (StringUtils.isNotBlank(pos)) {
                                index = Integer.parseInt(pos) - 1;
                            }
                            try {
                                // LOG.info("condition replace render array 1 path {}", path);
                                Object eval = JSONPath.eval(parameters.getJSONObject(key + "#" + value).getJSONArray("value"), path);
                                String v = JsonUtils.toJSONStringWithArm(((JSONArray) eval).get(index));
                                LOG.info("condition replace render array 1 path {} value {}", path, v);
                                cond.set(i, c.replace(replaceKey, v));
                            } catch (Exception e2) {
                                path = path.replace("$.", "$[" + index + "].");
                                LOG.info("condition replace render array 2 path {}", path);
                                Object eval = JSONPath.eval(parameters.getJSONObject(key + "#" + value).getJSONArray("value"), path);
                                LOG.info("condition replace render array 2 path {} value {}", path, eval == null ? "" : JsonUtils.toJSONStringWithArm(eval));
                                cond.set(i, c.replace(replaceKey, eval == null ? "" : JsonUtils.toJSONStringWithArm(eval)));
                            }

                        }
                    } catch (Exception e) {
                        LOG.error("condition replace render data error", e);
                    }
                }
                Matcher startInputMatcher = START_INPUT_PATTERN.matcher(c.replace("\"variableName\":\"{{", "\"variableName\":\""));
                while (startInputMatcher.find()) {
                    try {
                        LOG.info("startInputMatcher found key-value pair {}", startInputMatcher.group(1));
                        String value = parameters.getJSONObject(startInputMatcher.group(1)).getString("value");
                        cond.set(i, c.replace("{{开始输入#" + startInputMatcher.group(1) + "}}", value));
                    } catch (Exception e) {
                        LOG.error("start input condition replace render data error", e);
                    }
                }
            }
        }
    }

    /**
     * 文本渲染
     * @param subItem
     * @param parameters
     * @param logPerfix 用于记录日志
     */
    private void textRender(JSONObject subItem, JSONObject parameters, String logPerfix) {
        if (StringUtils.isEmpty(logPerfix)) {
            logPerfix = "";
        }
        Matcher matcher = PATTERN.matcher(subItem.getString("value"));
        while (matcher.find()) {
            subItem.remove("isTemplate");
            try {
                String key = matcher.group(1);
                String value = matcher.group(2);
                // LOG.info(logPerfix + " " + subItem.getString("value") + " Found key-value pair: " + key + "#" + value);
                String jsonDataPath = matcher.group(4);
                String path = "";
                // if (jsonDataPath.contains("_") && !jsonDataPath.contains("-")) {
                //     path = jsonDataPath.replace("_", ".");
                // } else
                if (jsonDataPath.contains("-")) {
                    path = jsonDataPath.replace("-", ".");
                } else {
                    path = jsonDataPath;
                }
                path = String.format("$.%s", path);
                try {
                    LOG.info(logPerfix + " text insert render dataKey " + key + "#" + value + " path " + path);
                    Object eval = JSONPath.eval(parameters.getJSONObject(key + "#" + value).getJSONObject("value"), path);
                    String v = eval == null ? "": JsonUtils.toJSONStringWithArm(eval);
                    subItem.put("value", v);
                    // 设置溯源信息
                    if (StringUtils.isNotBlank(v)) {
                        subItem.put("groupIds", JSONArray.of(parameters.getJSONObject(key + "#" + value).getString("source_workflow_run_result_id")));
                    }
                } catch (Exception e) {
                    String pos = matcher.group(3).replace("$", "");
                    int index = 0;
                    if (StringUtils.isNotBlank(pos)) {
                        index = Integer.parseInt(pos) - 1;
                    }
                    try {
                        Object eval = JSONPath.eval(parameters.getJSONObject(key + "#" + value).getJSONArray("value"), path);
                        String v = JsonUtils.toJSONStringWithArm(((JSONArray) eval).get(index));
                        subItem.put("value", v);
                        // 设置溯源信息
                        if (StringUtils.isNotBlank(v)) {
                            subItem.put("groupIds", JSONArray.of(parameters.getJSONObject(key + "#" + value).getString("source_workflow_run_result_id")));
                        }
                    } catch (Exception e2) {
                        path = path.replace("$.", "$[" + index + "].");
                        LOG.info(logPerfix + " text insert render array path {}", path);
                        Object eval = JSONPath.eval(parameters.getJSONObject(key + "#" + value).getJSONArray("value"), path);
                        String v = eval == null ? "" : JsonUtils.toJSONStringWithArm(eval);
                        subItem.put("value", v);
                        // 设置溯源信息
                        if (StringUtils.isNotBlank(v)) {
                            subItem.put("groupIds", JSONArray.of(parameters.getJSONObject(key + "#" + value).getString("source_workflow_run_result_id")));
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error(logPerfix + " text insert render data error", e);
            }
        }
        // 读取全局变量
        setGlobalVariable(subItem, parameters, logPerfix);
        // 设置模块输出，如QDoc、普通提示
        setModuleVariable(subItem, parameters, logPerfix);
    }

    private void setGlobalVariable(JSONObject subItem, JSONObject parameters, String logPerfix) {
        Matcher startInputMatcher = START_INPUT_PATTERN.matcher(subItem.getString("value"));
        // LOG.info("{} startInputMatcher subItem {}", logPerfix, subItem);
        while (startInputMatcher.find()) {
            try {
                LOG.info("{} text render startInputMatcher found key-value pair {}", logPerfix, startInputMatcher.group(1));
                String value = parameters.getJSONObject(startInputMatcher.group(1)).getString("value");
                subItem.put("value", value);
                // 全局变量不设置溯源信息
                // if (StringUtils.isNotBlank(value)) {
                //     subItem.put("groupIds", JSONArray.of(parameters.getJSONObject(startInputMatcher.group(1)).getString("source_workflow_run_result_id")));
                // }
            } catch (Exception e) {
                LOG.error( logPerfix + " text render start input render data error", e);
            }
        }
    }

    private void setModuleVariable(JSONObject subItem, JSONObject parameters, String logPerfix) {
        Matcher moduleValueMatcher = MODULE_VALUE_PATTERN.matcher(subItem.getString("value"));
        // LOG.info("{} startInputMatcher subItem {}", logPerfix, subItem);
        while (moduleValueMatcher.find()) {
            try {
                String moduleKey = moduleValueMatcher.group(1) + "#" + moduleValueMatcher.group(2);
                LOG.info("{} text render moduleValueMatcher found key-value pair {}", logPerfix, moduleKey);
                String value = parameters.getJSONObject(moduleKey).getString("value");
                subItem.put("value", value);
                // 设置溯源信息
                if (StringUtils.isNotBlank(value)) {
                    subItem.put("groupIds", JSONArray.of(parameters.getJSONObject(moduleKey).getString("source_workflow_run_result_id")));
                }
            } catch (Exception e) {
                LOG.error( logPerfix + " text render moduleValue render data error", e);
            }
        }
    }

    private List<RenderElement> conditionParserComplex(JSONArray jsonArray) {

        List<RenderElement> result = new ArrayList<>();
        boolean collecting = false;
        List<String> list = null;
        RenderElement renderElement = null;
        int startCount = 0;
        int endCount = 0;
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            if ("start".equals(obj.getString("conditionDirection"))) {
                collecting = true;
                startCount += 1;
                if (list == null) {
                    list = new ArrayList<>();
                    renderElement = new RenderElement();
                    renderElement.setIndex(i);
                }
                renderElement.getPrepareRemoveObjects().put(i, obj);
            } else if ("end".equals(obj.getString("conditionDirection"))) {
                endCount += 1;
                if (startCount == endCount) {
                    collecting = false;
                    startCount = 0;
                    endCount = 0;
                    renderElement.getResult().add(list);
                    renderElement.getPrepareRemoveObjects().put(i, obj);
                    result.add(renderElement);
                    list = null;
                }
            }

            if (collecting && !"start".equals(obj.getString("conditionDirection"))) {
                String value = obj.getString("value");
                if ("true".equals(obj.getString("isCondition")) && ((value.startsWith("<#") || value.startsWith("</#")) && value.endsWith(">"))) {
                    list.add(value);
                } else {
                    String jsonString = JsonUtils.toJSONStringWithArm(obj);
                    JSONObject newObj = JsonUtils.parseJsonObjectWithArm(jsonString);
                    newObj.put("newObject", IdUtil.simpleUUID().replace("-", ""));
                    // list.add(JSON.toJSONString(newObj, JSONWriter.Feature.WriteNulls));
                    list.add(JsonUtils.toJSONStringWithArm(newObj));
                }
                // if ("table".equals(obj.getString("type"))) {
                //     list.add(JSON.toJSONString(obj, JSONWriter.Feature.WriteNulls));
                // } else if ((obj.getJSONArray("valueList") != null && !obj.getJSONArray("valueList").isEmpty())) {
                //     list.add(JSON.toJSONString(obj, JSONWriter.Feature.WriteNulls));
                // }
                // if (!"\n".equals(value) || "true".equals(obj.getString("isCondition"))) {
                renderElement.getPrepareRemoveObjects().put(i, obj);
                // }

            }
        }
        // 按位置从大到小排，方便操作 jsonobject
        Collections.sort(result, (o1, o2) -> Integer.compare(o2.index, o1.index));
        return result;
    }

    /**
     * 表格行循环渲染
     *
     * @param root       trTemplate 的父级结构
     * @param trTemplate 行模版
     * @param dataObject 数据
     * @param isLastRow 是否为最后一行
     * @param rowIndex 行索引
     * @param startLoopIndex 如果有 startLoopIndex 和 endLoopIndex 时，为一行中连续的列进行循环，同行的其他表格为合并单元格
     * @param endLoopIndex 如果有 startLoopIndex 和 endLoopIndex 时，为一行中连续的列进行循环，同行的其他表格为合并单元格
     */
    private void loopRowTableRenderData(JSONObject parameters, JSONArray root, JSONObject trTemplate, JSONObject dataObject, boolean isLastRow,
                                        int rowIndex, int startLoopIndex, int endLoopIndex, int colgroup) {
        JSONArray tdList = trTemplate.getJSONArray("tdList");
        // 根据模板找到对应的数据，放到 cache 中
        int size = 0;
        Map<String, Object> cache = new HashMap<String, Object>();
        int tdSize = tdList.size();
        for (int g = 0; g < tdSize; g++) {
            JSONObject td = tdList.getJSONObject(g);
            JSONArray ele = td.getJSONArray("value");
            if (ele.isEmpty()) {
                continue;
            }
            for (int i = 0; i < ele.size(); i++) {
                JSONObject eleValue = ele.getJSONObject(i);
                String eleValueStr = eleValue.getString("value");
                Matcher matcher = PATTERN.matcher(eleValueStr);
                while (matcher.find()) {
                    String key = matcher.group(1);
                    String value = matcher.group(2);
                    String jsonDataPath = matcher.group(4);
                    String path = "";
                    // if (jsonDataPath.contains("_") && !jsonDataPath.contains("-")) {
                    //     path = jsonDataPath.replace("_", ".");
                    // } else
                    if (jsonDataPath.contains("-")) {
                        path = jsonDataPath.replace("-", ".");
                    } else {
                        path = jsonDataPath;
                    }
                    path = String.format("$.%s", path);
                    // LOG.info(eleValueStr + " Found key-value pair: " + key + "#" + value + " path " + path);
                    Object eval = JSONPath.eval(JsonUtils.toJSONStringWithArm(dataObject), path);
                    if (eval == null) {
                        continue;
                    }
                    if (size == 0) {
                        try {
                            size = ((JSONArray) eval).size();
                        } catch (JSONException e) {
                            LOG.error("Error reading file expectation is array: " + path, e);
                        }
                    }
                    cache.putIfAbsent(path, eval);
                }

                setGlobalVariable(eleValue, parameters, "loopRowTableRenderData");
            }

        }

        // 生成新的数据行
        List<JSONObject> newTrList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            // JSONObject newTr = JSONObject.parseObject(trTemplate.toString());
            JSONObject newTr = JsonUtils.parseJsonObjectWithArm(JsonUtils.toJSONStringWithArm(trTemplate));
            newTr.put("loop", "false");
            JSONArray tds = newTr.getJSONArray("tdList");
            for (int g = 0; g < tds.size(); g++) {
                JSONObject td = tds.getJSONObject(g);
                td.remove("backgroundColor");
                JSONArray ele = td.getJSONArray("value");
                if (ele.isEmpty()) {
                    continue;
                }
                for (int i1 = 0; i1 < ele.size(); i1++) {
                    JSONObject eleValue = ele.getJSONObject(i1);
                    eleValue.remove("isTemplate");
                    String eleValueStr = eleValue.getString("value");
                    Matcher matcher = PATTERN.matcher(eleValueStr);
                    while (matcher.find()) {
                        try {
                            String key = matcher.group(1);
                            String value = matcher.group(2);
                            String jsonDataPath = matcher.group(4);
                            String path = "";
                            // if (jsonDataPath.contains("_") && !jsonDataPath.contains("-")) {
                            //     path = jsonDataPath.replace("_", ".");
                            // } else
                            if (jsonDataPath.contains("-")) {
                                path = jsonDataPath.replace("-", ".");
                            } else {
                                path = jsonDataPath;
                            }
                            path = String.format("$.%s", path);
                            LOG.info(eleValueStr + "object found key-value pair: " + key + "#" + value + " path " + path);
                            Object o = cache.get(path);
                            if (o == null) {
                                LOG.warn( "{} not found object data by path {} ",eleValueStr, path);
                                continue;
                            }
                            String v = JsonUtils.toJSONStringWithArm(((JSONArray) o).get(i));
                            eleValue.put("value", v);
                        } catch (Exception e) {
                            LOG.error("loopRowTableRenderData tr loop render data error", e);
                        }

                    }
                }
            }
            // root.add(newTr);
            newTrList.add(newTr);
        }
        if (CollectionUtils.isNotEmpty(newTrList)) {
            int trSize = newTrList.size();
            mergeRowspan(parameters, root, rowIndex, startLoopIndex, endLoopIndex, newTrList, trSize, tdSize, colgroup);

            if (isLastRow) {
                root.remove(trTemplate);
                root.addAll(newTrList);
            } else {
                root.addAll(rowIndex, newTrList);
                root.remove(rowIndex + trSize);
            }

        }
    }

    private void mergeRowspan(JSONObject parameters, JSONArray root, int rowIndex, int startLoopIndex, int endLoopIndex, List<JSONObject> newTrList, int trSize, int tdSize, int colgroup) {
        try {
            if (!((startLoopIndex == -1 && endLoopIndex == -1) || (startLoopIndex ==0 && endLoopIndex == tdSize-1 && colgroup == tdSize))) {

                // 如果 startLoopIndex 和 endLoopIndex 不为 -1 时，计算需要合并的单元格
                JSONArray tds = newTrList.getFirst().getJSONArray("tdList");
                for (int j = 0; j < startLoopIndex; j++) {
                    tds.getJSONObject(j).put("rowspan", trSize);
                }

                for (int i = endLoopIndex + 1; i < tdSize; i++) {
                    tds.getJSONObject(i).put("rowspan", trSize);
                }

                for (int i = 1; i < trSize; i++) {
                    JSONArray tdList1 = newTrList.get(i).getJSONArray("tdList");
                    String jsonString =JsonUtils.toJSONStringWithArm(tdList1);
                    JSONArray array = JSONArray.parseArray(jsonString);
                    tdList1.clear();
                    for (int j = startLoopIndex; j <= endLoopIndex; j++) {
                        tdList1.add(array.get(j));
                    }
                }

                // 处理模板中有合并表格的情况
                if (rowIndex > 0) {
                    JSONObject preRowObject = root.getJSONObject(rowIndex - 1);
                    JSONArray preTdList = preRowObject.getJSONArray("tdList");
                    for (int i = 0; i < preTdList.size(); i++) {
                        JSONObject td = preTdList.getJSONObject(i);
                        if ("true".equals(td.getString("rowMerged"))) {
                            // JSONArray  eleArray = td.getJSONArray("value");
                            // for (int k = 0; k < eleArray.size(); k++) {
                            //     JSONObject eleValue = eleArray.getJSONObject(k);
                            //     LOG.warn("==========> {}", eleValue);
                            //     textRender(eleValue, parameters, "mergeRowspan");
                            // }
                            td.put("rowspan", td.getIntValue("rowspan") + trSize -1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("mergeRowspan error", e);
        }

    }

    /**
     *
     * @param root trTemplate 的父级结构
     * @param trTemplate 模板
     * @param dataObject 数据
     * @param isLastRow 是否为最后一行
     * @param rowIndex 行索引
     */
    private void loopRowTableRenderDataByJSONArray(JSONObject parameters, JSONArray root, JSONObject trTemplate, JSONArray dataObject,
                                                   boolean isLastRow, int rowIndex, int startLoopIndex, int endLoopIndex, int colgroup) {
        JSONArray tdList = trTemplate.getJSONArray("tdList");
        // 根据模板找到对应的数据，放到 cache 中
        int size = 0;
        Map<String, Object> cache = new HashMap<String, Object>();
        int tdSize = tdList.size();
        for (int g = 0; g < tdSize; g++) {
            JSONObject td = tdList.getJSONObject(g);
            td.remove("backgroundColor");
            JSONArray ele = td.getJSONArray("value");
            if (ele.isEmpty()) {
                continue;
            }
            for (int i = 0; i < ele.size(); i++) {
                JSONObject eleValue = ele.getJSONObject(i);
                String eleValueStr = eleValue.getString("value");
                Matcher matcher = PATTERN.matcher(eleValueStr);
                while (matcher.find()) {
                    String key = matcher.group(1);
                    String value = matcher.group(2);
                    String jsonDataPath = matcher.group(4);
                    String path = "";
                    // if (jsonDataPath.contains("_") && !jsonDataPath.contains("-")) {
                    //     path = jsonDataPath.replace("_", ".");
                    // } else
                    if (jsonDataPath.contains("-")) {
                        path = jsonDataPath.replace("-", ".");
                    } else {
                        path = jsonDataPath;
                    }
                    path = String.format("$.%s", path);
                    // LOG.info(eleValueStr + " Found key-value pair: " + key + "#" + value + " path " + path);
                    // Object eval = JSONPath.eval(dataObject.toString(), path);
                    if (size == 0) {
                        try {
                            if (dataObject != null) {
                                size = dataObject.size();
                            }
                        } catch (JSONException e) {
                            LOG.error("Error reading file expectation is array: " + path, e);
                        }
                    }
                    cache.putIfAbsent(path, dataObject);
                }
                setGlobalVariable(eleValue, parameters, "loopRowTableRenderDataByJSONArray");
                String rowMerged = td.getString("rowMerged");
                if ("true".equals(rowMerged)) {
                    textRender(eleValue, parameters, "loopRowTableRenderDataByJSONArray");
                }
            }

        }

        List<JSONObject> newTrList = new ArrayList<>();
        // 生成新的数据行
        for (int i = 0; i < size; i++) {
            // JSONObject newTr = JSONObject.parseObject(trTemplate.toString());
            JSONObject newTr = JsonUtils.parseJsonObjectWithArm(JsonUtils.toJSONStringWithArm(trTemplate));
            newTr.put("loop", "false");
            JSONArray tds = newTr.getJSONArray("tdList");
            for (int g = 0; g < tds.size(); g++) {
                JSONObject td = tds.getJSONObject(g);

                JSONArray ele = td.getJSONArray("value");
                if (ele.isEmpty()) {
                    continue;
                }
                for (int i1 = 0; i1 < ele.size(); i1++) {
                    JSONObject eleValue = ele.getJSONObject(i1);
                    eleValue.remove("backgroundColor");
                    eleValue.remove("isTemplate");
                    String eleValueStr = eleValue.getString("value");
                    Matcher matcher = PATTERN.matcher(eleValueStr);
                    while (matcher.find()) {
                        try {
                            String key = matcher.group(1);
                            String value = matcher.group(2);
                            String jsonDataPath = matcher.group(4);
                            // String path = jsonDataPath.replace("-", ".");
                            String path = "";
                            // if (jsonDataPath.contains("_") && !jsonDataPath.contains("-")) {
                            //     path = jsonDataPath.replace("_", ".");
                            // } else
                            if (jsonDataPath.contains("-")) {
                                path = jsonDataPath.replace("-", ".");
                            } else {
                                path = jsonDataPath;
                            }
                            path = String.format("$.%s", path);
                            LOG.info(eleValueStr + "array found key-value pair: " + key + "#" + value + " path " + path);
                            Object o = cache.get(path);
                            if (o == null) {
                                LOG.warn("{} not found array data by path {}", eleValueStr, path);
                                continue;
                            }
                            Object v = JSONPath.eval(JsonUtils.toJSONStringWithArm(((JSONArray) o).get(i)), path);
                            eleValue.put("value", v == null ? "" : JsonUtils.toJSONStringWithArm(v));
                        } catch (Exception e) {
                            LOG.error("tr loop render data error", e);
                        }
                    }
                }
            }
            // root.add(newTr);
            newTrList.add(newTr);
        }
        if (CollectionUtils.isNotEmpty(newTrList)) {
            int trSize = newTrList.size();
            mergeRowspan(parameters, root, rowIndex, startLoopIndex, endLoopIndex, newTrList, trSize, tdSize, colgroup);
            if (isLastRow) {
                root.remove(trTemplate);
                root.addAll(newTrList);
            } else {
                root.addAll(rowIndex, newTrList);
                root.remove(rowIndex + trSize);
            }

        }
    }


    /**
     * 文本内容markDown格式转Wrod格式
     */
    public JSONObject markDownToWord(String contentMarkDown) {
        MarkDownToWordParam markDownToWordParam = new MarkDownToWordParam();
        markDownToWordParam.setMarkdown(contentMarkDown);
        JSONObject wordResult = cmsNodeServiceClient.markdownToWord(markDownToWordParam);
        LOG.info(">>>>>文本内容markDown格式转Wrod格式，调用结果为：{}", wordResult);
        if (ObjectUtil.isNull(wordResult) || wordResult.getIntValue("code") != 0) {
            LOG.error(">>>>>文本内容markDown格式转Wrod格式，调用失败，失败原因：{}", wordResult.getString("message"));
            return null;
        }
        return wordResult.getJSONObject("data");
    }

    /**
     * 处理非表格类型的数据
     */
    private JSONObject otherTypeRenderData(JSONObject templateItem, JSONObject parameters) {
        //todo 其他类型，目前只有文本类型，如果后期再添加其他类型时再拓展。
        LOG.info(">>>>>处理非表格类型的数据，templateItem为：{}", templateItem);
        String isTemplate = templateItem.getString("isTemplate");
        // 处理复选框和单选框
        if(StrUtil.equals("checkbox",isTemplate) || StrUtil.equals("radio",isTemplate)){
            Object etlValue = getValueByEtlName(parameters, templateItem.getString("value"));
            if(StrUtil.equals("checkbox",isTemplate) && ObjectUtil.equals(etlValue, true)){
                // templateItem = JSONObject.parseObject("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":true}}");
                templateItem = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":true}}");
            } else if (StrUtil.equals("checkbox", isTemplate) && ObjectUtil.notEqual(etlValue, true)) {
                // templateItem = JSONObject.parseObject("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":false}}");
                templateItem = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"checkbox\",\"width\":24,\"checkbox\":{\"value\":false}}");
            }
            if (StrUtil.equals("radio",isTemplate) && ObjectUtil.equals(etlValue, true)) {
                // templateItem = JSONObject.parseObject("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"checkbox\":{\"value\":true}}");
                templateItem = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"checkbox\":{\"value\":true}}");
            } else if (StrUtil.equals("radio",isTemplate) && ObjectUtil.notEqual(etlValue, true)) {
                // templateItem = JSONObject.parseObject("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"checkbox\":{\"value\":false}}");
                templateItem = JsonUtils.parseJsonObjectWithArm("{\"value\":\"\",\"type\":\"radio\",\"width\":24,\"checkbox\":{\"value\":false}}");
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
    private Object getValueByEtlName(JSONObject parameters, String etlVariableName) {
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

    @Data
    public class RenderElement {
        // 记录 start 位置
        Integer index;
        // List<JSONObject> prepareRemoveObjects = new ArrayList<>();
        Map<Integer, JSONObject> prepareRemoveObjects = new HashMap<>();
        // 条件判断
        List<List<String> >result = new ArrayList<>();
    }

    // 判断一个集合中是否包含指定的引用对象（使用 == 判断
    private static boolean containsByReference(Collection<?> collection, Object target) {
        for (Object obj : collection) {
            if (obj == target) {
                return true;
            }
        }
        return false;
    }

    /**
     * 修复JSON字符串中的转义字符问题
     * @param jsonStr 原始JSON字符串
     * @return 修复后的JSON字符串
     */
    private String fixJsonEscapeCharacters(String jsonStr) {
        if (StringUtils.isBlank(jsonStr)) {
            return jsonStr;
        }

        try {
            // 先尝试解析，如果成功则直接返回
            JsonUtils.parseJsonObjectWithArm(jsonStr);
            return jsonStr;
        } catch (Exception e) {
            // 解析失败，进行转义字符修复
            LOG.warn("JSON解析失败，尝试修复转义字符: {}", jsonStr);
        }

        String fixed = jsonStr;

        // 处理错误的转义序列
        // 1. 修复双重转义
        fixed = fixed.replace("\\\\n", "\\n")
                .replace("\\\\t", "\\t")
                .replace("\\\\r", "\\r")
                .replace("\\\\\"", "\\\"")
                .replace("\\\\\\\\", "\\\\");

        // 2. 处理不正确的反斜杠转义
        // 移除不必要的反斜杠（除了合法的转义字符）
        fixed = fixed.replaceAll("\\\\(?![\"\\\\nrtbf/])", "");

        // 3. 确保字符串值中的换行符被正确转义
        fixed = fixed.replaceAll("(?<!\\\\)\\n", "\\\\n")
                .replaceAll("(?<!\\\\)\\r", "\\\\r")
                .replaceAll("(?<!\\\\)\\t", "\\\\t");

        // 如果修复后的字符串仍然无法解析，尝试更激进的修复
        try {
            JsonUtils.parseJsonObjectWithArm(fixed);
            return fixed;
        } catch (Exception e) {
            LOG.warn("JSON修复失败，返回原始字符串: {}", jsonStr);
            // 如果仍然无法解析，返回原始字符串
            return jsonStr;
        }
    }

}
