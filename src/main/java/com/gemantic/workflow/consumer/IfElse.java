package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.workflow.Condition;
import com.gemantic.gpt.support.workflow.Node;
import com.gemantic.gpt.support.workflow.WorkflowException;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.utils.JsonUtils;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 条件判断组件
 */
@Component("ifElse")
public class IfElse extends BaseConsumer {

    private static final Pattern VALUE_PATTERN = Pattern.compile("\\{\\{([^#]*?)#([^#|^$].*?)#\\$(\\d+)#([^#].*?)\\}\\}");

    private static final Pattern CUSTOM_VALUE_PATTERN = Pattern.compile("\\{\\{([^#]*?)#\\$(\\d+)#([^#].*?)\\}\\}");
    // 模块变量，如 Doc 组件、大模型组件，输出变量为 普通提示#输出 格式
    private static final Pattern MODULE_VALUE_PATTERN = Pattern.compile("\\{\\{([^#]*?)#([^#]*?)\\}\\}");

    private static final Pattern START_INPUT_PATTERN = Pattern.compile("\\{\\{开始输入#(.*?)\\}\\}");

    private static final Pattern CUSTOM_INPUT_PATTERN = Pattern.compile("\\{\\{([^#]*?)\\}\\}");

    @Value("${CONSUMER_IF_ELSE_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize(){
        return MoreObjects.firstNonNull(consumerBatchSize,super.getConsumerBatchSize());
    }


    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {

        JSONObject inputs = workflowRunResult.getInputs();
        // 获取输入参数
        if (MapUtils.isEmpty(inputs)) {
            LOG.error("条件判断组件输入为空 workflowRunResultId={}", workflowRunResult.getId());
            Response<List<JSONObject>> errorResponse = Response.error(null);
            errorResponse.getMessage().setMessage("条件判断组件输入为空");
            return errorResponse;
        }
        JSONObject originalInputs = WorkflowUtil.getInputsByTemplate(inputs);
        if (MapUtils.isNotEmpty(workflowRunResult.getData()) && CollectionUtils.isNotEmpty(workflowRunResult.getData().getJSONArray(WorkflowJSONKey.nodesHandle.name()))) {
            originalInputs = WorkflowUtil.getInputsByTemplate(workflowRunResult.getData().getList(WorkflowJSONKey.nodesHandle.name(), Node.class).getFirst().getData().getTemplate());
        }
        //上游节点的所有输出结果和文档输出组件的定义变量合并后的参数
        JSONObject parameters = DataParametersUtil.mergeParameters(originalInputs, inputs);
        // LOG.info("所有参数 {}", JSON.toJSONString(parameters));

        // 获取条件配置
        JSONObject conditionsParam = DataParametersUtil.getParameterByTemplate("if_else_template", inputs);
        if (MapUtils.isEmpty(conditionsParam)) {
            LOG.error("条件判断组件配置为空 workflowRunResultId={}", workflowRunResult.getId());
            Response<List<JSONObject>> errorResponse = Response.error(null);
            errorResponse.getMessage().setMessage("条件判断组件配置为空");
            return errorResponse;
        }

        // 获取条件列表
        JSONArray conditions = conditionsParam.getJSONArray("value");
        // LOG.info("条件列表 {}", JSON.toJSONString(conditions));
        if (conditions == null || conditions.isEmpty()) {
            LOG.error("条件判断组件条件列表为空 workflowRunResultId={}", workflowRunResult.getId());
            Response<List<JSONObject>> errorResponse = Response.error(null);
            errorResponse.getMessage().setMessage("条件判断组件条件列表为空");
            return errorResponse;
        }

        // 遍历条件，找到第一个满足的条件
        String outputValue = null;
        String branchType = null;
        String branchId = null;
        String originalEl = null;
        String parsedEl = null;

        for (int i = 0; i < conditions.size(); i++) {
            JSONObject condition = conditions.getJSONObject(i);
            branchType = condition.getString("branchType");
            branchId = condition.getString("branchId");

            
            // 如果是else分支，直接返回输出
            if ("else".equals(branchType)) {
                outputValue = "true";
                originalEl = null;
                parsedEl = null;
                LOG.info("条件判断组件执行else分支 workflowRunResultId={}", workflowRunResult.getId());
                break;
            }
            
            // 对于if和else_if分支，需要判断条件是否满足
            originalEl = condition.getString("condition");
            if (StringUtils.isBlank(originalEl)) {
                continue;
            }

            // conditionExpr 变量替换
            parsedEl = getParsedEl(originalEl, parameters);

            // 使用FreeMarker解析条件表达式
            try {
                boolean conditionResult = evaluateCondition(parsedEl);
                if (conditionResult) {
                    outputValue = "true";
                    LOG.info("条件判断组件条件满足 workflowRunResultId={}, branchType={}, branchId={}, conditionExpr={}",
                            workflowRunResult.getId(), branchType, branchId, originalEl);
                    break;
                }
            } catch (Exception e) {
                LOG.error("条件判断组件条件表达式解析失败 workflowRunResultId={}, conditionExpr={}", workflowRunResult.getId(), originalEl, e);
                // throw new WorkflowException(String.format("（%s）组件表达式错误", workflowRunResult.getNodeName()));
                Response<List<JSONObject>> errorResponse = Response.error(null);
                errorResponse.getMessage().setMessage(String.format("（%s）条件组件 %s , %s", workflowRunResult.getNodeName(), originalEl, e.getMessage()));
                return errorResponse;
            }

        }

        // 如果没有找到满足的条件，返回空结果
        if (outputValue == null) {
            LOG.warn("条件判断组件没有满足的条件 workflowRunResultId={}", workflowRunResult.getId());
            // outputValue = "false";
            // branchType = "none";
            // branchId = "none";
        }

        // 创建输出
        List<JSONObject> outputs = Lists.newArrayList();
        Condition condition = new Condition();
        condition.setBranchType(branchType);
        condition.setBranchId(branchId);
        condition.setOriginalEl(originalEl);
        condition.setParsedEl(parsedEl);
        condition.setOutput(outputValue);
        
        // 创建主要输出
        JSONObject output = WorkflowUtil.createOutput(WorkflowJSONKey.output.name(),  WorkflowJSONKey.dict.name(), condition);
        outputs.add(output);
        return Response.ok(outputs);
    }

    /**
     * 解析原始EL表达式并替换为相应的值
     *
     * @param originalEl 原始的EL表达式字符串
     * @param parameters 包含替换值的参数对象
     * @return 解析后的EL表达式字符串，如果没有可替换的值，则返回原始EL表达式
     */
    protected @Nullable String getParsedEl(String originalEl, JSONObject parameters) throws RuntimeException {
        String parsedEl = null;
        if (StringUtils.isBlank(originalEl)) {
            return null;
        }

        // 第一种模式：{{开始输入#xxx}}
        Matcher startInputMatcher = START_INPUT_PATTERN.matcher(originalEl);
        while (startInputMatcher.find()) {
            try {
                String key = startInputMatcher.group(1);
                if (parameters.containsKey(key)) {
                    String value = parameters.getJSONObject(key).getString("value");
                    parsedEl = originalEl.replace("{{开始输入#" + key + "}}", value);
                    originalEl = originalEl.replace("{{开始输入#" + key + "}}", value);
                }
            } catch (Exception e) {
                LOG.error("start input condition replace render data error", e);
                throw new RuntimeException("开始输入变量解析出错", e);
            }
        }

        // 第二种模式：{{xxx#yyy}}
        if (StringUtils.isBlank(parsedEl) || parsedEl.contains("{{")) {
            Matcher moduleValueMatcher = MODULE_VALUE_PATTERN.matcher(originalEl);
            while (moduleValueMatcher.find()) {
                try {
                    String key1 = moduleValueMatcher.group(1);
                    String key2 = moduleValueMatcher.group(2);
                    String moduleKey = key1 + "#" + key2;
                    if (parameters.containsKey(moduleKey)) {
                        String value = parameters.getJSONObject(moduleKey).getString("value");
                        parsedEl = originalEl.replace("{{" + key1 + "#" + key2 + "}}", value);
                        originalEl = originalEl.replace("{{" + key1 + "#" + key2 + "}}", value);
                    }
                } catch (Exception e) {
                    LOG.error("moduleValue render data error", e);
                    throw new RuntimeException("模块输入变量解析出错", e);
                }
            }
        }

        // 第三种模式：{{xxx#yyy#$1#field}}
        if (StringUtils.isBlank(parsedEl) || parsedEl.contains("{{")) {
            Matcher matcher = VALUE_PATTERN.matcher(originalEl);
            String loopKey = null;
            String jsonPath = null;
            String fullMatchedStr = null;
            while (matcher.find()) {
                String key = matcher.group(1);
                String key2 = matcher.group(2);
                loopKey = key + "#" + key2;

                int index = Integer.parseInt(matcher.group(3)) - 1;
                String field = matcher.group(4);
                jsonPath = String.format("$[%d].%s", index, field);

                fullMatchedStr = "{{" + key + "#" + key2 + "#$" + matcher.group(3) + "#" + field + "}}";

                if (StringUtils.isNotBlank(loopKey) && StringUtils.isNotBlank(jsonPath) && StringUtils.isNotBlank(fullMatchedStr)) {
                    try {
                        if (parameters.containsKey(loopKey)) {
                            JSONArray parameterValue = parameters.getJSONObject(loopKey).getJSONArray("value");
                            Object eval = JSONPath.eval(parameterValue, jsonPath);
                            String value = eval == null ? "" : JsonUtils.toJSONStringWithArm(eval);
                            parsedEl = originalEl.replace(fullMatchedStr, value);
                            originalEl = originalEl.replace(fullMatchedStr, value);
                        }
                    } catch (Exception e) {
                        LOG.error("list field render data error", e);
                        throw new RuntimeException("列表输入变量解析出错", e);
                    }
                }
            }


        }
        // 第三种模式：{{xxx#$1#field}}
        if (StringUtils.isBlank(parsedEl) || parsedEl.contains("{{")) {
            Matcher matcher = CUSTOM_VALUE_PATTERN.matcher(originalEl);
            String loopKey = null;
            String jsonPath = null;
            String fullMatchedStr = null;
            while (matcher.find()) {
                String key = matcher.group(1);
                loopKey = key;

                int index = Integer.parseInt(matcher.group(2)) - 1;
                String field = matcher.group(3);
                jsonPath = String.format("$[%d].%s", index, field);

                fullMatchedStr = "{{" + key + "#$" + matcher.group(2) + "#" + field + "}}";

                if (StringUtils.isNotBlank(loopKey) && StringUtils.isNotBlank(jsonPath) && StringUtils.isNotBlank(fullMatchedStr)) {
                    try {
                        if (parameters.containsKey(loopKey)) {
                            JSONArray parameterValue = parameters.getJSONObject(loopKey).getJSONArray("value");
                            Object eval = JSONPath.eval(parameterValue, jsonPath);
                            String value = eval == null ? "" : JsonUtils.toJSONStringWithArm(eval);
                            parsedEl = originalEl.replace(fullMatchedStr, value);
                            originalEl = originalEl.replace(fullMatchedStr, value);
                        }
                    } catch (Exception e) {
                        LOG.error("list field render data error", e);
                        throw new RuntimeException("列表输入变量解析出错", e);
                    }
                }
            }


        }
        if (StringUtils.isBlank(parsedEl) || parsedEl.contains("{{")) {
            Matcher customInputMatcher = CUSTOM_INPUT_PATTERN.matcher(originalEl);
            while (customInputMatcher.find()) {
                try {
                    String key = customInputMatcher.group(1);
                    if (parameters.containsKey(key)) {
                        String value = parameters.getJSONObject(key).getString("value");
                        parsedEl = originalEl.replace("{{" + key + "}}", value);
                        originalEl = originalEl.replace("{{" + key + "}}", value);
                    }
                } catch (Exception e) {
                    LOG.error("start input condition replace render data error", e);
                    throw new RuntimeException("自定义输入变量解析出错", e);
                }
            }
        }

        return parsedEl != null ? parsedEl : originalEl;
    }


    /**
     * 使用FreeMarker解析条件表达式
     */
    private boolean evaluateCondition(String conditionExpr) throws RuntimeException {
        if (StringUtils.isEmpty(conditionExpr)){
            LOG.warn("条件表达示为空 {}", conditionExpr);
            throw new RuntimeException("表达式不能为空。");
        }
        // 创建数据模型，用于FreeMarker模板解析
        Map<String, Object> dataModel = new HashMap<>();
        try {
            // 创建FreeMarker配置
            Configuration cfg = new Configuration(new Version(2, 3, 31));
            cfg.setDefaultEncoding("UTF-8");
            
            // 构建完整的条件表达式模板
            String templateString = "<#if " + conditionExpr + ">true<#else>false</#if>";
            
            // 创建模板
            Template template = new Template("conditionTemplate", templateString, cfg);
            
            // 渲染模板
            StringWriter out = new StringWriter();
            template.process(dataModel, out);
            
            // 获取结果
            String result = out.toString();
            return "true".equals(result);
        } catch (IOException | TemplateException e) {
            LOG.error("解析条件表达式失败: {}", conditionExpr, e);
            throw new RuntimeException("表达式解析出错，请检查语法是否正确。", e);
        }
    }
}