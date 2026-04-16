package com.gemantic.workflow.consumer;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 新格式条件模板测试示例
 */
public class ConditionTemplateTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(ConditionTemplateTest.class);
    
    /**
     * 测试新格式条件模板的转换
     */
    @Test
    public void testNewFormatConditionTemplate() {
        // 创建新格式的条件模板
        JSONObject conditionTemplate = createNewFormatConditionTemplate();
        
        // 创建 mainArray
        JSONArray mainArray = new JSONArray();
        mainArray.add(conditionTemplate);
        
        LOG.info("原始 mainArray: {}", mainArray.toJSONString());
        
        // 这里应该调用 DocOutput 的 processNewFormatConditionTemplate 方法
        // JSONArray processedArray = docOutput.processNewFormatConditionTemplate(mainArray);
        
        // LOG.info("处理后 mainArray: {}", processedArray.toJSONString());
    }
    
    /**
     * 创建新格式的条件模板示例
     */
    private JSONObject createNewFormatConditionTemplate() {
        JSONObject template = new JSONObject();
        template.put("type", "conditionTemplate");
        
        // 设置属性
        JSONObject attrs = new JSONObject();
        attrs.put("id", "condition-position-001");
        attrs.put("sourceId", "group-id-001");
        attrs.put("backgroundColor", "#f9f0ff");
        template.put("attrs", attrs);
        
        // 创建内容
        JSONArray content = new JSONArray();
        
        // 创建段落
        JSONObject paragraph = new JSONObject();
        paragraph.put("type", "paragraph");
        
        JSONObject paraAttrs = new JSONObject();
        paraAttrs.put("id", "para-uuid-001");
        paraAttrs.put("sourceId", "group-id-001");
        paraAttrs.put("position", "condition-position-001");
        paragraph.put("attrs", paraAttrs);
        
        // 创建文本节点
        JSONArray paraContent = new JSONArray();
        JSONObject textNode = new JSONObject();
        textNode.put("type", "text");
        textNode.put("text", 
            "<#if \"{{开始输入#是否VIP}}\" == \"是\" || \"{{ETL#原始数据#$1#订单状态}}\"?contains(\"已退款\") || \"{{SQL#原始数据#$1#支付日期}}\"?? >\n" +
            "支持引用开始输入、ETL、SQL数据，进行文本比较，包含 ==、?contains、??\n" +
            "<#elseif {{开始输入#订单ID}} == 42 || {{ETL#原始数据#$1#订单金额}} lt 600 && {{SQL#原始数据#$1#支付金额}} lt 800>\n" +
            "也支持数字比较，包括：==、gt、gte、lt、lte\n" +
            "<#elseif (true || !false ) && false>\n" +
            "支持布尔操作\n" +
            "<#else>\n" +
            "也支持else\n" +
            "</#if>");
        paraContent.add(textNode);
        
        paragraph.put("content", paraContent);
        content.add(paragraph);
        
        template.put("content", content);
        
        return template;
    }
    
    /**
     * 创建预期的旧格式输出
     */
    private JSONArray createExpectedOldFormat() {
        JSONArray result = new JSONArray();
        
        // 开始标记
        JSONObject startMarker = new JSONObject();
        startMarker.put("type", "text");
        startMarker.put("isTemplate", "condition");
        startMarker.put("isCondition", "true");
        startMarker.put("conditionDirection", "start");
        startMarker.put("value", 
            "↪<#if \"{{开始输入#是否VIP}}\" == \"是\" || \"{{ETL#原始数据#$1#订单状态}}\"?contains(\"已退款\") || \"{{SQL#原始数据#$1#支付日期}}\"?? >\n" +
            "支持引用开始输入、ETL、SQL数据，进行文本比较，包含 ==、?contains、??\n" +
            "<#elseif {{开始输入#订单ID}} == 42 || {{ETL#原始数据#$1#订单金额}} lt 600 && {{SQL#原始数据#$1#支付金额}} lt 800>\n" +
            "也支持数字比较，包括：==、gt、gte、lt、lte\n" +
            "<#elseif (true || !false ) && false>\n" +
            "支持布尔操作\n" +
            "<#else>\n" +
            "也支持else\n" +
            "</#if>↩");
        startMarker.put("position", "condition-position-001");
        
        JSONArray groupIds = new JSONArray();
        groupIds.add("group-id-001");
        startMarker.put("groupIds", groupIds);
        startMarker.put("backgroundColor", "#f9f0ff");
        
        result.add(startMarker);
        
        // 结束标记
        JSONObject endMarker = new JSONObject();
        endMarker.put("type", "text");
        endMarker.put("isTemplate", "condition");
        endMarker.put("isCondition", "true");
        endMarker.put("conditionDirection", "end");
        endMarker.put("value", "");
        endMarker.put("position", "condition-position-001");
        endMarker.put("groupIds", groupIds);
        
        result.add(endMarker);
        
        return result;
    }
    
    /**
     * 测试简单的 if-else 条件
     */
    @Test
    public void testSimpleIfElse() {
        JSONObject template = new JSONObject();
        template.put("type", "conditionTemplate");
        
        JSONObject attrs = new JSONObject();
        attrs.put("id", "simple-condition");
        template.put("attrs", attrs);
        
        JSONArray content = new JSONArray();
        JSONObject paragraph = new JSONObject();
        paragraph.put("type", "paragraph");
        
        JSONArray paraContent = new JSONArray();
        JSONObject textNode = new JSONObject();
        textNode.put("type", "text");
        textNode.put("text", "<#if \"{{开始输入#是否VIP}}\" == \"是\">VIP用户<#else>普通用户</#if>");
        paraContent.add(textNode);
        
        paragraph.put("content", paraContent);
        content.add(paragraph);
        template.put("content", content);
        
        LOG.info("简单条件模板: {}", template.toJSONString());
    }
    
    /**
     * 测试数字比较条件
     */
    @Test
    public void testNumberComparison() {
        JSONObject template = new JSONObject();
        template.put("type", "conditionTemplate");
        
        JSONObject attrs = new JSONObject();
        attrs.put("id", "number-condition");
        template.put("attrs", attrs);
        
        JSONArray content = new JSONArray();
        JSONObject paragraph = new JSONObject();
        paragraph.put("type", "paragraph");
        
        JSONArray paraContent = new JSONArray();
        JSONObject textNode = new JSONObject();
        textNode.put("type", "text");
        textNode.put("text", 
            "<#if {{ETL#原始数据#$1#订单金额}} gte 1000>\n" +
            "大额订单\n" +
            "<#elseif {{ETL#原始数据#$1#订单金额}} gte 500>\n" +
            "中等订单\n" +
            "<#else>\n" +
            "小额订单\n" +
            "</#if>");
        paraContent.add(textNode);
        
        paragraph.put("content", paraContent);
        content.add(paragraph);
        template.put("content", content);
        
        LOG.info("数字比较条件模板: {}", template.toJSONString());
    }
}
