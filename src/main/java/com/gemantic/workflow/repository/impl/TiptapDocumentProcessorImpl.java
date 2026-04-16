package com.gemantic.workflow.repository.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.gemantic.utils.ElementFormatterUtil;
import com.gemantic.utils.JsonUtils;
import com.gemantic.utils.MarkdownToTiptapConverter;
import com.gemantic.workflow.repository.TiptapDocumentProcessor;
import com.gemantic.workflow.util.HeadingNumberingUtil;
import org.apache.commons.collections4.CollectionUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.StringWriter;
import java.io.IOException;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

import static com.gemantic.utils.ElementFormatterUtil.getValueByVariableName;
import static com.gemantic.utils.ElementFormatterUtil.getloopKeyByVariableName;
import com.gemantic.utils.ElementFormatterUtil.NodeData;


/**
 * Tiptap 在线文档处理器实现类
 * 用于处理新的 Tiptap 文档类型 (type="doc")
 * 输入: parameters (JSONObject)
 * 输出: List<JSONObject>
 */
@Repository
public class TiptapDocumentProcessorImpl implements TiptapDocumentProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TiptapDocumentProcessorImpl.class);

    // 变量替换的正则表达式模式
    private static final String VALUE_PATTERN = "\\{\\{(.*?)#(.*?)#(.*?)#(.*?)\\}\\}";
    private static final String MODULE_VALUE_PATTERN_STR = "\\{\\{(.*?)#(.*?)\\}\\}";
    private static final String START_INPUT_PATTERN_STR = "\\{\\{开始输入#(.*?)\\}\\}";
    private static final Pattern MARKDOWN_TABLE_ROW_PATTERN = Pattern.compile("(?m)^\\s*\\|.+\\|\\s*$");
    private static final Pattern MARKDOWN_TABLE_SEPARATOR_PATTERN = Pattern.compile("(?m)^\\s*\\|?\\s*:?-{3,}:?(\\s*\\|\\s*:?-{3,}:?)+\\s*\\|?\\s*$");

    // 条件模板的正则表达式模式
    private static final String CONDITION_VALUE_PATTERN = "\\{\\{([^#\\{\\}]+)#([^#\\{\\}]+)#([^#\\{\\}]+)#([^#\\{\\}]+)\\}\\}";
    private static final String CONDITION_START_INPUT_PATTERN_STR = "\\{\\{开始输入#([^#\\{\\}]+)\\}\\}";
    private static final String CONDITION_EXIST_PATTERN_STR = "\\{\\{([^#\\{\\}]+)#([^#\\{\\}]+)#([^#\\{\\}]+)#([^#\\{\\}]+)\\}\\}\\?\\?";
    private static final String CONDITION_START_EXIST_PATTERN_STR = "\\{\\{开始输入#([^#\\{\\}]+)\\}\\}\\?\\?";

    private static final Pattern PATTERN = Pattern.compile(VALUE_PATTERN);
    private static final Pattern MODULE_VALUE_PATTERN = Pattern.compile(MODULE_VALUE_PATTERN_STR);
    private static final Pattern START_INPUT_PATTERN = Pattern.compile(START_INPUT_PATTERN_STR);

    // 条件模板的正则表达式 Pattern
    private static final Pattern CONDITION_PATTERN = Pattern.compile(CONDITION_VALUE_PATTERN);
    private static final Pattern CONDITION_START_INPUT_PATTERN = Pattern.compile(CONDITION_START_INPUT_PATTERN_STR);
    private static final Pattern CONDITION_EXIST_PATTERN = Pattern.compile(CONDITION_EXIST_PATTERN_STR);
    private static final Pattern CONDITION_START_EXIST_PATTERN = Pattern.compile(CONDITION_START_EXIST_PATTERN_STR);

    @Override
    public List<JSONObject> process(JSONObject parameters) {
        LOG.info("开始处理 Tiptap 文档类型, parameters: {}", parameters);

        List<JSONObject> result = new ArrayList<>();

        try {
            // 获取文档模板
            JSONObject template = parameters.getJSONObject("content");
            if (template == null) {
                LOG.warn("未找到 content 字段");
                return result;
            }

            JSONObject value = template.getJSONObject("value");
            if (value == null) {
                LOG.warn("未找到 value 字段");
                return result;
            }

            JSONObject data = value.getJSONObject("data");
            if (data == null) {
                LOG.warn("未找到 data 字段");
                return result;
            }

            String type = data.getString("type");
            if (!"doc".equals(type)) {
                LOG.warn("文档类型不是 doc, 实际类型: {}", type);
                return result;
            }

            // 获取文档内容
            JSONArray content = data.getJSONArray("content");
            if (content == null) {
                LOG.warn("未找到 content 数组");
                return result;
            }

            // 兼容旧模版：顶层仅 conditionTemplate 时在末尾追加空段落占位
            ensurePlaceholderParagraphWhenOnlyConditionTemplate(content);

            LOG.info("Tiptap 文档包含 {} 个内容节点", content.size());

            for (int i = 0; i < content.size(); i++) {
                JSONObject item = content.getJSONObject(i);
                if (item != null) {
                    JSONArray processedItems = processContentNode(item, parameters);
                    if (processedItems != null) {
                        // 将返回的所有节点添加到结果中（支持一个节点被替换为多个节点）
                        for (int j = 0; j < processedItems.size(); j++) {
                            result.add(processedItems.getJSONObject(j));
                        }
                    }
                }
            }


            adjustColonUnderlineTextLengths(result);

            // ------------------ 标题 -------------
            HeadingNumberingUtil.applyHeadingNumberingIfEnabled(result, parameters);
            // 全局兜底：文档处理完成后，再补一次 Markdown 残留转换（如表格未被转成 Tiptap table）
            convertResidualMarkdownBlocks(result);

            LOG.info("Tiptap 文档处理完成, 输出 {} 个节点", result.size());

        } catch (Exception e) {
            LOG.error("处理 Tiptap 文档时发生错误", e);
        }

        return result;
    }


    private void adjustUnderlineTextLengths(List<JSONObject> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }


        int maxLength = 0;
        List<JSONObject> underlineTextNodes = new ArrayList<>();

        for (JSONObject node : nodes) {
            if ("paragraph".equals(node.getString("type"))) {
                JSONArray content = node.getJSONArray("content");
                if (content != null) {
                    for (int i = 0; i < content.size(); i++) {
                        JSONObject item = content.getJSONObject(i);
                        if (item != null && "text".equals(item.getString("type")) && hasUnderlineMark(item)) {
                            String text = item.getString("text");
                            if (text != null) {
                                maxLength = Math.max(maxLength, text.length());
                                underlineTextNodes.add(item);
                            }
                        }
                    }
                }
            }
        }


        if (maxLength > 0) {
            for (JSONObject node : underlineTextNodes) {
                String text = node.getString("text");
                if (text != null) {
                    int currentLength = text.length();
                    if (currentLength < maxLength) {
                        StringBuilder paddedText = new StringBuilder(text);
                        for (int i = 0; i < maxLength - currentLength; i++) {
                            paddedText.append(" ");
                        }
                        node.put("text", paddedText.toString());
                    }
                }
            }
        }
    }


    private void adjustColonUnderlineTextLengths(List<JSONObject> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }


        List<JSONObject> underlineTextNodes = new ArrayList<>();

        for (JSONObject node : nodes) {
            if ("paragraph".equals(node.getString("type"))) {
                JSONArray content = node.getJSONArray("content");
                if (content != null) {
                    boolean foundColon = false;
                    for (int i = 0; i < content.size(); i++) {
                        JSONObject item = content.getJSONObject(i);
                        if (item != null && "text".equals(item.getString("type"))) {
                            String text = item.getString("text");
                            if (!foundColon && text != null) {
                                if (text.contains("：") || text.contains(":")) {
                                    foundColon = true;
                                }
                            } else if (foundColon && hasUnderlineMark(item)) {
                                underlineTextNodes.add(item);
                            }
                        }
                    }
                }
            }
        }


        double maxWidth = 0;
        for (JSONObject item : underlineTextNodes) {
            String text = item.getString("text");
            if (text != null) {
                double width = calculateDisplayWidth(text);
                maxWidth = Math.max(maxWidth, width);
            }
        }

        if (maxWidth > 0) {
            for (JSONObject node : underlineTextNodes) {
                String text = node.getString("text");
                if (text != null) {
                    double currentWidth = calculateDisplayWidth(text);
                    if (currentWidth < maxWidth) {
                        double widthDiff = maxWidth - currentWidth;
                        int spacesToAdd = (int) Math.ceil(widthDiff);
                        
                        StringBuilder paddedText = new StringBuilder(text);
                        for (int i = 0; i < spacesToAdd; i++) {
                            paddedText.append(" ");
                        }
                        node.put("text", paddedText.toString());
                        LOG.debug("补齐下划线: 原文本='{}', 原宽度={}, 目标宽度={}, 补齐{}个空格", 
                                text, currentWidth, maxWidth, spacesToAdd);
                    }
                }
            }
        }
    }


    private double calculateDisplayWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        double width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCJKCharacter(c) || isFullWidthCharacter(c)) {
                width += 2;
            } else {
                width += 1;
            }
        }
        return width;
    }


    private boolean isCJKCharacter(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT;
    }

    private boolean isFullWidthCharacter(char c) {
        return (c >= 0xFF01 && c <= 0xFF5E)
                || (c >= 0x3000 && c <= 0x303F);
    }


    private void convertResidualMarkdownBlocks(List<JSONObject> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        int index = 0;
        while (index < nodes.size()) {
            JSONObject node = nodes.get(index);
            if (node == null) {
                index++;
                continue;
            }
            JSONArray replacement = convertResidualMarkdownInNode(node);
            if (replacement != null && !replacement.isEmpty()) {
                nodes.remove(index);
                for (int i = 0; i < replacement.size(); i++) {
                    nodes.add(index + i, replacement.getJSONObject(i));
                }
                index += replacement.size();
            } else {
                index++;
            }
        }
    }

    private void convertResidualMarkdownBlocksInArray(JSONArray content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        int index = 0;
        while (index < content.size()) {
            JSONObject child = content.getJSONObject(index);
            if (child == null) {
                index++;
                continue;
            }
            JSONArray replacement = convertResidualMarkdownInNode(child);
            if (replacement != null && !replacement.isEmpty()) {
                content.remove(index);
                for (int i = 0; i < replacement.size(); i++) {
                    content.add(index + i, replacement.getJSONObject(i));
                }
                index += replacement.size();
            } else {
                index++;
            }
        }
    }

    private JSONArray convertResidualMarkdownInNode(JSONObject node) {
        JSONArray content = node.getJSONArray("content");
        if (content != null && !content.isEmpty()) {
            convertResidualMarkdownBlocksInArray(content);
        }

        if (!"paragraph".equals(node.getString("type"))) {
            return null;
        }
        String markdownText = extractParagraphTextAsTableCandidate(node);
        if (!isMarkdownTableText(markdownText)) {
            return null;
        }

        String sourceId = null;
        String position = null;
        JSONObject attrs = node.getJSONObject("attrs");
        if (attrs != null) {
            sourceId = attrs.getString("sourceId");
            position = attrs.getString("position");
        }

        var tiptapDocument = MarkdownToTiptapConverter.convertMarkdownToTiptap(
                markdownText,
                MarkdownToTiptapConverter.ConvertOptions.builder()
                        .sourceId(sourceId)
                        .position(position)
                        .build()
        );
        if (tiptapDocument == null || tiptapDocument.get("content") == null) {
            return null;
        }

        try {
            return JSONArray.from(tiptapDocument.get("content"));
        } catch (Exception e) {
            LOG.warn("全局 markdown 残留转换失败，保留原段落", e);
            return null;
        }
    }


    private String extractParagraphTextAsTableCandidate(JSONObject paragraphNode) {
        JSONArray content = paragraphNode.getJSONArray("content");
        if (content == null || content.isEmpty()) {
            return null;
        }
        if (content.size() != 1) {
            return null;
        }
        JSONObject item = content.getJSONObject(0);
        if (item == null || !"text".equals(item.getString("type"))) {
            return null;
        }
        String text = item.getString("text");
        return StringUtils.isBlank(text) ? null : text;
    }

    private boolean isMarkdownTableText(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        return MARKDOWN_TABLE_ROW_PATTERN.matcher(text).find()
                && MARKDOWN_TABLE_SEPARATOR_PATTERN.matcher(text).find();
    }

    private String extractModuleKeyFromVariableText(String variableText) {
        if (StringUtils.isBlank(variableText)) {
            return null;
        }
        Matcher matcher = PATTERN.matcher(variableText);
        if (matcher.find()) {
            return matcher.group(1) + "#" + matcher.group(2);
        }
        Matcher moduleMatcher = MODULE_VALUE_PATTERN.matcher(variableText);
        if (moduleMatcher.find()) {
            return moduleMatcher.group(1) + "#" + moduleMatcher.group(2);
        }
        return null;
    }

    /**
     * 兼容旧模版：若顶层 content 下所有节点类型均为 conditionTemplate（无 paragraph、heading 等），
     * 则在末尾追加一个空段落，避免条件都不满足时整篇文档无任何内容。
     * 仅遍历 data.content 第一层，不关心子节点。
     *
     * @param content data.content 数组，可能被追加一个 paragraph 节点
     */
    private void ensurePlaceholderParagraphWhenOnlyConditionTemplate(JSONArray content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        for (int i = 0; i < content.size(); i++) {
            JSONObject item = content.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String nodeType = item.getString("type");
            if (!"conditionTemplate".equals(nodeType)) {
                return;
            }
        }
        LOG.info("顶层 content 仅包含 conditionTemplate 节点，追加一个空段落作为占位");
        JSONObject paragraph = new JSONObject();
        paragraph.put("type", "paragraph");
        JSONObject attrs = new JSONObject();
        attrs.put("id", java.util.UUID.randomUUID().toString());
        attrs.put("sourceId", (Object) null);
        attrs.put("position", (Object) null);
        attrs.put("diffStatus", (Object) null);
        attrs.put("textAlign", (Object) null);
        attrs.put("lineHeight", (Object) null);
        paragraph.put("attrs", attrs);
        paragraph.put("content", new JSONArray());
        content.add(paragraph);
    }

    /**
     * 处理单个内容节点
     * @param node 内容节点
     * @param parameters 参数对象
     * @return 处理后的节点数组（可能是一个或多个节点）
     */
    private JSONArray processContentNode(JSONObject node, JSONObject parameters) {
        if (node == null) {
            return null;
        }

        String nodeType = node.getString("type");
        LOG.info("处理节点类型: {}", nodeType);

        JSONObject processedNode = new JSONObject();
        processedNode.putAll(node);

        // 根据不同的节点类型进行处理：表格、图片、文本（其他所有类型）
        switch (nodeType) {
            case "table":
                Map<String, JSONObject> keyDataMap = new HashMap<>();
                processTable(node, keyDataMap, parameters);
                break;
            case "image":
                JSONArray imageArray = JSONArray.of(node);
                ElementFormatterUtil.formatElementNonTableList(imageArray, parameters);

                if (imageArray.isEmpty()) {
                    return new JSONArray();
                }
                if (imageArray.size() > 0) {
                    return imageArray;
                }
                break;

            case "conditionTemplate":
                try {
                    JSONArray conditionResult = processConditionTemplate(node, parameters);
                    LOG.info("processConditionTemplate 返回结果: size={}", conditionResult != null ? conditionResult.size() : 0);
                    if (conditionResult != null && conditionResult.size() > 0) {
                        return conditionResult;
                    }
                    return new JSONArray();
                } catch (Exception e) {
                    LOG.error("处理 conditionTemplate 时发生异常", e);
                    return new JSONArray();
                }
            default:
                // 文本类型：包括 paragraph, heading, bulletList, orderedList, codeBlock, blockquote 等
                JSONArray replacementNodes = processTextNode(processedNode, parameters);
                if (replacementNodes != null && replacementNodes.size() > 0) {
                    // 如果发生markdown转换，返回替换节点数组
                    return replacementNodes;
                }
                break;
        }

        // 默认返回包含单个节点的数组
        return JSONArray.of(processedNode);
    }

    /**
     * 处理文本类型节点（包括段落、标题、列表、代码块、引用块等）
     * @return 如果发生markdown转换，返回替换节点数组；否则返回null
     */
    private JSONArray processTextNode(JSONObject node, JSONObject parameters) {
        String nodeType = node.getString("type");
        JSONArray content = node.getJSONArray("content");

        if (content == null) {
            return null;
        }

        // 根据节点类型处理内容
        switch (nodeType) {
            case "bulletList":
            case "orderedList":
                // 处理列表项
                for (int i = 0; i < content.size(); i++) {
                    JSONObject listItem = content.getJSONObject(i);
                    if (listItem != null && "listItem".equals(listItem.getString("type"))) {
                        JSONArray itemContent = listItem.getJSONArray("content");
                        if (itemContent != null) {
                            processAndReplaceNodes(itemContent, parameters);
                        }
                    }
                }
                break;
            case "blockquote":
                // 处理引用块内的内容
                processAndReplaceNodes(content, parameters);
                break;
            default:
                // 处理段落、标题、代码块等的内联内容
                JSONArray markDownTranceNodes = processInlineContent(content, parameters);
                LOG.info("processTextNode markDownTranceNodes: {}", markDownTranceNodes);
                if(markDownTranceNodes != null && markDownTranceNodes.size() > 0){
                    // 递归处理markDownTranceNodes之外的其他内容（排除最后一个已处理的节点）
                    for (int i = 0; i < content.size() - 1; i++) {
                        JSONObject item = content.getJSONObject(i);
                        if (item != null) {
                            processContentNode(item, parameters);
                        }
                    }
                    // 返回markDownTranceNodes，由调用方在父数组中替换当前node位置
                    return markDownTranceNodes;
                }
                break;
        }
        return null;
    }

    /**
     * 处理并替换数组中的节点
     * 遍历数组中的每个节点，如果处理后返回多个节点，则替换原位置
     */
    private void processAndReplaceNodes(JSONArray nodeArray, JSONObject parameters) {
        if (nodeArray == null) {
            return;
        }
        for (int i = nodeArray.size() - 1; i >= 0; i--) {
            JSONObject item = nodeArray.getJSONObject(i);
            if (item != null) {
                JSONArray processedNodes = processContentNode(item, parameters);
                if (processedNodes != null && processedNodes.size() > 0) {
                    // 移除原节点
                    nodeArray.remove(i);
                    // 在原位置插入所有处理后的节点
                    for (int j = processedNodes.size() - 1; j >= 0; j--) {
                        nodeArray.add(i, processedNodes.get(j));
                    }
                }
            }
        }
    }

    /**
     * 处理表格节点
     */
    private void processTable(JSONObject node, Map<String, JSONObject> keyDataMap, JSONObject parameters) {
        processNewFormatTable(node, keyDataMap, parameters);
        processConditionTemplatesInTable(node, parameters);
        if (detectBrokenRowspanForLoopRows(node)) {
            LOG.info("表格处理完成后，检测到有断开的合并单元格，开始修复");
            fixBrokenRowspanForLoopRows(node);
        }
        processAllNodesInTable(node, parameters);
        normalizeEmptyTableCells(node);
        cleanupEmptyTextNodesInTable(node);
    }


    /**
     * 处理内联内容 (文本、链接等)
     * @return 如果发生markdown转tiptap处理，返回替换节点数组；否则返回null
     */
    private JSONArray processInlineContent(JSONArray content, JSONObject parameters) {
        if (content == null) {
            return null;
        }

        for (int i = content.size() - 1; i >= 0; i--) {
            JSONObject item = content.getJSONObject(i);
            if (item == null) {
                content.remove(i);
                continue;
            }

            String type = item.getString("type");
            if ("text".equals(type)) {
                processInlineTextNode(item, parameters);
            } else if ("inlineTemplate".equals(type)) {
                Boolean isMarkDownTrance = processInlineTemplate(item, parameters, content);
                LOG.info("processInlineContent isMarkDownTrance: {}, item.content: {}", isMarkDownTrance, item.getJSONArray("content"));
                if(isMarkDownTrance){
                    // 返回markdown转换后的tiptap块级节点数组
                    return item.getJSONArray("content");
                }
                // 检查是否需要删除该节点（替换后为空）
                if (item.getBooleanValue("_shouldRemove")) {
                    content.remove(i);
                }
            }
        }


        mergeConsecutiveUnderlinedTextNodes(content);

        return null;
    }

    private void mergeConsecutiveUnderlinedTextNodes(JSONArray content) {
        if (content == null || content.size() < 2) {
            return;
        }

        int i = 0;
        while (i < content.size() - 1) {
            JSONObject currentNode = content.getJSONObject(i);
            JSONObject nextNode = content.getJSONObject(i + 1);

            if (currentNode != null && nextNode != null && "text".equals(currentNode.getString("type")) && "text".equals(nextNode.getString("type"))) {
                boolean currentHasUnderline = hasUnderlineMark(currentNode);
                boolean nextHasUnderline = hasUnderlineMark(nextNode);

                if (currentHasUnderline && nextHasUnderline) {
                    String currentText = currentNode.getString("text") != null ? currentNode.getString("text") : "";
                    String nextText = nextNode.getString("text") != null ? nextNode.getString("text") : "";

                    currentNode.put("text", currentText + nextText);

                    content.remove(i + 1);
                } else {
                    i++;
                }
            } else {
                i++;
            }
        }
    }

    private boolean hasUnderlineMark(JSONObject node) {
        if (node == null) {
            return false;
        }

        JSONArray marks = node.getJSONArray("marks");
        if (marks == null || marks.isEmpty()) {
            return false;
        }

        for (int i = 0; i < marks.size(); i++) {
            JSONObject mark = marks.getJSONObject(i);
            if (mark != null && "underline".equals(mark.getString("type"))) {
                return true;
            }
        }

        return false;
    }

    /**
     * 处理内联文本节点（type="text"）
     */
    private void processInlineTextNode(JSONObject node, JSONObject parameters) {
        String text = node.getString("text");
        if (text != null) {
            // 处理文本的变量替换
            ReplacementResult result = replaceVariables(text, parameters, false, null);
            if (result != null) {
                node.put("text", result.getText());
                // 应用 sourceId 和 position 到 marks（使用 textStyle 类型）
                applySourceIdToMarks(node, result.getGroupIds());
                // 如果有Tiptap文档对象，放到node的content中
                if (result.getTiptapContent() != null) {
                    node.put("content", result.getTiptapContent().get("content"));
                }
            }
        }
    }

    /**
     * 处理内联模板节点,并且返回是否进行了markdown转tiptap的操作
     * @param node 当前节点
     * @param parameters 参数对象
     * @param parentContent 父级content数组，用于判断是否是块级元素
     */
    private Boolean processInlineTemplate(JSONObject node, JSONObject parameters, JSONArray parentContent) {
        JSONObject attrs = node.getJSONObject("attrs");
        if (attrs == null) {
            return false;
        }

        // 检查是否是模板 (isTemplate 可能是布尔值或字符串)
        Object isTemplateObj = attrs.get("isTemplate");
        // 判断是否是块级元素：如果父级content中没有text、radio、checkbox类型的兄弟节点，则是块级
        boolean isBlockLevel = isBlockLevelElement(parentContent);
        boolean isTemplate = false;

        if (isTemplateObj instanceof Boolean) {
            isTemplate = (Boolean) isTemplateObj;
        } else if (isTemplateObj instanceof String) {
            // 如果是字符串，只要不为空就认为是模板
            isTemplate = StringUtils.isNotBlank((String) isTemplateObj);
        }

        if (isTemplate) {
            // 是模板，需要替换变量
            JSONArray content = node.getJSONArray("content");
            if (content != null && content.size() > 0) {
                // 获取父节点的 id 作为 position
                String position = attrs.getString("id");

                // 先合并所有 text 节点的文本（处理变量被拆分到多个节点的情况）
                StringBuilder combinedText = new StringBuilder();
                JSONArray firstNodeMarks = null;
                for (int i = 0; i < content.size(); i++) {
                    JSONObject item = content.getJSONObject(i);
                    if (item != null && "text".equals(item.getString("type"))) {
                        String text = item.getString("text");
                        if (text != null) {
                            combinedText.append(text);
                        }
                        // 保留第一个节点的 marks
                        if (firstNodeMarks == null) {
                            firstNodeMarks = item.getJSONArray("marks");
                        }
                    }
                }

                // 处理合并后的文本，收集 groupIds
                LinkedHashSet<String> allGroupIds = new LinkedHashSet<>();
                Map<String, Object> tiptapContent = null;
                String replacedText = combinedText.toString();

                if (StringUtils.isNotBlank(replacedText)) {
                    ReplacementResult result = replaceVariables(replacedText, parameters, isBlockLevel, position, firstNodeMarks);
                    if (result != null) {
                        replacedText = result.getText();
                        // 收集所有的 groupIds
                        if (result.getGroupIds() != null && !result.getGroupIds().isEmpty()) {
                            allGroupIds.addAll(result.getGroupIds());
                        }
                        // 收集 tiptapContent
                        if (result.getTiptapContent() != null) {
                            tiptapContent = result.getTiptapContent();
                        }
                    }
                }

                // 获取第一个 groupId 作为 sourceId
                String sourceId = allGroupIds.isEmpty() ? null : allGroupIds.iterator().next();

                // 处理子工作流和循环子工作流类型
                String variableName = combinedText.toString();
                NodeData nodeData = getValueByVariableName(parameters, variableName);
                
                // 检查是否是子工作流类型的变量
                boolean isSubWorkflowType = false;
                if (nodeData != null) {
                    String nodeType = nodeData.getType();
                    isSubWorkflowType = StrUtil.equals("sub_workflow", nodeType) || StrUtil.equals("sub_workflow_list", nodeType);
                }
                
                if (nodeData != null && ObjectUtil.isNotEmpty(nodeData.getValue())) {
                    String nodeType = nodeData.getType();

                    // 处理子工作流 (sub_workflow)
                    if (StrUtil.equals("sub_workflow", nodeType)) {
                        JSONArray subWorkflowContent = processSubWorkflow(nodeData);
                        if (subWorkflowContent != null && subWorkflowContent.size() > 0) {
                            LOG.info("processInlineTemplate sub_workflow content size: {}", subWorkflowContent.size());
                            node.put("content", subWorkflowContent);
                            return true;
                        } else {
                            // 子工作流内容为空，将文本替换为空字符串并标记删除
                            LOG.info("processInlineTemplate sub_workflow content is empty, replacing with empty string");
                            replacedText = "";
                            node.put("_shouldRemove", true);
                        }
                    }

                    // 处理循环子工作流 (sub_workflow_list)
                    if (StrUtil.equals("sub_workflow_list", nodeType)) {
                        JSONArray subWorkflowListContent = processSubWorkflowList(nodeData);
                        if (subWorkflowListContent != null && subWorkflowListContent.size() > 0) {
                            LOG.info("processInlineTemplate sub_workflow_list content size: {}", subWorkflowListContent.size());
                            node.put("content", subWorkflowListContent);
                            return true;
                        } else {
                            // 循环子工作流内容为空，将文本替换为空字符串并标记删除
                            LOG.info("processInlineTemplate sub_workflow_list content is empty, replacing with empty string");
                            replacedText = "";
                            node.put("_shouldRemove", true);
                        }
                    }
                } else if (isSubWorkflowType) {
                    // 如果是子工作流类型但值为空，也需要清空文本并标记删除
                    LOG.info("processInlineTemplate sub_workflow type but value is empty, replacing with empty string");
                    replacedText = "";
                    node.put("_shouldRemove", true);
                }

                // 保底：块级模板变量替换后若仍是 markdown 表格文本，强制转换为 Tiptap（避免被当纯文本输出）
                if (tiptapContent == null && isBlockLevel && isMarkdownTableText(replacedText)) {
                    String moduleKey = extractModuleKeyFromVariableText(combinedText.toString());
                    tiptapContent = processMarkdownToTiptap(replacedText, parameters, moduleKey, position, firstNodeMarks);
                }

                // 如果有Tiptap文档对象（块级输出），直接存储到node的content中，不转换为text类型
                if (tiptapContent != null) {
                    LOG.info("processInlineTemplate tiptapContent: {}", tiptapContent.get("content"));
                    node.put("content", tiptapContent.get("content"));
                    return true;
                }

                // 检查是否已经标记为删除（子工作流为空的情况）
                if (node.getBooleanValue("_shouldRemove")) {
                    LOG.info("processInlineTemplate node marked for removal, skipping further processing");
                    return false;
                }

                // 获取 isTemplate 类型
                String templateType = isTemplateObj instanceof String ? (String) isTemplateObj : null;

                // 处理单选框和复选框类型
                if ("radio".equals(templateType) || "checkbox".equals(templateType)) {
                    // 将替换后的文本转换为布尔值
                    boolean checked = "true".equalsIgnoreCase(replacedText.trim())
                            || "1".equals(replacedText.trim())
                            || "是".equals(replacedText.trim())
                            || "yes".equalsIgnoreCase(replacedText.trim());

                    // 转换为 radio 或 checkbox 节点
                    node.put("type", templateType);
                    node.remove("content");

                    // 设置 attrs，包含溯源信息
                    JSONObject newAttrs = new JSONObject();
                    newAttrs.put("checked", checked);
                    newAttrs.put("annotationId", null);
                    newAttrs.put("annotationGroup", null);
                    // 添加溯源信息
                    if (StringUtils.isNotBlank(position)) {
                        newAttrs.put("position", position);
                    }
                    if (StringUtils.isNotBlank(sourceId)) {
                        newAttrs.put("sourceId", sourceId);
                    }
                    node.put("attrs", newAttrs);

                    return false;
                }

                // 使用合并后的文本替换节点（非块级输出）
                // 判断是否发生了替换（原文本包含变量占位符）
                boolean hasVariableReplaced = !combinedText.toString().equals(replacedText);
                if (StringUtils.isNotBlank(replacedText)) {
                    // 计算原文本和替换后文本的长度差异
                    // 从变量占位符中提取字段名称作为参考长度
                    int referenceLength = replacedText.length();
                    String originalText = combinedText.toString();
                    
                    // 尝试从占位符中提取字段名称（最后一个#后面的部分）
                    if (hasVariableReplaced && originalText.contains("{{") && originalText.contains("}}")) {
                        Matcher fieldMatcher = PATTERN.matcher(originalText);
                        if (fieldMatcher.find()) {
                            // 提取字段名称（第4个分组，即最后一个#后面的内容）
                            String fieldName = fieldMatcher.group(4);
                            if (StringUtils.isNotBlank(fieldName)) {
                                referenceLength = fieldName.length();
                                LOG.info("从占位符提取字段名称作为参考长度: fieldName={}, length={}", fieldName, referenceLength);
                            }
                        } else {
                            // 尝试匹配模块变量格式 {{key#value}}
                            Matcher moduleFieldMatcher = MODULE_VALUE_PATTERN.matcher(originalText);
                            if (moduleFieldMatcher.find()) {
                                String fieldName = moduleFieldMatcher.group(2);
                                if (StringUtils.isNotBlank(fieldName)) {
                                    referenceLength = fieldName.length();
                                    LOG.info("从模块变量提取字段名称作为参考长度: fieldName={}, length={}", fieldName, referenceLength);
                                }
                            }
                        }
                    }
                    
                    int replacedLength = replacedText.length();
                    
                    // 如果替换后的文本更短，用空格补齐以保持下划线长度
                    if (replacedLength < referenceLength && hasVariableReplaced) {
                        int paddingLength = referenceLength - replacedLength;
                        StringBuilder padding = new StringBuilder();
                        for (int i = 0; i < paddingLength; i++) {
                            padding.append(" ");
                        }
                        replacedText = replacedText + padding.toString();
                        LOG.info("替换后文本较短，补齐空格: 参考长度={}, 新长度={}, 补齐={}个空格", referenceLength, replacedLength, paddingLength);
                    }
                    
                    // 将节点转换为 text 类型，使用合并后的替换文本
                    node.put("type", "text");
                    node.put("text", replacedText);

                    // 使用第一个节点的 marks
                    if (firstNodeMarks != null) {
                        node.put("marks", firstNodeMarks);
                    } else {
                        node.put("marks", new JSONArray());
                    }

                    // 在 marks 中添加或更新 textStyle，包含 position 和 sourceId
                    if (StringUtils.isNotBlank(position) || StringUtils.isNotBlank(sourceId)) {
                        JSONArray marks = node.getJSONArray("marks");

                        // 查找是否已存在 textStyle mark
                        JSONObject textStyleMark = null;
                        for (int i = 0; i < marks.size(); i++) {
                            JSONObject mark = marks.getJSONObject(i);
                            if (mark != null && "textStyle".equals(mark.getString("type"))) {
                                textStyleMark = mark;
                                break;
                            }
                        }

                        // 如果不存在，创建新的 textStyle mark
                        if (textStyleMark == null) {
                            textStyleMark = new JSONObject();
                            textStyleMark.put("type", "textStyle");
                            JSONObject textStyleAttrs = new JSONObject();
                            textStyleMark.put("attrs", textStyleAttrs);
                            marks.add(textStyleMark);
                        }

                        // 设置 position 和 sourceId
                        JSONObject textStyleAttrs = textStyleMark.getJSONObject("attrs");
                        if (textStyleAttrs == null) {
                            textStyleAttrs = new JSONObject();
                            textStyleMark.put("attrs", textStyleAttrs);
                        }

                        if (StringUtils.isNotBlank(position)) {
                            textStyleAttrs.put("position", position);
                        }
                        if (StringUtils.isNotBlank(sourceId)) {
                            textStyleAttrs.put("sourceId", sourceId);
                        }
                    }

                    // 移除 content 数组和 attrs
                    node.remove("content");
                    node.remove("attrs");
                } else if (hasVariableReplaced) {
                    // 替换后为空字符串，标记该节点需要删除
                    node.put("_shouldRemove", true);
                }
            }
        } else {
            // 不是模板，保持原样，递归处理内容
            JSONArray content = node.getJSONArray("content");
            if (content != null) {
                processInlineContent(content, parameters);
            }
        }
        return false;
    }

    /**
     * 判断是否是块级元素
     * 如果父级content中没有text、radio、checkbox类型的兄弟节点，则是块级元素
     * @param parentContent 父级content数组
     * @return true表示是块级元素，需要转换为Tiptap文档对象
     */
    private boolean isBlockLevelElement(JSONArray parentContent) {
        if (parentContent == null || parentContent.isEmpty()) {
            return true;
        }

        for (int i = 0; i < parentContent.size(); i++) {
            JSONObject sibling = parentContent.getJSONObject(i);
            if (sibling == null) {
                continue;
            }

            String type = sibling.getString("type");
            // 如果存在text类型的兄弟节点，则是行级元素
            if ("text".equals(type)) {
                return false;
            }

            // 检查inlineTemplate节点的isTemplate属性是否为radio或checkbox
            if ("inlineTemplate".equals(type)) {
                JSONObject attrs = sibling.getJSONObject("attrs");
                if (attrs != null) {
                    Object isTemplateObj = attrs.get("isTemplate");
                    if (isTemplateObj instanceof String) {
                        String templateType = (String) isTemplateObj;
                        if ("radio".equals(templateType) || "checkbox".equals(templateType)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * 从 JSONPath 提取字段名，用于快速从 row 对象取值。
     * 支持 $.fieldName 和 $['field-name'] 两种格式。
     */
    private String extractFieldNameFromPath(String path) {
        if (path == null || path.length() < 3) {
            return null;
        }
        if (path.startsWith("$['") && path.endsWith("']")) {
            return path.substring(3, path.length() - 2).replace("\\'", "'");
        }
        if (path.startsWith("$.") && !path.substring(2).contains(".")) {
            return path.substring(2);
        }
        return null;
    }

    /**
     * 根据列名/路径构建 JSONPath 表达式。
     * 当列名包含连字符（如 "2022-12"）时，使用方括号形式 $['2022-12']，
     * 否则会被错误解析为 $.2022.12（查找不存在的嵌套属性）。
     *
     * @param jsonDataPath 模板中的路径，如 "行名"、"2022-12"、"parent.child" 等
     * @return 正确的 JSONPath 表达式，如 $.行名、$['2022-12']、$.parent.child 等
     */
    private String buildJsonPathFromColumnKey(String jsonDataPath) {
        if (StringUtils.isBlank(jsonDataPath)) {
            return "$";
        }
        if (!jsonDataPath.contains(".")) {
            if (jsonDataPath.contains("-")) {
                return "$['" + jsonDataPath.replace("\\", "\\\\").replace("'", "\\'") + "']";
            }
            return "$." + jsonDataPath;
        }
        String[] segments = jsonDataPath.split("\\.");
        StringBuilder sb = new StringBuilder("$");
        for (String seg : segments) {
            if (seg.contains("-") || seg.contains(" ") || seg.matches("^\\d.*")) {
                sb.append("['").append(seg.replace("\\", "\\\\").replace("'", "\\'")).append("']");
            } else {
                sb.append(".").append(seg);
            }
        }
        return sb.toString();
    }

    /**
     * 替换文本中的变量
     * 支持格式: {{key#value#position#path}}
     * @param text 原始文本
     * @param parameters 参数对象
     * @param isBlockLevel 是否是块级元素，块级元素需要转换为Tiptap文档对象
     * @param position 位置标识，用于Tiptap节点的position属性
     * @return 替换结果，如果没有替换则返回 null
     */
    private ReplacementResult replaceVariables(String text, JSONObject parameters, boolean isBlockLevel, String position) {
        return replaceVariables(text, parameters, isBlockLevel, position, null);
    }

    /**
     * 替换文本中的变量（带原始marks样式）
     * 支持格式: {{key#value#position#path}}
     * @param text 原始文本
     * @param parameters 参数对象
     * @param isBlockLevel 是否是块级元素，块级元素需要转换为Tiptap文档对象
     * @param position 位置标识，用于Tiptap节点的position属性
     * @param originalMarks 原始文本节点的marks，用于保留字号、颜色、字体等样式
     * @return 替换结果，如果没有替换则返回 null
     */
    private ReplacementResult replaceVariables(String text, JSONObject parameters, boolean isBlockLevel, String position, JSONArray originalMarks) {
        if (StringUtils.isBlank(text)) {
            return null;
        }

        LOG.debug("替换变量前: {}", text);

        Matcher matcher = PATTERN.matcher(text);
        String result = text;
        boolean hasReplacement = false;
        LinkedHashSet<String> groupIds = new LinkedHashSet<>();
        Map<String, Object> tiptapContent = null;  // 存储Tiptap文档对象

        // 处理标准变量格式: {{key#value#position#path}}
        while (matcher.find()) {
            hasReplacement = true;
            try {
                String key = matcher.group(1);
                String value = matcher.group(2);
                String jsonDataPath = matcher.group(4);
                String path = buildJsonPathFromColumnKey(jsonDataPath);

                String replaceKey = "{{" + matcher.group(1) + "#" + matcher.group(2) + "#" + matcher.group(3) + "#" + matcher.group(4) + "}}";

                try {
                    Object eval = JSONPath.eval(parameters.getJSONObject(key + "#" + value).getJSONObject("value"), path);
                    String v = eval == null ? "" : JsonUtils.toJSONStringWithArm(eval);
                    result = result.replace(replaceKey, v);
                    addGroupId(parameters, groupIds, key, value);
                    // 四段变量在块级且整段仅占位符时，也走 Markdown 转 Tiptap，避免表格等结构被当纯文本
                    if (isBlockLevel && StringUtils.equals(StringUtils.trim(text), replaceKey)) {
                        tiptapContent = processMarkdownToTiptap(v, parameters, key + "#" + value, position, originalMarks);
                        result = "";
                    }
                } catch (Exception e) {
                    String pos = matcher.group(3).replace("$", "");
                    int index = 0;
                    if (StringUtils.isNotBlank(pos)) {
                        index = Integer.parseInt(pos) - 1;
                    }

                    try {
                        String arrayPath = path.startsWith("$['") ? path.replaceFirst("^\\$", "$[" + index + "]") : path.replace("$.", "$[" + index + "].");
                        JSONObject paramObj = parameters.getJSONObject(key + "#" + value);
                        Object eval = null;
                        if (paramObj != null && paramObj.getJSONArray("value") != null) {
                            eval = JSONPath.eval(paramObj.getJSONArray("value"), arrayPath);
                        }
                        String v = eval == null ? "" : JsonUtils.toJSONStringWithArm(eval);
                        result = result.replace(replaceKey, v);
                        addGroupId(parameters, groupIds, key, value);
                        // 兼容数组路径回退分支的 Markdown 转换逻辑
                        if (isBlockLevel && StringUtils.equals(StringUtils.trim(text), replaceKey)) {
                            tiptapContent = processMarkdownToTiptap(v, parameters, key + "#" + value, position, originalMarks);
                            result = "";
                        }
                    } catch (Exception e2) {
                        LOG.error("替换变量失败: key={}, value={}, path={}", key, value, path, e2);
                    }
                }
            } catch (Exception e) {
                LOG.error("处理变量替换时发生错误", e);
            }
        }

        // 处理全局变量: {{开始输入#xxx}}
        Matcher startInputMatcher = START_INPUT_PATTERN.matcher(result);
        while (startInputMatcher.find()) {
            hasReplacement = true;
            try {
                String value = parameters.getJSONObject(startInputMatcher.group(1)).getString("value");
                result = result.replace("{{开始输入#" + startInputMatcher.group(1) + "}}", value);
            } catch (Exception e) {
                LOG.error("替换全局变量失败", e);
            }
        }

        // 处理模块变量: {{key#value}}
        Matcher moduleValueMatcher = MODULE_VALUE_PATTERN.matcher(result);

        while (moduleValueMatcher.find()) {
            hasReplacement = true;
            try {
                String key = moduleValueMatcher.group(1);
                String value = moduleValueMatcher.group(2);
                String moduleKey = key + "#" + value;
                JSONObject paramObj = parameters.getJSONObject(moduleKey);
                String replaceKey = "{{" + key + "#" + value + "}}";

                // 如果参数对象不存在或值为空，替换为空字符串
                if (paramObj == null || paramObj.getString("value") == null) {
                    LOG.info("模块变量值为空，替换为空字符串: {}", replaceKey);
                    result = result.replace(replaceKey, "");
                    continue;
                }

                String moduleValue = paramObj.getString("value");
                
                // 如果值为空字符串，也替换为空
                if (StringUtils.isBlank(moduleValue)) {
                    LOG.info("模块变量值为空白，替换为空字符串: {}", replaceKey);
                    result = result.replace(replaceKey, "");
                    continue;
                }

                // 收集溯源信息
                addGroupId(parameters, groupIds, key, value);

                // 判断是否需要转换为Tiptap文档对象（块级元素需要转换）
                if (isBlockLevel) {
                    // 块级元素，转换为Tiptap文档对象
                    tiptapContent = processMarkdownToTiptap(moduleValue, parameters, moduleKey, position, originalMarks);
                    // 从text中移除该占位符
                    result = result.replace(replaceKey, "");
                } else {
                    // 不是普通提示#输出，直接替换text
                    result = result.replace(replaceKey, moduleValue);
                }
            } catch (Exception e) {
                LOG.error("替换模块变量失败", e);
            }
        }

        LOG.debug("替换变量后: {}", result);

        return hasReplacement ? new ReplacementResult(result, groupIds, tiptapContent) : null;
    }

    /**
     * 添加 groupId 到集合
     */
    private void addGroupId(JSONObject parameters, LinkedHashSet<String> groupIds, String key, String value) {
        try {
            String sourceWorkflowRunResultId = parameters.getJSONObject(key + "#" + value).getString("source_workflow_run_result_id");
            if (StringUtils.isNotBlank(sourceWorkflowRunResultId)) {
                groupIds.add(sourceWorkflowRunResultId);
            }
        } catch (Exception e) {
            LOG.debug("获取 source_workflow_run_result_id 失败: key={}, value={}", key, value);
        }
    }

    /**
     * 应用 sourceId 到节点的 marks（使用 textStyle 类型）
     */
    private void applySourceIdToMarks(JSONObject node, LinkedHashSet<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }

        JSONArray marks = node.getJSONArray("marks");
        if (marks == null) {
            marks = new JSONArray();
            node.put("marks", marks);
        }

        // 查找是否已经存在 textStyle mark
        JSONObject textStyleMark = null;
        for (int i = 0; i < marks.size(); i++) {
            JSONObject mark = marks.getJSONObject(i);
            if (mark != null && "textStyle".equals(mark.getString("type"))) {
                textStyleMark = mark;
                break;
            }
        }

        // 如果不存在，创建新的 textStyle mark
        if (textStyleMark == null) {
            textStyleMark = new JSONObject();
            textStyleMark.put("type", "textStyle");
            JSONObject attrs = new JSONObject();
            textStyleMark.put("attrs", attrs);
            marks.add(textStyleMark);
        }

        // 设置 sourceId（取第一个 groupId 作为 sourceId）和 position
        JSONObject attrs = textStyleMark.getJSONObject("attrs");
        if (attrs == null) {
            attrs = new JSONObject();
            textStyleMark.put("attrs", attrs);
        }

        // 取第一个 groupId 作为 sourceId
        String sourceId = groupIds.iterator().next();
        attrs.put("sourceId", sourceId);
        // 生成唯一的 position
        attrs.put("position", IdUtil.simpleUUID().replace("-", ""));
    }

    /**
     * 将Markdown内容转换为Tiptap文档对象
     *
     * @param content Markdown内容
     * @param parameters 参数对象
     * @param moduleKey 模块键
     * @param position 位置标识，用于Tiptap节点的position属性
     * @return Tiptap文档对象
     */
    private Map<String, Object> processMarkdownToTiptap(String content, JSONObject parameters, String moduleKey, String position) {
        return processMarkdownToTiptap(content, parameters, moduleKey, position, null);
    }

    /**
     * 将Markdown内容转换为Tiptap文档对象（带原始marks样式）
     *
     * @param content Markdown内容
     * @param parameters 参数对象
     * @param moduleKey 模块键
     * @param position 位置标识，用于Tiptap节点的position属性
     * @param originalMarks 原始文本节点的marks，用于保留字号、颜色、字体等样式
     * @return Tiptap文档对象
     */
    private Map<String, Object> processMarkdownToTiptap(String content, JSONObject parameters, String moduleKey, String position, JSONArray originalMarks) {
        try {
            LOG.debug("开始转换Markdown到Tiptap格式");

            // 获取sourceId
            String sourceId = null;
            try {
                sourceId = parameters.getJSONObject(moduleKey).getString("source_workflow_run_result_id");
            } catch (Exception e) {
                LOG.debug("无法获取source_workflow_run_result_id，将使用null作为sourceId");
            }

            // 从原始marks中提取样式信息
            String font = null;
            String rawFontSize = null;
            String color = null;
            String backgroundColor = null;
            if (originalMarks != null) {
                for (int i = 0; i < originalMarks.size(); i++) {
                    JSONObject mark = originalMarks.getJSONObject(i);
                    if (mark != null && "textStyle".equals(mark.getString("type"))) {
                        JSONObject attrs = mark.getJSONObject("attrs");
                        if (attrs != null) {
                            if (StringUtils.isNotBlank(attrs.getString("fontFamily"))) {
                                font = attrs.getString("fontFamily");
                            }
                            if (StringUtils.isNotBlank(attrs.getString("fontSize"))) {
                                rawFontSize = attrs.getString("fontSize");
                            }
                            if (StringUtils.isNotBlank(attrs.getString("color"))) {
                                color = attrs.getString("color");
                            }
                            if (StringUtils.isNotBlank(attrs.getString("backgroundColor"))) {
                                backgroundColor = attrs.getString("backgroundColor");
                            }
                        }
                        break;
                    }
                }
            }

            // 创建ConvertOptions，设置sourceId、position和样式信息
            var optionsBuilder = MarkdownToTiptapConverter.ConvertOptions.builder()
                    .sourceId(sourceId)
                    .position(position);
            if (font != null) {
                optionsBuilder.font(font);
            }
            if (rawFontSize != null) {
                optionsBuilder.rawFontSize(rawFontSize);
            }
            if (color != null) {
                optionsBuilder.color(color);
            }
            if (backgroundColor != null) {
                optionsBuilder.backgroundColor(backgroundColor);
            }
            var convertOptions = optionsBuilder.build();

            // 使用MarkdownToTiptapConverter将Markdown转换为Tiptap格式
            var tiptapDocument = MarkdownToTiptapConverter.convertMarkdownToTiptap(content, convertOptions);

            LOG.debug("Markdown转换完成，sourceId: {}, position: {}, 原始长度: {}", sourceId, position, content.length());

            return tiptapDocument;

        } catch (Exception e) {
            LOG.error("处理Markdown值时发生错误", e);
            return null;
        }
    }

    /**
     * 处理子工作流 (sub_workflow) 类型
     * 从子工作流输出中提取 Tiptap 文档内容
     *
     * @param nodeData 节点数据
     * @return Tiptap 文档内容数组，如果无法提取则返回 null
     */
    private JSONArray processSubWorkflow(NodeData nodeData) {
        try {
            JSONObject value = JsonUtils.parseJsonObjectWithArm(JsonUtils.toJSONStringWithArm(nodeData.getValue()));
            // 优先尝试 Tiptap 格式路径 $.docOutput.data.content
            Object eval = JSONPath.eval(value, "$.docOutput.data.content");
            if (ObjectUtil.isNotNull(eval) && eval instanceof JSONArray) {
                LOG.info("processSubWorkflow: 从 $.docOutput.data.content 提取到内容");
                return (JSONArray) eval;
            }
            // 兼容旧格式路径 $.docOutput.data.main
            eval = JSONPath.eval(value, "$.docOutput.data.main");
            if (ObjectUtil.isNotNull(eval) && eval instanceof JSONArray) {
                LOG.info("processSubWorkflow: 从 $.docOutput.data.main 提取到内容");
                return (JSONArray) eval;
            }
            LOG.warn("processSubWorkflow: 未能从子工作流输出中提取文档内容");
            return null;
        } catch (Exception e) {
            LOG.error("processSubWorkflow: 处理子工作流时发生错误", e);
            return null;
        }
    }

    /**
     * 处理循环子工作流 (sub_workflow_list) 类型
     * 从循环子工作流输出中提取并合并所有 Tiptap 文档内容
     *
     * @param nodeData 节点数据
     * @return 合并后的 Tiptap 文档内容数组，如果无法提取则返回 null
     */
    private JSONArray processSubWorkflowList(NodeData nodeData) {
        try {
            // 将节点值转换为 JSONArray
            JSONArray value;
            Object nodeValue = nodeData.getValue();
            if (nodeValue instanceof JSONArray) {
                value = (JSONArray) nodeValue;
            } else if (nodeValue instanceof String) {
                value = JsonUtils.parseArrayWithArm((String) nodeValue);
            } else {
                value = JsonUtils.parseArrayWithArm(JsonUtils.toJSONStringWithArm(nodeValue));
            }

            // 遍历每个子工作流结果，提取文档内容并合并
            JSONArray result = new JSONArray();
            for (int i = 0; i < value.size(); i++) {
                Object obj = value.get(i);
                if (obj == null) {
                    continue;
                }

                // 优先尝试 Tiptap 格式路径 $.docOutput.data.content
                Object eval = JSONPath.eval(obj, "$.docOutput.data.content");
                if (ObjectUtil.isNull(eval) || !(eval instanceof JSONArray)) {
                    // 兼容旧格式路径 $.docOutput.data.main
                    eval = JSONPath.eval(obj, "$.docOutput.data.main");
                }

                if (ObjectUtil.isNotNull(eval) && eval instanceof JSONArray) {
                    JSONArray array = (JSONArray) eval;
                    // 将每个子工作流的内容添加到结果中
                    result.addAll(array);
                    // 在每个子工作流结果后添加换行分隔（创建一个空段落作为分隔）
                    if (i < value.size() - 1) {
                        JSONObject separator = new JSONObject();
                        separator.put("type", "paragraph");
                        result.add(separator);
                    }
                }
            }

            LOG.info("processSubWorkflowList: 合并了 {} 个子工作流的内容，总共 {} 个节点", value.size(), result.size());
            return result.size() > 0 ? result : null;
        } catch (Exception e) {
            LOG.error("processSubWorkflowList: 处理循环子工作流时发生错误", e);
            return null;
        }
    }

    /**
     * 替换结果类
     */
    private static class ReplacementResult {
        private final String text;
        private final LinkedHashSet<String> groupIds;
        private final Map<String, Object> tiptapContent;

        public ReplacementResult(String text, LinkedHashSet<String> groupIds) {
            this(text, groupIds, null);
        }

        public ReplacementResult(String text, LinkedHashSet<String> groupIds, Map<String, Object> tiptapContent) {
            this.text = text;
            this.groupIds = groupIds;
            this.tiptapContent = tiptapContent;
        }

        public String getText() {
            return text;
        }

        public LinkedHashSet<String> getGroupIds() {
            return groupIds;
        }

        public Map<String, Object> getTiptapContent() {
            return tiptapContent;
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

            JSONObject rowAttrs = tableRow.getJSONObject("attrs");
            if (rowAttrs == null) {
                rowAttrs = new JSONObject();
                tableRow.put("attrs", rowAttrs);
            }

            String loop = rowAttrs.getString("loop");
            Object startLoopIndexObj = rowAttrs.get("startLoopIndex");
            Object endLoopIndexObj = rowAttrs.get("endLoopIndex");
            int startLoopIndex = parseLoopIndex(startLoopIndexObj);
            int endLoopIndex = parseLoopIndex(endLoopIndexObj);

            JSONArray cellContent = tableRow.getJSONArray("content");
            if (cellContent == null) {
                continue;
            }

            if (StringUtils.isNotBlank(loop) && "true".equals(loop)) {
                String loopKey = "";
                for (int g = 0; g < cellContent.size(); g++) {
                    JSONObject cell = cellContent.getJSONObject(g);
                    if (cell == null) {
                        continue;
                    }

                    JSONObject cellAttrs = cell.getJSONObject("attrs");
                    if (cellAttrs != null) {
                        String markInsert = cellAttrs.getString("markInsert");
                        if (StringUtils.isNotBlank(markInsert) && "true".equals(markInsert)) {
                            continue;
                        }
                    }

                    boolean inLoopRange = false;
                    if (startLoopIndex == -1 && endLoopIndex == -1) {
                        inLoopRange = true;
                    } else {
                        inLoopRange = (g >= startLoopIndex && g <= endLoopIndex);
                    }

                    if (!inLoopRange) {
                        continue;
                    }

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

                                            if ("inlineTemplate".equals(nodeType)) {
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

                    item.put("sourceId", loopKeyObj.getString("source_workflow_run_result_id"));
                }
            } else {
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
                                        String isTemplate = null;
                                        JSONObject nodeAttrs = textNode.getJSONObject("attrs");
                                        if (nodeAttrs != null) {
                                            isTemplate = nodeAttrs.getString("isTemplate");
                                        }
                                        if (StringUtils.isBlank(isTemplate)) {
                                            isTemplate = textNode.getString("isTemplate");
                                        }

                                        if (StrUtil.equals("checkbox", isTemplate) || StrUtil.equals("radio", isTemplate)) {
                                            String variableText = extractVariableTextFromNode(textNode);
                                            if (StringUtils.isBlank(variableText)) {
                                                continue;
                                            }

                                            ElementFormatterUtil.NodeData value = getValueByVariableName(parameters, variableText);
                                            if (value == null) {
                                                continue;
                                            }

                                            String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(variableText)))
                                                    .map(obj -> obj.getString("source_workflow_run_result_id"))
                                                    .orElse("");

                                            boolean checked = ObjectUtil.equals(value.getValue(), true);
                                            textNode = new JSONObject();
                                            textNode.put("type", isTemplate);

                                            JSONObject attrs = new JSONObject();
                                            attrs.put("checked", checked);
                                            if (StringUtils.isNotBlank(groupIds)) {
                                                attrs.put("sourceId", groupIds);
                                            }
                                            String position = null;
                                            if (nodeAttrs != null && nodeAttrs.containsKey("id")) {
                                                position = nodeAttrs.getString("id");
                                            }
                                            if (StringUtils.isBlank(position)) {
                                                position = IdUtil.simpleUUID().replace("-", "");
                                            }
                                            attrs.put("position", position);
                                            textNode.put("attrs", attrs);

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

                                                    if ("chart_equity_penetration".equals(type)) {
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

                                                            String imageUrl = ElementFormatterUtil.equityPenetrationImage(valueList, legend);
                                                            if (StrUtil.isNotEmpty(imageUrl)) {
                                                                textNode.put("value", imageUrl);
                                                            } else {
                                                                paraContent.remove(k);
                                                                k--;
                                                            }
                                                        } else {
                                                            paraContent.remove(k);
                                                            k--;
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            boolean needReplaceParagraph = textRenderNewFormat(paraContent, k, parameters, "table", paragraph, cellContentArray, paraIdx);
                                            if (needReplaceParagraph) {
                                                paraIdx--;
                                                break;
                                            }
                                        }

                                        if ((StringUtils.isNotBlank(markInsert) && "true".equals(markInsert)) ||
                                                (StringUtils.isNotBlank(rowMerged) && "true".equals(rowMerged))) {
                                            cellAttrs.put("markInsert", null);
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

        int placeholderRowCount = calculatePlaceholderRowCount(cellContent);
        int size = 0;
        Map<String, Object> cache = new HashMap<>();

        LOG.info("loopRowTableRenderDataNewFormat: 开始构建cache, cellContent.size()={}", cellContent.size());
        for (int g = 0; g < cellContent.size(); g++) {
            JSONObject cell = cellContent.getJSONObject(g);
            if (cell == null) {
                continue;
            }

            JSONObject cellAttrs = cell.getJSONObject("attrs");
            if (cellAttrs != null) {
                String markInsert = cellAttrs.getString("markInsert");
                if (StringUtils.isNotBlank(markInsert) && "true".equals(markInsert)) {
                    LOG.info("loopRowTableRenderDataNewFormat: 跳过markInsert单元格, g={}", g);
                    continue;
                }
            }

            JSONArray cellContentArray = cell.getJSONArray("content");
            if (cellContentArray == null) {
                LOG.info("loopRowTableRenderDataNewFormat: 单元格content为空, g={}", g);
                continue;
            }

            LOG.info("loopRowTableRenderDataNewFormat: 处理单元格, g={}, cellContentArray.size()={}", g, cellContentArray.size());
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
                        eleValueStr = textNode.getString("value");
                    }

                    if (StringUtils.isBlank(eleValueStr)) {
                        continue;
                    }

                    LOG.info("loopRowTableRenderDataNewFormat: 找到模板变量, g={}, eleValueStr={}", g, eleValueStr);
                    Matcher matcher = PATTERN.matcher(eleValueStr);
                    while (matcher.find()) {
                        String key = matcher.group(1);
                        String value = matcher.group(2);
                        String arrayIndexRef = matcher.group(3);
                        String fieldName = matcher.group(4);

                        String loopKey = key + "#" + value;
                        String cacheKey = String.format("%s#%s", key, value);

                        LOG.info("loopRowTableRenderDataNewFormat: 处理模板变量, loopKey={}, cacheKey={}, cache.containsKey={}", loopKey, cacheKey, cache.containsKey(cacheKey));

                        if (cache.containsKey(cacheKey)) {
                            LOG.info("loopRowTableRenderDataNewFormat: 已缓存, 跳过, cacheKey={}", cacheKey);
                            continue;
                        }

                        JSONObject loopKeyObj = parameters.getJSONObject(loopKey);
                        if (loopKeyObj == null) {
                            LOG.info("无法在 parameters 中找到数据源: {}", loopKey);
                            continue;
                        }

                        JSONArray array = null;
                        Object valueObj = loopKeyObj.get("value");

                        if (valueObj instanceof JSONArray) {
                            array = (JSONArray) valueObj;
                            LOG.info("loopRowTableRenderDataNewFormat: 直接获取到数组, loopKey={}, arraySize={}", loopKey, array.size());
                        } else if (valueObj instanceof JSONObject) {
                            JSONObject valueJsonObj = (JSONObject) valueObj;
                            Object eval = JSONPath.eval(JsonUtils.toJSONStringWithArm(valueJsonObj), "$.value");
                            if (eval instanceof JSONArray) {
                                array = (JSONArray) eval;
                            } else {
                                eval = JSONPath.eval(JsonUtils.toJSONStringWithArm(valueJsonObj), "$");
                                if (eval instanceof JSONArray) {
                                    array = (JSONArray) eval;
                                } else {
                                    for (String field : valueJsonObj.keySet()) {
                                        Object fieldValue = valueJsonObj.get(field);
                                        if (fieldValue instanceof JSONArray) {
                                            array = (JSONArray) fieldValue;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        if (array == null) {
                            LOG.info("无法在数据源 {} 中找到数组: {}", loopKey, eleValueStr);
                            continue;
                        }

                        if (size == 0 || array.size() > size) {
                            size = array.size();
                        }

                        cache.put(cacheKey, array);
                        LOG.info("loopRowTableRenderDataNewFormat: 缓存数组, cacheKey={}, arraySize={}", cacheKey, array.size());
                    }

                    setGlobalVariable(textNode, parameters, "loopRowTableRenderDataNewFormat");
                }
            }
        }
        LOG.info("loopRowTableRenderDataNewFormat: cache构建完成, cacheSize={}, size={}", cache.size(), size);


        List<JSONObject> newRowList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            JSONObject newRow = JsonUtils.parseJsonObjectWithArm(JsonUtils.toJSONStringWithArm(tableRowTemplate));
            JSONObject newRowAttrs = newRow.getJSONObject("attrs");
            if (newRowAttrs == null) {
                newRowAttrs = new JSONObject();
                newRow.put("attrs", newRowAttrs);
            }
            newRowAttrs.put("loop", null);

            JSONObject loopParameters = JsonUtils.parseJsonObjectWithArm(JsonUtils.toJSONStringWithArm(parameters));
            Set<String> processedLoopKeys = new HashSet<>();

            JSONArray templateCellContent = tableRowTemplate.getJSONArray("content");
            LOG.info("loopRowTableRenderDataNewFormat: 开始准备loopParameters, i={}, templateCellContent.size()={}", i, templateCellContent != null ? templateCellContent.size() : 0);
            if (templateCellContent != null) {
                for (int g = 0; g < templateCellContent.size(); g++) {
                    JSONObject cell = templateCellContent.getJSONObject(g);
                    if (cell == null) {
                        continue;
                    }

                    JSONObject cellAttrs = cell.getJSONObject("attrs");
                    if (cellAttrs != null) {
                        String markInsert = cellAttrs.getString("markInsert");
                        if (StringUtils.isNotBlank(markInsert) && "true".equals(markInsert)) {
                            continue;
                        }
                    }

                    boolean inLoopRange = false;
                    if (startLoopIndex == -1 && endLoopIndex == -1) {
                        inLoopRange = true;
                    } else {
                        inLoopRange = (g >= startLoopIndex && g <= endLoopIndex);
                    }

                    if (!inLoopRange) {
                        continue;
                    }

                    JSONArray cellContentArray = cell.getJSONArray("content");
                    if (cellContentArray == null) {
                        continue;
                    }

                    // 遍历单元格内容，查找所有模板变量
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

                        for (int i1 = 0; i1 < paraContent.size(); i1++) {
                            JSONObject textNode = paraContent.getJSONObject(i1);
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
                                eleValueStr = textNode.getString("value");
                            }

                            if (StringUtils.isBlank(eleValueStr)) {
                                continue;
                            }

                            LOG.info("loopRowTableRenderDataNewFormat: 准备loopParameters时找到模板变量, i={}, g={}, eleValueStr={}", i, g, eleValueStr);
                            Matcher matcher = PATTERN.matcher(eleValueStr);
                            while (matcher.find()) {
                                try {
                                    String key = matcher.group(1);
                                    String value = matcher.group(2);
                                    String loopKey = key + "#" + value;

                                    LOG.info("loopRowTableRenderDataNewFormat: 处理模板变量, i={}, loopKey={}, processedLoopKeys.contains={}", i, loopKey, processedLoopKeys.contains(loopKey));

                                    if (processedLoopKeys.contains(loopKey)) {
                                        continue;
                                    }

                                    String cacheKey = String.format("%s#%s", key, value);
                                    Object o = cache.get(cacheKey);
                                    LOG.info("loopRowTableRenderDataNewFormat: 从cache获取, i={}, loopKey={}, cacheKey={}, cache.containsKey={}, o==null={}, o instanceof JSONArray={}",
                                            i, loopKey, cacheKey, cache.containsKey(cacheKey), o == null, o instanceof JSONArray);
                                    if (o == null || !(o instanceof JSONArray)) {
                                        LOG.info("loopRowTableRenderDataNewFormat: cache中没有找到数组, loopKey={}, cacheKey={}, cacheSize={}", loopKey, cacheKey, cache.size());
                                        continue;
                                    }

                                    JSONArray array = (JSONArray) o;
                                    if (i < array.size()) {
                                        Object item = array.get(i);
                                        JSONObject originalParam = parameters.getJSONObject(loopKey);
                                        if (originalParam != null) {
                                            JSONObject newParam = JsonUtils.parseJsonObjectWithArm(JsonUtils.toJSONStringWithArm(originalParam));
                                            newParam.put("value", item);
                                            loopParameters.put(loopKey, newParam);
                                            processedLoopKeys.add(loopKey);
                                            LOG.info("loopRowTableRenderDataNewFormat: 成功添加循环参数, loopKey={}, i={}", loopKey, i);
                                        } else {
                                            LOG.info("loopRowTableRenderDataNewFormat: parameters中没有找到原始参数, loopKey={}", loopKey);
                                        }
                                    } else {
                                        LOG.info("loopRowTableRenderDataNewFormat: 数组索引越界, loopKey={}, i={}, arraySize={}", loopKey, i, array.size());
                                    }
                                } catch (Exception e) {
                                    LOG.error("loopRowTableRenderDataNewFormat prepare loop parameters error", e);
                                }
                            }
                        }
                    }
                }
            }

            JSONArray newCellContent = newRow.getJSONArray("content");
            if (newCellContent != null) {
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

                    if (isMarkInsert) {
                        if (i == 0) {
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
                                            String isTemplate = null;
                                            JSONObject nodeAttrs = textNode.getJSONObject("attrs");
                                            if (nodeAttrs != null) {
                                                isTemplate = nodeAttrs.getString("isTemplate");
                                            }
                                            if (StringUtils.isBlank(isTemplate)) {
                                                isTemplate = textNode.getString("isTemplate");
                                            }

                                            if (StrUtil.equals("checkbox", isTemplate) || StrUtil.equals("radio", isTemplate)) {
                                                String variableText = extractVariableTextFromNode(textNode);
                                                if (StringUtils.isBlank(variableText)) {
                                                    continue;
                                                }

                                                ElementFormatterUtil.NodeData value = getValueByVariableName(parameters, variableText);
                                                if (value == null) {
                                                    continue;
                                                }

                                                String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(variableText)))
                                                        .map(obj -> obj.getString("source_workflow_run_result_id"))
                                                        .orElse("");

                                                boolean checked = ObjectUtil.equals(value.getValue(), true);
                                                textNode = new JSONObject();
                                                textNode.put("type", isTemplate);

                                                JSONObject attrs = new JSONObject();
                                                attrs.put("checked", checked);
                                                if (StringUtils.isNotBlank(groupIds)) {
                                                    attrs.put("sourceId", groupIds);
                                                }
                                                String position = null;
                                                if (nodeAttrs != null && nodeAttrs.containsKey("id")) {
                                                    position = nodeAttrs.getString("id");
                                                }
                                                if (StringUtils.isBlank(position)) {
                                                    position = IdUtil.simpleUUID().replace("-", "");
                                                }
                                                attrs.put("position", position);
                                                textNode.put("attrs", attrs);

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

                                                        if ("chart_equity_penetration".equals(type)) {
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

                                                                String imageUrl = ElementFormatterUtil.equityPenetrationImage(valueList, legend);
                                                                if (StrUtil.isNotEmpty(imageUrl)) {
                                                                    textNode.put("value", imageUrl);
                                                                } else {
                                                                    paraContent.remove(k);
                                                                    k--;
                                                                }
                                                            } else {
                                                                paraContent.remove(k);
                                                                k--;
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                boolean needReplaceParagraph = textRenderNewFormat(paraContent, k, parameters, "table", paragraph, cellContentArray, paraIdx);
                                                if (needReplaceParagraph) {
                                                    paraIdx--;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Object templateRowspanObj = cellAttrs.get("rowspan");
                            int templateRowspan = 1;
                            if (templateRowspanObj instanceof Number) {
                                templateRowspan = ((Number) templateRowspanObj).intValue();
                            }
                            cellAttrs.put("rowspan", templateRowspan + size - 1);
                            cellAttrs.put("markInsert", null);
                        } else {
                            markInsertIndicesToRemove.add(g);
                        }
                        continue;
                    }

                    boolean inLoopRange = false;
                    if (startLoopIndex == -1 && endLoopIndex == -1) {
                        inLoopRange = true;
                    } else {
                        inLoopRange = (g >= startLoopIndex && g <= endLoopIndex);
                    }


                    if (!inLoopRange) {
                        continue;
                    }

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

                            String isTemplate = null;
                            JSONObject nodeAttrs = textNode.getJSONObject("attrs");
                            if (nodeAttrs != null) {
                                isTemplate = nodeAttrs.getString("isTemplate");
                            }
                            if (StringUtils.isBlank(isTemplate)) {
                                isTemplate = textNode.getString("isTemplate");
                            }

                            if (StrUtil.equals("checkbox", isTemplate) || StrUtil.equals("radio", isTemplate)) {
                                String variableText = extractVariableTextFromNode(textNode);
                                if (StringUtils.isBlank(variableText)) {
                                    continue;
                                }

                                ElementFormatterUtil.NodeData value = getValueByVariableName(loopParameters, variableText);
                                if (value == null) {
                                    continue;
                                }

                                String groupIds = Optional.ofNullable(loopParameters.getJSONObject(getloopKeyByVariableName(variableText)))
                                        .map(obj -> obj.getString("source_workflow_run_result_id"))
                                        .orElse("");

                                boolean checked = ObjectUtil.equals(value.getValue(), true);
                                textNode = new JSONObject();
                                textNode.put("type", isTemplate);

                                JSONObject attrs = new JSONObject();
                                attrs.put("checked", checked);
                                if (StringUtils.isNotBlank(groupIds)) {
                                    attrs.put("sourceId", groupIds);
                                }
                                String position = null;
                                if (nodeAttrs != null && nodeAttrs.containsKey("id")) {
                                    position = nodeAttrs.getString("id");
                                }
                                if (StringUtils.isBlank(position)) {
                                    position = IdUtil.simpleUUID().replace("-", "");
                                }
                                attrs.put("position", position);
                                textNode.put("attrs", attrs);

                                paraContent.set(i1, textNode);
                            } else if (StrUtil.equals("chart", isTemplate)) {
                                String groupIds = Optional.ofNullable(loopParameters.getJSONObject(getloopKeyByVariableName(textNode.getString("value"))))
                                        .map(obj -> obj.getString("source_workflow_run_result_id"))
                                        .orElse("");

                                textNode.remove("isTemplate");

                                String chartValueRaw = textNode.getString("chartValue");
                                String key = chartValueRaw;
                                if (StrUtil.isNotEmpty(chartValueRaw)) {
                                    key = chartValueRaw.replaceFirst("^\\{\\{([^\\{].*[^\\}])\\}\\}$", "$1");
                                }
                                JSONObject paramObj = loopParameters.getJSONObject(key);
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

                                        if ("chart_equity_penetration".equals(type)) {
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

                                                String imageUrl = ElementFormatterUtil.equityPenetrationImage(valueList, legend);
                                                if (StrUtil.isNotEmpty(imageUrl)) {
                                                    textNode.put("value", imageUrl);
                                                } else {
                                                    paraContent.remove(i1);
                                                    i1--;
                                                }
                                            } else {
                                                paraContent.remove(i1);
                                                i1--;
                                            }
                                        }
                                    }
                                }
                            } else {
                                boolean needReplaceParagraph = textRenderNewFormat(paraContent, i1, loopParameters, "table", paragraph, cellContentArray, paraIdx);
                                if (needReplaceParagraph) {
                                    paraIdx--;
                                    break;
                                }
                            }
                        }
                    }
                }

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

        int placeholderRowCount = calculatePlaceholderRowCount(cellContent);
        int size = 0;
        Map<String, Object> cache = new HashMap<>();

        LOG.info("loopRowTableRenderDataByJSONArrayNewFormat: 开始构建cache, cellContent.size()={}", cellContent.size());
        for (int g = 0; g < cellContent.size(); g++) {
            JSONObject cell = cellContent.getJSONObject(g);
            if (cell == null) {
                continue;
            }

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
                        eleValueStr = textNode.getString("value");
                    }

                    if (StringUtils.isBlank(eleValueStr)) {
                        continue;
                    }

                    Matcher matcher = PATTERN.matcher(eleValueStr);
                    while (matcher.find()) {
                        String key = matcher.group(1);
                        String value = matcher.group(2);
                        String loopKey = key + "#" + value;
                        String cacheKey = String.format("%s#%s", key, value);

                        if (cache.containsKey(cacheKey)) {
                            continue;
                        }

                        JSONObject loopKeyObj = parameters.getJSONObject(loopKey);
                        if (loopKeyObj == null) {
                            continue;
                        }

                        JSONArray array = null;
                        Object valueObj = loopKeyObj.get("value");

                        if (valueObj instanceof JSONArray) {
                            array = (JSONArray) valueObj;
                        } else if (valueObj instanceof JSONObject) {
                            JSONObject valueJsonObj = (JSONObject) valueObj;
                            Object eval = JSONPath.eval(JsonUtils.toJSONStringWithArm(valueJsonObj), "$.value");
                            if (eval instanceof JSONArray) {
                                array = (JSONArray) eval;
                            } else {
                                eval = JSONPath.eval(JsonUtils.toJSONStringWithArm(valueJsonObj), "$");
                                if (eval instanceof JSONArray) {
                                    array = (JSONArray) eval;
                                } else {
                                    for (String field : valueJsonObj.keySet()) {
                                        Object fieldValue = valueJsonObj.get(field);
                                        if (fieldValue instanceof JSONArray) {
                                            array = (JSONArray) fieldValue;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        if (array == null) {
                            continue;
                        }

                        if (size == 0 || array.size() > size) {
                            size = array.size();
                        }

                        cache.put(cacheKey, array);
                        LOG.info("loopRowTableRenderDataByJSONArrayNewFormat: 缓存数组, cacheKey={}, arraySize={}", cacheKey, array.size());
                    }
                }
            }
        }

        if (size == 0 && dataObject != null) {
            size = dataObject.size();
        }

        LOG.info("loopRowTableRenderDataByJSONArrayNewFormat: cache构建完成, cacheSize={}, size={}", cache.size(), size);


        List<JSONObject> newRowList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            JSONObject newRow = JsonUtils.parseJsonObjectWithArm(JsonUtils.toJSONStringWithArm(tableRowTemplate));
            JSONObject newRowAttrs = newRow.getJSONObject("attrs");
            if (newRowAttrs == null) {
                newRowAttrs = new JSONObject();
                newRow.put("attrs", newRowAttrs);
            }
            newRowAttrs.put("loop", null);

            JSONObject loopParameters = JsonUtils.parseJsonObjectWithArm(JsonUtils.toJSONStringWithArm(parameters));
            Set<String> processedLoopKeys = new HashSet<>();

            JSONArray templateCellContent = tableRowTemplate.getJSONArray("content");
            if (templateCellContent != null) {
                for (int g = 0; g < templateCellContent.size(); g++) {
                    JSONObject cell = templateCellContent.getJSONObject(g);
                    if (cell == null) {
                        continue;
                    }

                    JSONObject cellAttrs = cell.getJSONObject("attrs");
                    if (cellAttrs != null) {
                        String markInsert = cellAttrs.getString("markInsert");
                        if (StringUtils.isNotBlank(markInsert) && "true".equals(markInsert)) {
                            continue;
                        }
                    }

                    boolean inLoopRange = false;
                    if (startLoopIndex == -1 && endLoopIndex == -1) {
                        inLoopRange = true;
                    } else {
                        inLoopRange = (g >= startLoopIndex && g <= endLoopIndex);
                    }

                    if (!inLoopRange) {
                        continue;
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

                        for (int i1 = 0; i1 < paraContent.size(); i1++) {
                            JSONObject textNode = paraContent.getJSONObject(i1);
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
                                eleValueStr = textNode.getString("value");
                            }

                            if (StringUtils.isBlank(eleValueStr)) {
                                continue;
                            }

                            Matcher matcher = PATTERN.matcher(eleValueStr);
                            while (matcher.find()) {
                                try {
                                    String key = matcher.group(1);
                                    String value = matcher.group(2);
                                    String loopKey = key + "#" + value;

                                    if (processedLoopKeys.contains(loopKey)) {
                                        continue;
                                    }

                                    String cacheKey = String.format("%s#%s", key, value);
                                    Object o = cache.get(cacheKey);
                                    if (o == null || !(o instanceof JSONArray)) {
                                        continue;
                                    }

                                    JSONArray array = (JSONArray) o;
                                    if (i < array.size()) {
                                        Object item = array.get(i);
                                        JSONObject originalParam = parameters.getJSONObject(loopKey);
                                        if (originalParam != null) {
                                            JSONObject newParam = JsonUtils.parseJsonObjectWithArm(JsonUtils.toJSONStringWithArm(originalParam));
                                            newParam.put("value", item);
                                            loopParameters.put(loopKey, newParam);
                                            processedLoopKeys.add(loopKey);
                                        }
                                    }
                                } catch (Exception e) {
                                    LOG.error("loopRowTableRenderDataByJSONArrayNewFormat prepare loop parameters error", e);
                                }
                            }
                        }
                    }
                }
            }

            JSONArray newCellContent = newRow.getJSONArray("content");
            if (newCellContent != null) {
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

                    if (isMarkInsert) {
                        if (i == 0) {
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
                                            String isTemplate = null;
                                            JSONObject nodeAttrs = textNode.getJSONObject("attrs");
                                            if (nodeAttrs != null) {
                                                isTemplate = nodeAttrs.getString("isTemplate");
                                            }
                                            if (StringUtils.isBlank(isTemplate)) {
                                                isTemplate = textNode.getString("isTemplate");
                                            }

                                            if (StrUtil.equals("checkbox", isTemplate) || StrUtil.equals("radio", isTemplate)) {
                                                String variableText = extractVariableTextFromNode(textNode);
                                                if (StringUtils.isBlank(variableText)) {
                                                    continue;
                                                }

                                                ElementFormatterUtil.NodeData value = getValueByVariableName(parameters, variableText);
                                                if (value == null) {
                                                    continue;
                                                }

                                                String groupIds = Optional.ofNullable(parameters.getJSONObject(getloopKeyByVariableName(variableText)))
                                                        .map(obj -> obj.getString("source_workflow_run_result_id"))
                                                        .orElse("");

                                                boolean checked = ObjectUtil.equals(value.getValue(), true);
                                                textNode = new JSONObject();
                                                textNode.put("type", isTemplate);

                                                JSONObject attrs = new JSONObject();
                                                attrs.put("checked", checked);
                                                if (StringUtils.isNotBlank(groupIds)) {
                                                    attrs.put("sourceId", groupIds);
                                                }
                                                String position = null;
                                                if (nodeAttrs != null && nodeAttrs.containsKey("id")) {
                                                    position = nodeAttrs.getString("id");
                                                }
                                                if (StringUtils.isBlank(position)) {
                                                    position = IdUtil.simpleUUID().replace("-", "");
                                                }
                                                attrs.put("position", position);
                                                textNode.put("attrs", attrs);

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

                                                        if ("chart_equity_penetration".equals(type)) {
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

                                                                String imageUrl = ElementFormatterUtil.equityPenetrationImage(valueList, legend);
                                                                if (StrUtil.isNotEmpty(imageUrl)) {
                                                                    textNode.put("value", imageUrl);
                                                                } else {
                                                                    paraContent.remove(k);
                                                                    k--;
                                                                }
                                                            } else {
                                                                paraContent.remove(k);
                                                                k--;
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                boolean needReplaceParagraph = textRenderNewFormat(paraContent, k, parameters, "table", paragraph, cellContentArray, paraIdx);
                                                if (needReplaceParagraph) {
                                                    paraIdx--;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Object templateRowspanObj = cellAttrs.get("rowspan");
                            int templateRowspan = 1;
                            if (templateRowspanObj instanceof Number) {
                                templateRowspan = ((Number) templateRowspanObj).intValue();
                            }
                            cellAttrs.put("rowspan", templateRowspan + size - 1);
                            cellAttrs.put("markInsert", null);
                        } else {
                            markInsertIndicesToRemove.add(g);
                        }
                        continue;
                    }

                    boolean inLoopRange = false;
                    if (startLoopIndex == -1 && endLoopIndex == -1) {
                        inLoopRange = true;
                    } else {
                        inLoopRange = (g >= startLoopIndex && g <= endLoopIndex);
                    }

                    if (!inLoopRange) {
                        continue;
                    }

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

                            String isTemplate = null;
                            JSONObject nodeAttrs = textNode.getJSONObject("attrs");
                            if (nodeAttrs != null) {
                                isTemplate = nodeAttrs.getString("isTemplate");
                            }
                            if (StringUtils.isBlank(isTemplate)) {
                                isTemplate = textNode.getString("isTemplate");
                            }

                            if (StrUtil.equals("checkbox", isTemplate) || StrUtil.equals("radio", isTemplate)) {
                                String variableText = extractVariableTextFromNode(textNode);
                                if (StringUtils.isBlank(variableText)) {
                                    continue;
                                }

                                ElementFormatterUtil.NodeData value = getValueByVariableName(loopParameters, variableText);
                                if (value == null) {
                                    continue;
                                }

                                String groupIds = Optional.ofNullable(loopParameters.getJSONObject(getloopKeyByVariableName(variableText)))
                                        .map(obj -> obj.getString("source_workflow_run_result_id"))
                                        .orElse("");

                                boolean checked = ObjectUtil.equals(value.getValue(), true);
                                textNode = new JSONObject();
                                textNode.put("type", isTemplate);

                                JSONObject attrs = new JSONObject();
                                attrs.put("checked", checked);
                                if (StringUtils.isNotBlank(groupIds)) {
                                    attrs.put("sourceId", groupIds);
                                }
                                String position = null;
                                if (nodeAttrs != null && nodeAttrs.containsKey("id")) {
                                    position = nodeAttrs.getString("id");
                                }
                                if (StringUtils.isBlank(position)) {
                                    position = IdUtil.simpleUUID().replace("-", "");
                                }
                                attrs.put("position", position);
                                textNode.put("attrs", attrs);

                                paraContent.set(i1, textNode);
                            } else if (StrUtil.equals("chart", isTemplate)) {
                                String groupIds = Optional.ofNullable(loopParameters.getJSONObject(getloopKeyByVariableName(textNode.getString("value"))))
                                        .map(obj -> obj.getString("source_workflow_run_result_id"))
                                        .orElse("");

                                textNode.remove("isTemplate");

                                String chartValueRaw = textNode.getString("chartValue");
                                String key = chartValueRaw;
                                if (StrUtil.isNotEmpty(chartValueRaw)) {
                                    key = chartValueRaw.replaceFirst("^\\{\\{([^\\{].*[^\\}])\\}\\}$", "$1");
                                }
                                JSONObject paramObj = loopParameters.getJSONObject(key);
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

                                        if ("chart_equity_penetration".equals(type)) {
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

                                                String imageUrl = ElementFormatterUtil.equityPenetrationImage(valueList, legend);
                                                if (StrUtil.isNotEmpty(imageUrl)) {
                                                    textNode.put("value", imageUrl);
                                                } else {
                                                    paraContent.remove(i1);
                                                    i1--;
                                                }
                                            } else {
                                                paraContent.remove(i1);
                                                i1--;
                                            }
                                        }
                                    }
                                }
                            } else {
                                boolean needReplaceParagraph = textRenderNewFormat(paraContent, i1, loopParameters, "table", paragraph, cellContentArray, paraIdx);
                                if (needReplaceParagraph) {
                                    paraIdx--;
                                    break;
                                }
                            }
                        }
                    }
                }

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
     * 但是，为了避免影响普通单元格的 rowspan 合并，只在确实存在占位符行时才移除它们
     */
    private int calculatePlaceholderRowCount(JSONArray cellContent) {
        if (cellContent == null || cellContent.isEmpty()) {
            return 0;
        }


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
     * 解析循环索引值（支持 Number 和 String 类型）
     */
    private int parseLoopIndex(Object indexObj) {
        if (indexObj instanceof Number) {
            return ((Number) indexObj).intValue();
        }
        if (indexObj instanceof String) {
            try {
                return Integer.parseInt(((String) indexObj).trim());
            } catch (NumberFormatException ignore) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * 从节点中提取变量文本（支持 inlineTemplate 和旧格式）
     * @param textNode 节点对象
     * @return 变量文本，如果无法提取则返回 null
     */
    private String extractVariableTextFromNode(JSONObject textNode) {
        if (textNode == null) {
            return null;
        }

        if ("inlineTemplate".equals(textNode.getString("type"))) {
            JSONArray inlineContent = textNode.getJSONArray("content");
            if (inlineContent != null) {
                for (int m = 0; m < inlineContent.size(); m++) {
                    JSONObject innerNode = inlineContent.getJSONObject(m);
                    if (innerNode != null && "text".equals(innerNode.getString("type"))) {
                        return innerNode.getString("text");
                    }
                }
            }
        }

        if ("text".equals(textNode.getString("type"))) {
            return textNode.getString("text");
        }

        return textNode.getString("value");
    }

    /**
     * 移除占位符行
     */
    private void removePlaceholderTemplates(JSONArray root, int rowIndex, int placeholderCount) {
        if (placeholderCount <= 0 || root == null) {
            return;
        }


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


                    String markInsert = cellAttrs.getString("markInsert");
                    if (StringUtils.isNotBlank(markInsert) && "true".equals(markInsert)) {
                        continue;
                    }

                    boolean inLoopRange = false;
                    if (startLoopIndex == -1 && endLoopIndex == -1) {
                        inLoopRange = true;
                    } else {
                        inLoopRange = (j >= startLoopIndex && j <= endLoopIndex);
                    }

                    if (!inLoopRange) {
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
                                        if (textNode != null && isImageNode(textNode)) {
                                            k = handleTableImageNode(paraContent, k, parameters);
                                        }
                                    }
                                }
                            }
                        }

                        Object existingRowspanObj = cellAttrs.get("rowspan");
                        int existingRowspan = 1;
                        if (existingRowspanObj instanceof Number) {
                            existingRowspan = ((Number) existingRowspanObj).intValue();
                        }

                        if (existingRowspan > 1) {
                            cellAttrs.put("_originalRowspan", existingRowspan);
                        }

                        cellAttrs.put("rowspan", trSize);
                        cellsToRemove.add(j);
                    }
                }

                for (int i = 1; i < trSize; i++) {
                    JSONArray rowCells = newRowList.get(i).getJSONArray("content");
                    if (rowCells != null) {
                        for (int j = rowCells.size() - 1; j >= 0; j--) {
                            JSONObject cell = rowCells.getJSONObject(j);
                            if (cell != null) {
                                JSONObject cellAttrs = cell.getJSONObject("attrs");
                                if (cellAttrs != null) {
                                    boolean inLoopRange = false;
                                    if (startLoopIndex == -1 && endLoopIndex == -1) {
                                        inLoopRange = true;
                                    } else {
                                        inLoopRange = (j >= startLoopIndex && j <= endLoopIndex);
                                    }
                                    if (!inLoopRange) {
                                        rowCells.remove(j);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (rowIndex > 0) {
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

                        boolean isRowMerged = "true".equals(cellAttrs.getString("rowMerged"));
                        Object rowspanObj = cellAttrs.get("rowspan");
                        int currentRowspan = (rowspanObj instanceof Number) ? ((Number) rowspanObj).intValue() : 1;

                        if (!isRowMerged && currentRowspan <= 1) {
                            continue;
                        }

                        if (prevIdx + currentRowspan > rowIndex) {
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
     * 检测表格中是否有断开的合并单元格
     * 通过检查每行的实际列数是否一致来判断表格结构是否断开
     *
     * @param tableNode 表格节点
     * @return true 如果检测到断开的合并单元格（行列不一致），false 否则
     */
    private boolean detectBrokenRowspanForLoopRows(JSONObject tableNode) {
        if (tableNode == null) {
            return false;
        }

        JSONArray root = tableNode.getJSONArray("content");
        if (root == null || root.size() <= 1) {
            return false;
        }

        int baseColumnCount = calculateRowColumnCount(root, 0);
        if (baseColumnCount <= 0) {
            return false;
        }

        for (int rowIndex = 1; rowIndex < root.size(); rowIndex++) {
            int currentColumnCount = calculateRowColumnCount(root, rowIndex);
            if (currentColumnCount != baseColumnCount) {
                LOG.info("检测到表格行列不一致: 基准列数={}, 第{}行列数={}", baseColumnCount, rowIndex, currentColumnCount);
                return true;
            }
        }

        return false;
    }

    /**
     * 计算指定行的实际列数（考虑前面行的 rowspan 影响）
     *
     * @param root 表格行数组
     * @param rowIndex 行索引
     * @return 实际列数
     */
    private int calculateRowColumnCount(JSONArray root, int rowIndex) {
        if (root == null || rowIndex < 0 || rowIndex >= root.size()) {
            return 0;
        }

        JSONObject row = root.getJSONObject(rowIndex);
        if (row == null || !"tableRow".equals(row.getString("type"))) {
            return 0;
        }

        JSONArray cellContent = row.getJSONArray("content");
        if (cellContent == null) {
            return 0;
        }

        int columnCount = 0;


        for (int i = 0; i < cellContent.size(); i++) {
            JSONObject cell = cellContent.getJSONObject(i);
            if (cell == null) {
                continue;
            }

            JSONObject cellAttrs = cell.getJSONObject("attrs");
            if (cellAttrs == null) {
                columnCount++;
                continue;
            }

            Object colspanObj = cellAttrs.get("colspan");
            int colspan = (colspanObj instanceof Number) ? ((Number) colspanObj).intValue() : 1;
            columnCount += colspan;
        }

        for (int prevIdx = rowIndex - 1; prevIdx >= 0; prevIdx--) {
            JSONObject prevRow = root.getJSONObject(prevIdx);
            if (prevRow == null) {
                continue;
            }

            JSONArray prevCellContent = prevRow.getJSONArray("content");
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

                Object rowspanObj = cellAttrs.get("rowspan");
                int rowspan = (rowspanObj instanceof Number) ? ((Number) rowspanObj).intValue() : 1;

                if (prevIdx + rowspan > rowIndex) {
                    Object colspanObj = cellAttrs.get("colspan");
                    int colspan = (colspanObj instanceof Number) ? ((Number) colspanObj).intValue() : 1;
                    columnCount += colspan;
                }
            }
        }

        return columnCount;
    }

    /**
     * 修复断开的合并单元格（在表格处理完成后统一处理）
     * 通过迭代修复，直到表格行列一致为止
     *
     * @param tableNode 表格节点
     */
    private void fixBrokenRowspanForLoopRows(JSONObject tableNode) {
        if (tableNode == null) {
            return;
        }

        JSONArray root = tableNode.getJSONArray("content");
        if (root == null || root.size() <= 1) {
            return;
        }

        int maxIterations = 100;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            int baseColumnCount = calculateRowColumnCount(root, 0);
            if (baseColumnCount <= 0) {
                break;
            }

            boolean fixed = false;

            for (int rowIndex = 1; rowIndex < root.size(); rowIndex++) {
                int currentColumnCount = calculateRowColumnCount(root, rowIndex);
                if (currentColumnCount == baseColumnCount) {
                    continue;
                }

                LOG.info("发现行列不一致的行: 基准列数={}, 第{}行列数={}, 开始修复", baseColumnCount, rowIndex, currentColumnCount);

                for (int prevIdx = rowIndex - 1; prevIdx >= 0; prevIdx--) {
                    JSONObject prevRow = root.getJSONObject(prevIdx);
                    if (prevRow == null) {
                        continue;
                    }

                    JSONArray prevCellContent = prevRow.getJSONArray("content");
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

                        Object originalRowspanObj = cellAttrs.get("_originalRowspan");
                        if (!(originalRowspanObj instanceof Number)) {
                            continue;
                        }

                        int originalRowspan = ((Number) originalRowspanObj).intValue();
                        Object rowspanObj = cellAttrs.get("rowspan");
                        int currentRowspan = (rowspanObj instanceof Number) ? ((Number) rowspanObj).intValue() : 1;

                        if (prevIdx + currentRowspan <= rowIndex && originalRowspan > 1) {
                            int currentRowTrSize = calculateRowTrSize(root, rowIndex);

                            LOG.debug("检查合并单元格: prevIdx={}, currentRowspan={}, rowIndex={}, currentRowTrSize={}",
                                    prevIdx, currentRowspan, rowIndex, currentRowTrSize);

                            if (currentRowTrSize > 0) {
                                int newRowspan = currentRowspan + currentRowTrSize;
                                cellAttrs.put("rowspan", newRowspan);

                                LOG.info("修复断开的合并单元格: prevIdx={}, 原始 rowspan={}, 当前 rowspan={}, 新 rowspan={}, 当前循环展开行数={}, rowIndex={}",
                                        prevIdx, originalRowspan, currentRowspan, newRowspan, currentRowTrSize, rowIndex);
                                fixed = true;
                            } else {
                                if (originalRowspan > 1 && prevIdx + currentRowspan == rowIndex) {
                                    int nextRowTrSize = 0;
                                    for (int nextIdx = rowIndex; nextIdx < root.size(); nextIdx++) {
                                        int trSize = calculateRowTrSize(root, nextIdx);
                                        if (trSize > 0) {
                                            nextRowTrSize = trSize;
                                            break;
                                        }
                                    }

                                    if (nextRowTrSize > 0) {
                                        int newRowspan = currentRowspan + nextRowTrSize;
                                        cellAttrs.put("rowspan", newRowspan);
                                        LOG.info("修复断开的合并单元格(使用后续循环行): prevIdx={}, 原始 rowspan={}, 当前 rowspan={}, 新 rowspan={}, 后续循环展开行数={}, rowIndex={}",
                                                prevIdx, originalRowspan, currentRowspan, newRowspan, nextRowTrSize, rowIndex);
                                        fixed = true;
                                    }
                                }
                            }
                        }
                    }
                }

                if (!fixed) {
                    boolean moved = moveCellValuesForMarkedCells(root, rowIndex, baseColumnCount, currentColumnCount);
                    if (moved) {
                        fixed = true;
                        LOG.info("通过移动值修复行列不一致: rowIndex={}, 基准列数={}, 当前列数={}",
                                rowIndex, baseColumnCount, currentColumnCount);
                    } else {
                        LOG.warn("未找到需要修复的合并单元格: rowIndex={}, 基准列数={}, 当前列数={}",
                                rowIndex, baseColumnCount, currentColumnCount);
                    }
                }
            }

            if (!fixed) {
                break;
            }

            boolean stillBroken = false;
            for (int rowIndex = 1; rowIndex < root.size(); rowIndex++) {
                int currentColumnCount = calculateRowColumnCount(root, rowIndex);
                if (currentColumnCount != baseColumnCount) {
                    stillBroken = true;
                    break;
                }
            }

            if (!stillBroken) {
                LOG.info("表格行列已修复一致，共迭代{}次", iteration + 1);
                break;
            }
        }
    }

    /**
     * 计算指定行的展开行数（trSize）
     * 如果该行是循环行的展开行，返回该循环行的展开行数
     * 否则返回 0
     *
     * @param root 表格行数组
     * @param rowIndex 当前行索引
     * @return 展开行数，如果不是循环行的展开行则返回 0
     */
    private int calculateRowTrSize(JSONArray root, int rowIndex) {
        if (rowIndex <= 0 || root == null) {
            return 0;
        }

        JSONObject currentRow = root.getJSONObject(rowIndex);
        if (currentRow == null) {
            return 0;
        }

        JSONObject rowAttrs = currentRow.getJSONObject("attrs");
        if (rowAttrs == null) {
            return 0;
        }

        Object startLoopIndexObj = rowAttrs.get("startLoopIndex");
        Object endLoopIndexObj = rowAttrs.get("endLoopIndex");
        int startLoopIndex = parseLoopIndex(startLoopIndexObj);
        int endLoopIndex = parseLoopIndex(endLoopIndexObj);

        if (startLoopIndex == -1 || endLoopIndex == -1) {
            return 0;
        }


        int trSize = 0;

        for (int i = rowIndex; i >= 0; i--) {
            JSONObject row = root.getJSONObject(i);
            if (row == null) {
                break;
            }

            JSONObject attrs = row.getJSONObject("attrs");
            if (attrs == null) {
                break;
            }

            int si = parseLoopIndex(attrs.get("startLoopIndex"));
            int ei = parseLoopIndex(attrs.get("endLoopIndex"));
            if (si != -1 && ei != -1) {
                if (si == startLoopIndex && ei == endLoopIndex) {
                    trSize++;
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        if (trSize > 0) {
            for (int i = rowIndex + 1; i < root.size(); i++) {
                JSONObject row = root.getJSONObject(i);
                if (row == null) {
                    break;
                }

                JSONObject attrs = row.getJSONObject("attrs");
                if (attrs == null) {
                    break;
                }

                int si = parseLoopIndex(attrs.get("startLoopIndex"));
                int ei = parseLoopIndex(attrs.get("endLoopIndex"));
                if (si != -1 && ei != -1) {
                    if (si == startLoopIndex && ei == endLoopIndex) {
                        trSize++;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }

        return trSize;
    }

    /**
     * 移动单元格值来修复行列不一致（有标记的扩大）
     * 查找有 _originalRowspan 标记的合并单元格，如果后续行有值，移动到左侧或右侧的空位置
     *
     * @param root 表格行数组
     * @param rowIndex 当前行索引
     * @param baseColumnCount 基准列数
     * @param currentColumnCount 当前列数
     * @return true 如果成功移动了值，false 否则
     */
    private boolean moveCellValuesForMarkedCells(JSONArray root, int rowIndex, int baseColumnCount, int currentColumnCount) {
        if (root == null || rowIndex < 0 || rowIndex >= root.size()) {
            return false;
        }

        for (int prevIdx = rowIndex - 1; prevIdx >= 0; prevIdx--) {
            JSONObject prevRow = root.getJSONObject(prevIdx);
            if (prevRow == null) {
                continue;
            }

            JSONArray prevCellContent = prevRow.getJSONArray("content");
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

                Object originalRowspanObj = cellAttrs.get("_originalRowspan");
                if (!(originalRowspanObj instanceof Number)) {
                    continue;
                }

                Object rowspanObj = cellAttrs.get("rowspan");
                int currentRowspan = (rowspanObj instanceof Number) ? ((Number) rowspanObj).intValue() : 1;

                if (prevIdx + currentRowspan > rowIndex) {
                    for (int nextRowIdx = rowIndex; nextRowIdx < root.size() && nextRowIdx < prevIdx + currentRowspan; nextRowIdx++) {
                        JSONObject nextRow = root.getJSONObject(nextRowIdx);
                        if (nextRow == null) {
                            continue;
                        }

                        JSONArray nextCellContent = nextRow.getJSONArray("content");
                        if (nextCellContent == null || nextCellContent.isEmpty()) {
                            continue;
                        }

                        for (int j = 0; j < nextCellContent.size(); j++) {
                            JSONObject nextCell = nextCellContent.getJSONObject(j);
                            if (nextCell == null) {
                                continue;
                            }

                            JSONArray cellContentArray = nextCell.getJSONArray("content");
                            if (cellContentArray == null || cellContentArray.isEmpty()) {
                                continue;
                            }

                            boolean hasContent = false;
                            for (int k = 0; k < cellContentArray.size(); k++) {
                                JSONObject para = cellContentArray.getJSONObject(k);
                                if (para != null) {
                                    JSONArray paraContent = para.getJSONArray("content");
                                    if (paraContent != null && !paraContent.isEmpty()) {
                                        hasContent = true;
                                        break;
                                    }
                                }
                            }

                            if (hasContent) {
                                boolean moved = moveCellToEmptyPosition(root, nextRowIdx, j, baseColumnCount, currentColumnCount);
                                if (moved) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 将单元格移动到左侧或右侧的空位置
     *
     * @param root 表格行数组
     * @param rowIndex 行索引
     * @param cellIndex 单元格索引
     * @param baseColumnCount 基准列数
     * @param currentColumnCount 当前列数
     * @return true 如果成功移动，false 否则
     */
    private boolean moveCellToEmptyPosition(JSONArray root, int rowIndex, int cellIndex, int baseColumnCount, int currentColumnCount) {
        if (root == null || rowIndex < 0 || rowIndex >= root.size()) {
            return false;
        }

        JSONObject row = root.getJSONObject(rowIndex);
        if (row == null) {
            return false;
        }

        JSONArray cellContent = row.getJSONArray("content");
        if (cellContent == null || cellIndex < 0 || cellIndex >= cellContent.size()) {
            return false;
        }

        JSONObject cellToMove = cellContent.getJSONObject(cellIndex);
        if (cellToMove == null) {
            return false;
        }

        int actualColumnPos = calculateActualColumnPosition(root, rowIndex, cellIndex);

        if (currentColumnCount < baseColumnCount) {
            for (int targetPos = baseColumnCount - 1; targetPos > actualColumnPos; targetPos--) {
                int targetCellIndex = findCellIndexAtPosition(root, rowIndex, targetPos);
                if (targetCellIndex < 0) {
                    JSONObject newCell = createEmptyTableCell();
                    JSONArray newCellContent = newCell.getJSONArray("content");
                    if (newCellContent != null) {
                        JSONArray originalContent = cellToMove.getJSONArray("content");
                        if (originalContent != null && !originalContent.isEmpty()) {
                            newCellContent.clear();
                            for (int i = 0; i < originalContent.size(); i++) {
                                JSONObject para = originalContent.getJSONObject(i);
                                if (para != null) {
                                    JSONObject newPara = JsonUtils.parseJsonObjectWithArm(JsonUtils.toJSONStringWithArm(para));
                                    newCellContent.add(newPara);
                                }
                            }
                        }
                    }

                    int insertIndex = cellContent.size();
                    cellContent.add(insertIndex, newCell);

                    JSONArray originalContent = cellToMove.getJSONArray("content");
                    if (originalContent != null) {
                        originalContent.clear();
                        JSONObject emptyPara = new JSONObject();
                        emptyPara.put("type", "paragraph");
                        emptyPara.put("attrs", new JSONObject());
                        emptyPara.put("content", new JSONArray());
                        originalContent.add(emptyPara);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 计算单元格的实际列位置（考虑前面行的 rowspan）
     *
     * @param root 表格行数组
     * @param rowIndex 行索引
     * @param cellIndex 单元格索引
     * @return 实际列位置
     */
    private int calculateActualColumnPosition(JSONArray root, int rowIndex, int cellIndex) {
        int position = 0;

        JSONObject row = root.getJSONObject(rowIndex);
        if (row != null) {
            JSONArray cellContent = row.getJSONArray("content");
            if (cellContent != null) {
                for (int i = 0; i < cellIndex && i < cellContent.size(); i++) {
                    JSONObject cell = cellContent.getJSONObject(i);
                    if (cell != null) {
                        JSONObject cellAttrs = cell.getJSONObject("attrs");
                        if (cellAttrs != null) {
                            Object colspanObj = cellAttrs.get("colspan");
                            int colspan = (colspanObj instanceof Number) ? ((Number) colspanObj).intValue() : 1;
                            position += colspan;
                        } else {
                            position++;
                        }
                    }
                }
            }
        }

        for (int prevIdx = rowIndex - 1; prevIdx >= 0; prevIdx--) {
            JSONObject prevRow = root.getJSONObject(prevIdx);
            if (prevRow == null) {
                continue;
            }

            JSONArray prevCellContent = prevRow.getJSONArray("content");
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

                Object rowspanObj = cellAttrs.get("rowspan");
                int rowspan = (rowspanObj instanceof Number) ? ((Number) rowspanObj).intValue() : 1;

                if (prevIdx + rowspan > rowIndex) {
                    Object colspanObj = cellAttrs.get("colspan");
                    int colspan = (colspanObj instanceof Number) ? ((Number) colspanObj).intValue() : 1;
                    position += colspan;
                }
            }
        }

        return position;
    }

    /**
     * 查找指定列位置的单元格索引
     *
     * @param root 表格行数组
     * @param rowIndex 行索引
     * @param targetPosition 目标列位置
     * @return 单元格索引，如果不存在则返回 -1
     */
    private int findCellIndexAtPosition(JSONArray root, int rowIndex, int targetPosition) {
        int currentPosition = 0;

        JSONObject row = root.getJSONObject(rowIndex);
        if (row == null) {
            return -1;
        }

        JSONArray cellContent = row.getJSONArray("content");
        if (cellContent == null) {
            return -1;
        }

        for (int prevIdx = rowIndex - 1; prevIdx >= 0; prevIdx--) {
            JSONObject prevRow = root.getJSONObject(prevIdx);
            if (prevRow == null) {
                continue;
            }

            JSONArray prevCellContent = prevRow.getJSONArray("content");
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

                Object rowspanObj = cellAttrs.get("rowspan");
                int rowspan = (rowspanObj instanceof Number) ? ((Number) rowspanObj).intValue() : 1;

                if (prevIdx + rowspan > rowIndex) {
                    Object colspanObj = cellAttrs.get("colspan");
                    int colspan = (colspanObj instanceof Number) ? ((Number) colspanObj).intValue() : 1;
                    currentPosition += colspan;
                }
            }
        }

        for (int i = 0; i < cellContent.size(); i++) {
            JSONObject cell = cellContent.getJSONObject(i);
            if (cell == null) {
                continue;
            }

            JSONObject cellAttrs = cell.getJSONObject("attrs");
            if (cellAttrs == null) {
                currentPosition++;
            } else {
                Object colspanObj = cellAttrs.get("colspan");
                int colspan = (colspanObj instanceof Number) ? ((Number) colspanObj).intValue() : 1;
                if (currentPosition <= targetPosition && targetPosition < currentPosition + colspan) {
                    return i;
                }
                currentPosition += colspan;
            }
        }

        return -1;
    }

    /**
     * 创建空的表格单元格
     *
     * @return 空的表格单元格 JSONObject
     */
    private JSONObject createEmptyTableCell() {
        JSONObject cell = new JSONObject();
        cell.put("type", "tableCell");

        JSONObject attrs = new JSONObject();
        attrs.put("id", IdUtil.simpleUUID().replace("-", ""));
        attrs.put("colspan", 1);
        attrs.put("rowspan", 1);
        cell.put("attrs", attrs);

        JSONArray content = new JSONArray();
        JSONObject paragraph = new JSONObject();
        paragraph.put("type", "paragraph");
        paragraph.put("attrs", new JSONObject());
        paragraph.put("content", new JSONArray());
        content.add(paragraph);
        cell.put("content", content);

        return cell;
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
     * @param paraContent 段落内容数组
     * @param nodeIndex 节点索引
     * @param parameters 参数对象
     * @param logPerfix 日志前缀
     * @param parentParagraph 父段落节点（可选，用于替换整个段落）
     * @param cellContentArray 单元格内容数组（可选，用于替换整个段落）
     * @param paraIndex 段落索引（可选，用于替换整个段落）
     * @return 如果返回true，表示需要替换整个段落，调用方应该处理段落替换
     */
    private boolean textRenderNewFormat(JSONArray paraContent, int nodeIndex, JSONObject parameters, String logPerfix,
                                        JSONObject parentParagraph, JSONArray cellContentArray, Integer paraIndex) {

        if (StringUtils.isEmpty(logPerfix)) {
            logPerfix = "";
        }

        if (paraContent == null || nodeIndex < 0 || nodeIndex >= paraContent.size()) {
            return false;
        }

        JSONObject node = paraContent.getJSONObject(nodeIndex);
        if (node == null) {
            return false;
        }

        String nodeType = node.getString("type");

        if ("inlineTemplate".equals(nodeType)) {
            JSONArray inlineContent = node.getJSONArray("content");
            if (inlineContent != null) {
                StringBuilder combinedText = new StringBuilder();
                for (int i = 0; i < inlineContent.size(); i++) {
                    JSONObject innerNode = inlineContent.getJSONObject(i);
                    if (innerNode != null && "text".equals(innerNode.getString("type"))) {
                        String text = innerNode.getString("text");
                        if (text != null) {
                            combinedText.append(text);
                        }
                    }
                }

                String variableName = combinedText.toString();
                if (StringUtils.isNotBlank(variableName)) {
                    NodeData nodeData = getValueByVariableName(parameters, variableName);
                    if (nodeData != null && ObjectUtil.isNotEmpty(nodeData.getValue())) {
                        String dataType = nodeData.getType();

                        if (StrUtil.equals("sub_workflow", dataType)) {
                            JSONArray subWorkflowContent = processSubWorkflow(nodeData);
                            if (subWorkflowContent != null && subWorkflowContent.size() > 0) {
                                LOG.info("textRenderNewFormat sub_workflow content size: {}", subWorkflowContent.size());
                                boolean isBlockLevel = false;
                                for (int i = 0; i < subWorkflowContent.size(); i++) {
                                    JSONObject contentNode = subWorkflowContent.getJSONObject(i);
                                    if (contentNode != null) {
                                        String contentType = contentNode.getString("type");
                                        if ("paragraph".equals(contentType) || "heading".equals(contentType) ||
                                                "bulletList".equals(contentType) || "orderedList".equals(contentType) ||
                                                "codeBlock".equals(contentType) || "blockquote".equals(contentType) ||
                                                "table".equals(contentType) || "image".equals(contentType)) {
                                            isBlockLevel = true;
                                            break;
                                        }
                                    }
                                }

                                if (isBlockLevel && parentParagraph != null && cellContentArray != null && paraIndex != null) {
                                    cellContentArray.remove(paraIndex.intValue());
                                    for (int i = subWorkflowContent.size() - 1; i >= 0; i--) {
                                        cellContentArray.add(paraIndex.intValue(), subWorkflowContent.getJSONObject(i));
                                    }
                                    return true;
                                } else {
                                    paraContent.remove(nodeIndex);
                                    for (int i = subWorkflowContent.size() - 1; i >= 0; i--) {
                                        paraContent.add(nodeIndex, subWorkflowContent.getJSONObject(i));
                                    }
                                    return false;
                                }
                            }
                        }

                        if (StrUtil.equals("sub_workflow_list", dataType)) {
                            JSONArray subWorkflowListContent = processSubWorkflowList(nodeData);
                            if (subWorkflowListContent != null && subWorkflowListContent.size() > 0) {
                                LOG.info("textRenderNewFormat sub_workflow_list content size: {}", subWorkflowListContent.size());
                                boolean isBlockLevel = false;
                                for (int i = 0; i < subWorkflowListContent.size(); i++) {
                                    JSONObject contentNode = subWorkflowListContent.getJSONObject(i);
                                    if (contentNode != null) {
                                        String contentType = contentNode.getString("type");
                                        if ("paragraph".equals(contentType) || "heading".equals(contentType) ||
                                                "bulletList".equals(contentType) || "orderedList".equals(contentType) ||
                                                "codeBlock".equals(contentType) || "blockquote".equals(contentType) ||
                                                "table".equals(contentType) || "image".equals(contentType)) {
                                            isBlockLevel = true;
                                            break;
                                        }
                                    }
                                }

                                if (isBlockLevel && parentParagraph != null && cellContentArray != null && paraIndex != null) {
                                    cellContentArray.remove(paraIndex.intValue());
                                    for (int i = subWorkflowListContent.size() - 1; i >= 0; i--) {
                                        cellContentArray.add(paraIndex.intValue(), subWorkflowListContent.getJSONObject(i));
                                    }
                                    return true;
                                } else {
                                    paraContent.remove(nodeIndex);
                                    for (int i = subWorkflowListContent.size() - 1; i >= 0; i--) {
                                        paraContent.add(nodeIndex, subWorkflowListContent.getJSONObject(i));
                                    }
                                    return false;
                                }
                            }
                        }
                    }
                }

                for (int i = 0; i < inlineContent.size(); i++) {
                    JSONObject innerNode = inlineContent.getJSONObject(i);

                    if (innerNode != null && "text".equals(innerNode.getString("type"))) {
                        String textValue = innerNode.getString("text");

                        if (StringUtils.isNotBlank(textValue)) {
                            JSONArray innerMarks = innerNode.getJSONArray("marks");
                            ReplacementResultTable replacement = replaceTemplateVariableNewFormat(textValue, parameters, logPerfix, innerMarks);
                            if (replacement != null) {
                                if (replacement.getTiptapContent() != null && replacement.getTiptapContent().get("content") != null) {
                                    Object contentObj = replacement.getTiptapContent().get("content");
                                    JSONArray newBlocks = new JSONArray();
                                    if (contentObj instanceof List) {
                                        @SuppressWarnings("unchecked")
                                        List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentObj;
                                        for (Map<String, Object> item : contentList) {
                                            newBlocks.add(new JSONObject(item));
                                        }
                                    } else if (contentObj instanceof JSONArray) {
                                        newBlocks = (JSONArray) contentObj;
                                    }

                                    if (!newBlocks.isEmpty()) {
                                        if (parentParagraph != null && cellContentArray != null && paraIndex != null) {
                                            cellContentArray.remove(paraIndex.intValue());
                                            for (int b = newBlocks.size() - 1; b >= 0; b--) {
                                                cellContentArray.add(paraIndex.intValue(), newBlocks.getJSONObject(b));
                                            }
                                            return true;
                                        } else {
                                            paraContent.remove(nodeIndex);
                                            for (int b = newBlocks.size() - 1; b >= 0; b--) {
                                                paraContent.add(nodeIndex, newBlocks.getJSONObject(b));
                                            }
                                            return false;
                                        }
                                    }
                                }

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

            return false;
        }

        if ("text".equals(nodeType)) {
            String textValue = node.getString("text");
            if (StringUtils.isNotBlank(textValue)) {
                JSONArray nodeMarks = node.getJSONArray("marks");
                ReplacementResultTable replacement = replaceTemplateVariableNewFormat(textValue, parameters, logPerfix, nodeMarks);
                if (replacement != null) {
                    if (replacement.getTiptapContent() != null && replacement.getTiptapContent().get("content") != null) {
                        Object contentObj = replacement.getTiptapContent().get("content");
                        JSONArray newBlocks = new JSONArray();
                        if (contentObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentObj;
                            for (Map<String, Object> item : contentList) {
                                newBlocks.add(new JSONObject(item));
                            }
                        } else if (contentObj instanceof JSONArray) {
                            newBlocks = (JSONArray) contentObj;
                        }

                        if (!newBlocks.isEmpty()) {
                            paraContent.remove(nodeIndex);
                            for (int b = newBlocks.size() - 1; b >= 0; b--) {
                                paraContent.add(nodeIndex, newBlocks.getJSONObject(b));
                            }
                            return false;
                        }
                    }

                    node.put("text", replacement.getText());
                    node.remove("isTemplate");
                    applySourceIdToMarksTable(node, replacement.getGroupIds());
                }
            }
        }
        return false;
    }

    /**
     * 判断节点是否为图片节点
     */
    private boolean isImageNode(JSONObject node) {
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
        if (shouldDeleteTableImageNode(imageNode, parameters)) {
            parentArray.remove(index);
            if (parentArray.isEmpty()) {
                JSONObject emptyParagraph = new JSONObject();
                emptyParagraph.put("type", "paragraph");
                JSONObject paraAttrs = new JSONObject();
                paraAttrs.put("id", IdUtil.simpleUUID());
                emptyParagraph.put("attrs", paraAttrs);
                parentArray.add(emptyParagraph);
            }
            return index - 1;
        }
        String position = IdUtil.simpleUUID().replace("-", "");
        Pair<JSONArray, String> result = ElementFormatterUtil.otherTypeRenderData(imageNode, parameters, "", position);
        parentArray.remove(index);
        if (result == null || "delete".equals(result.getRight())) {
            if (parentArray.isEmpty()) {
                JSONObject emptyParagraph = new JSONObject();
                emptyParagraph.put("type", "paragraph");
                JSONObject paraAttrs = new JSONObject();
                paraAttrs.put("id", IdUtil.simpleUUID());
                emptyParagraph.put("attrs", paraAttrs);
                parentArray.add(emptyParagraph);
            }
            return index - 1;
        }
        JSONArray newNodes = result.getLeft();
        if (newNodes == null || newNodes.isEmpty()) {
            if (parentArray.isEmpty()) {
                JSONObject emptyParagraph = new JSONObject();
                emptyParagraph.put("type", "paragraph");
                JSONObject paraAttrs = new JSONObject();
                paraAttrs.put("id", IdUtil.simpleUUID());
                emptyParagraph.put("attrs", paraAttrs);
                parentArray.add(emptyParagraph);
            }
            return index - 1;
        }
        for (int i = newNodes.size() - 1; i >= 0; i--) {
            parentArray.add(index, newNodes.getJSONObject(i));
        }
        return index + newNodes.size() - 1;
    }

    /**
     * 检查表格中的图片节点是否应该被删除（valueList 为空）
     * 复用 ElementFormatterUtil.otherTypeRenderData 的逻辑来获取 value
     * @param imageNode 图片节点
     * @param parameters 参数对象
     * @return true 如果应该删除，false 否则
     */
    private boolean shouldDeleteTableImageNode(JSONObject imageNode, JSONObject parameters) {
        if (imageNode == null || parameters == null) {
            return false;
        }
        JSONObject attrs = imageNode.getJSONObject("attrs");
        if (attrs == null) {
            return false;
        }
        String imageValueRaw = attrs.getString("chartValue");
        if (StrUtil.isBlank(imageValueRaw)) {
            imageValueRaw = attrs.getString("src");
        }
        if (StrUtil.isBlank(imageValueRaw)) {
            return false;
        }
        boolean isPlaceholder = false;
        String key = null;
        if (imageValueRaw.startsWith("{{") && imageValueRaw.endsWith("}}")) {
            isPlaceholder = true;
            key = imageValueRaw.replaceFirst("^\\{\\{([^\\{].*[^\\}])\\}\\}$", "$1");
        }
        if (!isPlaceholder || !parameters.containsKey(key)) {
            return false;
        }
        JSONObject paramObj = parameters.getJSONObject(key);
        if (paramObj == null || !paramObj.containsKey("value")) {
            return false;
        }
        Object value = paramObj.get("value");
        if (value instanceof JSONObject) {
            JSONObject valueObj = (JSONObject) value;
            String type = valueObj.getString("type");
            if (StrUtil.isNotBlank(type) && type.startsWith("chart_")) {
                if (StrUtil.equals("chart_equity_penetration", type)) {
                    Object valueListObj = valueObj.get("valueList");
                    if (valueListObj == null ||
                            (valueListObj instanceof List && ((List<?>) valueListObj).isEmpty()) ||
                            (valueListObj instanceof JSONArray && ((JSONArray) valueListObj).isEmpty())) {
                        return true;
                    }
                } else {
                    Object valueListObj = valueObj.get("valueList");
                    if (valueListObj == null ||
                            (valueListObj instanceof List && ((List<?>) valueListObj).isEmpty()) ||
                            (valueListObj instanceof JSONArray && ((JSONArray) valueListObj).isEmpty())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 处理表格内部的 conditionTemplate 节点
     */
    private void processConditionTemplatesInTable(JSONObject tableNode, JSONObject parameters) {
        if (tableNode == null) {
            return;
        }
        JSONArray rows = tableNode.getJSONArray("content");
        if (rows == null || rows.isEmpty()) {
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (row == null || !"tableRow".equals(row.getString("type"))) {
                continue;
            }
            JSONArray cells = row.getJSONArray("content");
            if (cells == null) {
                continue;
            }
            for (int j = 0; j < cells.size(); j++) {
                JSONObject cell = cells.getJSONObject(j);
                if (cell == null) {
                    continue;
                }
                JSONArray cellContentArray = cell.getJSONArray("content");
                if (cellContentArray == null) {
                    continue;
                }

                for (int k = 0; k < cellContentArray.size(); k++) {
                    JSONObject innerNode = cellContentArray.getJSONObject(k);
                    if (innerNode == null) {
                        continue;
                    }
                    if ("conditionTemplate".equals(innerNode.getString("type"))) {
                        JSONArray replacementNodes = processContentNode(innerNode, parameters);
                        cellContentArray.remove(k);
                        if (replacementNodes != null && !replacementNodes.isEmpty()) {
                            for (int r = replacementNodes.size() - 1; r >= 0; r--) {
                                cellContentArray.add(k, replacementNodes.getJSONObject(r));
                            }
                            k += replacementNodes.size() - 1;
                        } else {
                            k--;
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理表格内的所有节点（在表格处理完成后统一处理）
     * 遍历表格的所有行和单元格，对单元格内的所有节点进行递归处理
     * 包括：paragraph、heading、bulletList、orderedList、codeBlock、blockquote、checkbox、radio 等
     *
     * @param tableNode 表格节点
     * @param parameters 参数对象
     */
    private void processAllNodesInTable(JSONObject tableNode, JSONObject parameters) {
        if (tableNode == null) {
            return;
        }
        JSONArray rows = tableNode.getJSONArray("content");
        if (rows == null || rows.isEmpty()) {
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (row == null || !"tableRow".equals(row.getString("type"))) {
                continue;
            }
            JSONArray cells = row.getJSONArray("content");
            if (cells == null) {
                continue;
            }
            for (int j = 0; j < cells.size(); j++) {
                JSONObject cell = cells.getJSONObject(j);
                if (cell == null) {
                    continue;
                }
                JSONArray cellContentArray = cell.getJSONArray("content");
                if (cellContentArray == null) {
                    continue;
                }

                for (int k = cellContentArray.size() - 1; k >= 0; k--) {
                    JSONObject innerNode = cellContentArray.getJSONObject(k);
                    if (innerNode == null) {
                        continue;
                    }
                    String nodeType = innerNode.getString("type");


                    if ("table".equals(nodeType)) {
                        Map<String, JSONObject> nestedKeyDataMap = new HashMap<>();
                        processTable(innerNode, nestedKeyDataMap, parameters);
                        continue;
                    }

                    if ("conditionTemplate".equals(nodeType)) {
                        continue;
                    }

                    JSONArray replacementNodes = processContentNode(innerNode, parameters);
                    if (replacementNodes != null && !replacementNodes.isEmpty()) {
                        cellContentArray.remove(k);
                        for (int r = replacementNodes.size() - 1; r >= 0; r--) {
                            cellContentArray.add(k, replacementNodes.getJSONObject(r));
                        }
                    }
                }
            }
        }
    }

    /**
     * 仅处理空内容或被清理为 null 的单元格，补一个空段落占位，避免表格校验错误。
     * 不改动已有的块级/行级结构，降低对现有模板的影响。
     */
    private void normalizeEmptyTableCells(JSONObject tableNode) {
        if (tableNode == null) {
            return;
        }
        JSONArray rows = tableNode.getJSONArray("content");
        if (rows == null || rows.isEmpty()) {
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (row == null || !"tableRow".equals(row.getString("type"))) {
                continue;
            }

            JSONArray cells = row.getJSONArray("content");
            if (cells == null) {
                continue;
            }

            for (int j = 0; j < cells.size(); j++) {
                JSONObject cell = cells.getJSONObject(j);
                if (cell == null) {
                    continue;
                }

                JSONArray cellContent = cell.getJSONArray("content");

                if (cellContent == null || cellContent.isEmpty()) {
                    cell.put("content", createEmptyParagraphArray());
                    continue;
                }


                JSONArray cleaned = new JSONArray();
                for (int k = 0; k < cellContent.size(); k++) {
                    JSONObject child = cellContent.getJSONObject(k);
                    if (child != null) {
                        cleaned.add(child);
                    }
                }
                if (cleaned.isEmpty()) {
                    cell.put("content", createEmptyParagraphArray());
                } else if (cleaned.size() != cellContent.size()) {
                    cell.put("content", cleaned);
                }
            }
        }
    }

    /**
     * 生成仅包含一个空 paragraph 的内容数组
     */
    private JSONArray createEmptyParagraphArray() {
        JSONArray arr = new JSONArray();
        JSONObject paragraph = new JSONObject();
        paragraph.put("type", "paragraph");
        paragraph.put("content", new JSONArray());
        arr.add(paragraph);
        return arr;
    }

    /**
     * 删除表格内空文本节点，避免 Tiptap 报错 "Empty text nodes are not allowed"
     */
    private void cleanupEmptyTextNodesInTable(JSONObject tableNode) {
        if (tableNode == null) {
            return;
        }
        JSONArray rows = tableNode.getJSONArray("content");
        if (rows == null || rows.isEmpty()) {
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (row == null || !"tableRow".equals(row.getString("type"))) {
                continue;
            }

            JSONArray cells = row.getJSONArray("content");
            if (cells == null) {
                continue;
            }

            for (int j = 0; j < cells.size(); j++) {
                JSONObject cell = cells.getJSONObject(j);
                if (cell == null) {
                    continue;
                }
                JSONArray cellContent = cell.getJSONArray("content");
                if (cellContent == null) {
                    continue;
                }

                for (int k = 0; k < cellContent.size(); k++) {
                    JSONObject block = cellContent.getJSONObject(k);
                    if (block == null) {
                        continue;
                    }
                    String blockType = block.getString("type");
                    if (!"paragraph".equals(blockType) && !"heading".equals(blockType)) {
                        continue;
                    }

                    JSONArray paraContent = block.getJSONArray("content");
                    if (paraContent == null || paraContent.isEmpty()) {
                        continue;
                    }

                    for (int idx = paraContent.size() - 1; idx >= 0; idx--) {
                        JSONObject inline = paraContent.getJSONObject(idx);
                        if (inline == null) {
                            paraContent.remove(idx);
                            continue;
                        }
                        if (!"text".equals(inline.getString("type"))) {
                            continue;
                        }
                        String t = inline.getString("text");
                        if (t == null || t.isEmpty()) {
                            JSONArray nodeContent = inline.getJSONArray("content");
                            if (nodeContent == null || nodeContent.isEmpty()) {
                                paraContent.remove(idx);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 替换新格式中的模板变量
     */
    private ReplacementResultTable replaceTemplateVariableNewFormat(String textValue, JSONObject parameters, String logPerfix) {
        return replaceTemplateVariableNewFormat(textValue, parameters, logPerfix, null);
    }

    /**
     * 替换新格式中的模板变量（带原始marks样式）
     */
    private ReplacementResultTable replaceTemplateVariableNewFormat(String textValue, JSONObject parameters, String logPerfix, JSONArray originalMarks) {
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
                String path = buildJsonPathFromColumnKey(jsonDataPath);

                String replaceKey = "{{" + matcher.group(1) + "#" + matcher.group(2) + "#" + matcher.group(3) + "#" + matcher.group(4) + "}}";

                try {
                    JSONObject paramObj = parameters.getJSONObject(key + "#" + value);
                    if (paramObj == null) {
                        LOG.info("replaceTemplateVariableNewFormat: 参数对象不存在, key={}, value={}", key, value);
                        continue;
                    }
                    Object valueObj = paramObj.get("value");
                    if (valueObj == null) {
                        LOG.info("replaceTemplateVariableNewFormat: value字段不存在, key={}, value={}", key, value);
                        continue;
                    }

                    Object eval = null;
                    if (valueObj instanceof JSONObject) {
                        eval = JSONPath.eval(JsonUtils.toJSONStringWithArm((JSONObject) valueObj), path);
                    } else if (valueObj instanceof JSONArray) {
                        String pos = matcher.group(3).replace("$", "");
                        int index = 0;
                        if (StringUtils.isNotBlank(pos)) {
                            index = Integer.parseInt(pos) - 1;
                        }
                        if (index < ((JSONArray) valueObj).size()) {
                            Object item = ((JSONArray) valueObj).get(index);
                            if (item instanceof JSONObject) {
                                eval = JSONPath.eval(JsonUtils.toJSONStringWithArm((JSONObject) item), path);
                            } else {
                                eval = item;
                            }
                        }
                    } else {
                        eval = valueObj;
                    }

                    String v = eval == null ? "" : JsonUtils.toJSONStringWithArm(eval);
                    result = result.replace(replaceKey, v);
                    addGroupIdTable(parameters, groupIds, key, value);
                } catch (Exception e) {
                    LOG.info("replaceTemplateVariableNewFormat: 处理模板变量失败, key={}, value={}, path={}", key, value, path, e);
                }
            } catch (Exception e) {
                LOG.info(logPerfix + "文本插入渲染数据错误", e);
            }
        }


        Matcher startInputMatcher = START_INPUT_PATTERN.matcher(result);
        while (startInputMatcher.find()) {
            hasReplacement = true;
            try {
                String value = parameters.getJSONObject(startInputMatcher.group(1)).getString("value");
                result = result.replace("{{开始输入#" + startInputMatcher.group(1) + "}}", value);
            } catch (Exception e) {
                LOG.info(logPerfix + "文本渲染开始输入渲染数据错误", e);
            }
        }

        Matcher moduleValueMatcher = MODULE_VALUE_PATTERN.matcher(result);
        Map<String, Object> tiptapContent = null;
        while (moduleValueMatcher.find()) {
            hasReplacement = true;
            try {
                String key = moduleValueMatcher.group(1);
                String value = moduleValueMatcher.group(2);
                String moduleKey = key + "#" + value;
                String moduleValue = parameters.getJSONObject(moduleKey).getString("value");
                String replaceKey = "{{" + key + "#" + value + "}}";


                addGroupIdTable(parameters, groupIds, key, value);
                String sourceId = null;
                try {
                    sourceId = parameters.getJSONObject(moduleKey).getString("source_workflow_run_result_id");
                } catch (Exception e) {
                    LOG.debug("无法获取source_workflow_run_result_id，将使用null作为sourceId");
                }

                // 从原始marks中提取样式信息
                var optionsBuilder = MarkdownToTiptapConverter.ConvertOptions.builder()
                        .sourceId(sourceId)
                        .position(null);
                if (originalMarks != null) {
                    for (int mi = 0; mi < originalMarks.size(); mi++) {
                        JSONObject mark = originalMarks.getJSONObject(mi);
                        if (mark != null && "textStyle".equals(mark.getString("type"))) {
                            JSONObject markAttrs = mark.getJSONObject("attrs");
                            if (markAttrs != null) {
                                if (StringUtils.isNotBlank(markAttrs.getString("fontFamily"))) {
                                    optionsBuilder.font(markAttrs.getString("fontFamily"));
                                }
                                if (StringUtils.isNotBlank(markAttrs.getString("fontSize"))) {
                                    optionsBuilder.rawFontSize(markAttrs.getString("fontSize"));
                                }
                                if (StringUtils.isNotBlank(markAttrs.getString("color"))) {
                                    optionsBuilder.color(markAttrs.getString("color"));
                                }
                                if (StringUtils.isNotBlank(markAttrs.getString("backgroundColor"))) {
                                    optionsBuilder.backgroundColor(markAttrs.getString("backgroundColor"));
                                }
                            }
                            break;
                        }
                    }
                }
                var convertOptions = optionsBuilder.build();


                var tiptapDocument = MarkdownToTiptapConverter.convertMarkdownToTiptap(moduleValue, convertOptions);
                if (tiptapDocument != null) {
                    tiptapContent = tiptapDocument;
                    result = result.replace(replaceKey, "");
                } else {
                    result = result.replace(replaceKey, moduleValue);
                }
            } catch (Exception e) {
                LOG.error(logPerfix + "文本渲染模块值渲染数据错误", e);
            }
        }

        return hasReplacement ? new ReplacementResultTable(result, groupIds, tiptapContent) : null;
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
        private final Map<String, Object> tiptapContent;

        public ReplacementResultTable(String text, LinkedHashSet<String> groupIds) {
            this(text, groupIds, null);
        }

        public ReplacementResultTable(String text, LinkedHashSet<String> groupIds, Map<String, Object> tiptapContent) {
            this.text = text;
            this.groupIds = groupIds;
            this.tiptapContent = tiptapContent;
        }
    }

    /**
     设置全局变量
     @param subItem 子项
     @param parameters 参数对象
     @param logPerfix 日志前缀
     @return void
     */
    private void setGlobalVariable(JSONObject subItem, JSONObject parameters, String logPerfix) {
        Matcher startInputMatcher = START_INPUT_PATTERN.matcher(subItem.getString("value"));
        while (startInputMatcher.find()) {
            try {
                String value = parameters.getJSONObject(startInputMatcher.group(1)).getString("value");
                subItem.put("value", value);
            } catch (Exception e) {
                LOG.info( logPerfix + " text render start input render data error", e);
            }
        }
    }

    /**
     * 处理新格式条件模板（conditionTemplate）
     * 从 content[0].content[0].text 中提取条件表达式，替换变量，执行 FreeMarker 表达式，返回结果节点
     *
     * @param conditionNode 条件模板节点
     * @param parameters 参数对象
     * @return 处理后的节点数组，如果处理失败返回空数组
     */
    private JSONArray processConditionTemplate(JSONObject conditionNode, JSONObject parameters) {
        try {
            if (conditionNode == null) {
                return new JSONArray();
            }
            String nodeId = conditionNode.getJSONObject("attrs") != null ?
                    conditionNode.getJSONObject("attrs").getString("id") : "unknown";


            Map<String, Integer> nodePlaceholders = new HashMap<>();
            String conditionText = extractConditionText(conditionNode, nodePlaceholders, parameters);
            if (StringUtils.isBlank(conditionText)) {
                return new JSONArray();
            }


            String replacedText = replaceConditionVariables(conditionText, parameters);

            if (StringUtils.isBlank(replacedText)) {
                return new JSONArray();
            }

            String renderResult = executeConditionExpression(replacedText);

            if (StringUtils.isBlank(renderResult)) {
                return new JSONArray();
            }


            JSONArray resultNodes = parseRenderResultToNodes(renderResult, nodePlaceholders, conditionNode, parameters);


            return resultNodes;

        } catch (Exception e) {
            return new JSONArray();
        }
    }

    /**
     * 解析 FreeMarker 执行结果，按照结果中的顺序创建节点
     *
     * @param renderResult FreeMarker 执行结果
     * @param nodePlaceholders 占位符到节点索引的映射
     * @param conditionNode 条件模板节点
     * @param parameters 参数对象
     * @return 解析后的节点数组
     */
    private JSONArray parseRenderResultToNodes(String renderResult, Map<String, Integer> nodePlaceholders,
                                               JSONObject conditionNode, JSONObject parameters) {
        JSONArray resultNodes = new JSONArray();
        JSONArray conditionContent = conditionNode.getJSONArray("content");

        if (StringUtils.isBlank(renderResult) || conditionContent == null) {
            return resultNodes;
        }

        LOG.info("解析 FreeMarker 执行结果: {}", renderResult);

        Pattern pattern = Pattern.compile("(\\{\\{[^}]+\\}\\})");
        Matcher matcher = pattern.matcher(renderResult);

        int lastEnd = 0;
        StringBuilder currentText = new StringBuilder();

        while (matcher.find()) {
            int matchStart = matcher.start();

            if (matchStart > lastEnd) {
                String textBefore = renderResult.substring(lastEnd, matchStart);
                if (StringUtils.isNotBlank(textBefore.trim())) {
                    currentText.append(textBefore);
                }
            }

            String match = matcher.group(1);
            LOG.debug("处理占位符: {}", match);

            if (match.startsWith("{{NODE_")) {
                if (currentText.length() > 0) {
                    String textContent = currentText.toString().trim();
                    if (StringUtils.isNotBlank(textContent)) {
                        JSONObject textParagraph = createTextParagraph(textContent);
                        if (textParagraph != null) {
                            resultNodes.add(textParagraph);
                        }
                    }
                    currentText.setLength(0);
                }

                Integer nodeIndex = nodePlaceholders.get(match);
                if (nodeIndex != null && nodeIndex < conditionContent.size()) {
                    JSONObject node = conditionContent.getJSONObject(nodeIndex);
                    if (node != null) {
                        LOG.debug("处理节点 [{}]: {}", nodeIndex, node.getString("type"));
                        JSONArray processedNodes = processContentNode(node, parameters);
                        if (processedNodes != null && processedNodes.size() > 0) {
                            for (int i = 0; i < processedNodes.size(); i++) {
                                resultNodes.add(processedNodes.getJSONObject(i));
                            }
                        }
                    }
                } else {
                    LOG.warn("未找到占位符对应的节点: {}", match);
                }
            } else {
                String replacedValue = replaceVariablePlaceholder(match, parameters);
                if (replacedValue != null && !replacedValue.equals(match)) {
                    currentText.append(replacedValue);
                } else {
                    currentText.append(match);
                }
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < renderResult.length()) {
            String remainingText = renderResult.substring(lastEnd);
            if (StringUtils.isNotBlank(remainingText.trim())) {
                currentText.append(remainingText);
            }
        }

        if (currentText.length() > 0) {
            String textContent = currentText.toString().trim();
            if (StringUtils.isNotBlank(textContent)) {
                JSONObject textParagraph = createTextParagraph(textContent);
                if (textParagraph != null) {
                    resultNodes.add(textParagraph);
                }
            }
        }

        LOG.info("解析完成，生成 {} 个节点", resultNodes.size());
        return resultNodes;
    }

    /**
     * 替换变量占位符
     */
    private String replaceVariablePlaceholder(String placeholder, JSONObject parameters) {
        if (StringUtils.isBlank(placeholder) || !placeholder.startsWith("{{") || !placeholder.endsWith("}}")) {
            return placeholder;
        }

        String variablePattern = placeholder.substring(2, placeholder.length() - 2);
        

        try {
            Matcher fullMatcher = CONDITION_PATTERN.matcher(placeholder);
            if (fullMatcher.find()) {
                String key = fullMatcher.group(1);
                String value = fullMatcher.group(2);
                String posGroup = fullMatcher.group(3);
                String jsonDataPath = fullMatcher.group(4);
                String path = buildJsonPathFromColumnKey(jsonDataPath);

                Object resolvedValue = resolveParameterValue(key, value, posGroup, path, parameters);
                return resolvedValue != null ? String.valueOf(resolvedValue) : "";
            }

            Matcher startInputMatcher = CONDITION_START_INPUT_PATTERN.matcher(placeholder);
            if (startInputMatcher.find()) {
                String variableName = startInputMatcher.group(1);
                JSONObject variableObj = parameters.getJSONObject(variableName);
                Object rawValue = variableObj == null ? null : variableObj.get("value");
                return rawValue != null ? String.valueOf(rawValue) : "";
            }

            Matcher moduleMatcher = MODULE_VALUE_PATTERN.matcher(placeholder);
            if (moduleMatcher.find()) {
                String key = moduleMatcher.group(1);
                String value = moduleMatcher.group(2);
                String moduleKey = key + "#" + value;
                JSONObject paramObj = parameters.getJSONObject(moduleKey);
                Object rawValue = paramObj == null ? null : paramObj.get("value");
                return rawValue != null ? String.valueOf(rawValue) : "";
            }
        } catch (Exception e) {
            LOG.warn("替换变量占位符失败: {}", placeholder, e);
        }

        return placeholder;
    }




    /**
     * 创建包含文本的段落节点
     */
    private JSONObject createTextParagraph(String text) {
        if (StringUtils.isBlank(text.trim())) {
            return null;
        }

        JSONObject paragraph = new JSONObject();
        paragraph.put("type", "paragraph");
        paragraph.put("attrs", new JSONObject());

        JSONObject textNode = new JSONObject();
        textNode.put("type", "text");
        textNode.put("text", text.trim());
        textNode.put("marks", new JSONArray());

        JSONArray content = new JSONArray();
        content.add(textNode);
        paragraph.put("content", content);

        return paragraph;
    }

    /**
     * 条件分支信息
     */
    @Data
    private static class ConditionBranch {
        int startIndex;
        int endIndex;
        String branchType;
    }

    /**
     * 解析条件表达式，确定每个分支对应的节点范围
     * 通过分析条件文本中的标记来确定分支边界
     * 注意：分支范围包括所有节点类型（paragraph、table、image等）
     */
    private List<ConditionBranch> parseConditionBranches(String conditionText, JSONObject conditionNode) {
        List<ConditionBranch> branches = new ArrayList<>();
        JSONArray content = conditionNode.getJSONArray("content");
        if (content == null || content.isEmpty()) {
            return branches;
        }



        List<Integer> branchStartIndices = new ArrayList<>();
        List<Integer> branchEndIndices = new ArrayList<>();
        List<Integer> elseIfIndices = new ArrayList<>();

        for (int i = 0; i < content.size(); i++) {
            JSONObject node = content.getJSONObject(i);
            if (node == null) {
                continue;
            }

            String nodeType = node.getString("type");
            if ("paragraph".equals(nodeType)) {
                JSONArray paraContent = node.getJSONArray("content");
                if (paraContent != null) {
                    String paragraphText = extractTextFromParagraph(paraContent);
                    if (paragraphText != null) {
                        if (paragraphText.contains("<#if") && !paragraphText.contains("</#if")) {
                            branchStartIndices.add(i + 1);
                        } else if (paragraphText.contains("<#else") && !paragraphText.contains("</#if")) {
                            elseIfIndices.add(i);
                            branchStartIndices.add(i + 1);
                        } else if (paragraphText.contains("</#if")) {
                            branchEndIndices.add(i - 1);
                        }
                    }
                }
            }
        }

        if (branchStartIndices.isEmpty()) {
            for (int i = 0; i < content.size(); i++) {
                JSONObject node = content.getJSONObject(i);
                if (node != null && "paragraph".equals(node.getString("type"))) {
                    JSONArray paraContent = node.getJSONArray("content");
                    if (paraContent != null) {
                        String paragraphText = extractTextFromParagraph(paraContent);
                        if (paragraphText != null && paragraphText.contains("<#if")) {
                            branchStartIndices.add(i + 1);
                            break;
                        }
                    }
                }
            }
        }

        if (branchEndIndices.isEmpty()) {
            for (int i = content.size() - 1; i >= 0; i--) {
                JSONObject node = content.getJSONObject(i);
                if (node != null && "paragraph".equals(node.getString("type"))) {
                    JSONArray paraContent = node.getJSONArray("content");
                    if (paraContent != null) {
                        String paragraphText = extractTextFromParagraph(paraContent);
                        if (paragraphText != null && paragraphText.contains("</#if")) {
                            branchEndIndices.add(i - 1);
                            break;
                        }
                    }
                }
            }
            if (branchEndIndices.isEmpty()) {
                branchEndIndices.add(content.size() - 1);
            }
        }


        int startIndex = branchStartIndices.isEmpty() ? 0 : branchStartIndices.get(0);
        int endIndex = branchEndIndices.isEmpty() ? (content.size() - 1) : branchEndIndices.get(0);


        if (startIndex < 0) startIndex = 0;
        if (endIndex >= content.size()) endIndex = content.size() - 1;
        if (startIndex > endIndex) {
            return branches;
        }

        if (startIndex <= endIndex) {
            ConditionBranch firstBranch = new ConditionBranch();
            firstBranch.startIndex = startIndex;
            if (!elseIfIndices.isEmpty()) {
                firstBranch.endIndex = elseIfIndices.get(0) - 1;
            } else {
                firstBranch.endIndex = endIndex;
            }
            if (firstBranch.endIndex < firstBranch.startIndex) {
                firstBranch.endIndex = firstBranch.startIndex;
            }
            firstBranch.branchType = "if";
            branches.add(firstBranch);

            for (int i = 0; i < elseIfIndices.size(); i++) {
                ConditionBranch branch = new ConditionBranch();
                branch.startIndex = elseIfIndices.get(i) + 1;
                if (i < elseIfIndices.size() - 1) {
                    branch.endIndex = elseIfIndices.get(i + 1) - 1;
                } else {
                    branch.endIndex = endIndex;
                }
                if (branch.endIndex < branch.startIndex) {
                    branch.endIndex = branch.startIndex;
                }
                JSONObject elseNode = content.getJSONObject(elseIfIndices.get(i));
                String elseNodeType = elseNode != null ? elseNode.getString("type") : null;
                String elseText = null;
                if ("paragraph".equals(elseNodeType)) {
                    JSONArray elseParaContent = elseNode.getJSONArray("content");
                    if (elseParaContent != null) {
                        elseText = extractTextFromParagraph(elseParaContent);
                    }
                }
                branch.branchType = (elseText != null && elseText.contains("<#else>") && !elseText.contains("if"))
                        ? "else" : "elseif";
                branches.add(branch);
            }
        }

        return branches;
    }

    /**
     * 从 Tiptap 文档内容中提取所有文本
     */
    private void extractTextFromTiptapContent(JSONArray content, StringBuilder textBuilder) {
        if (content == null) {
            return;
        }
        for (int i = 0; i < content.size(); i++) {
            JSONObject node = content.getJSONObject(i);
            if (node == null) {
                continue;
            }
            String nodeType = node.getString("type");
            if ("paragraph".equals(nodeType) || "heading".equals(nodeType)) {
                JSONArray paraContent = node.getJSONArray("content");
                if (paraContent != null) {
                    String text = extractTextFromParagraph(paraContent);
                    if (text != null) {
                        textBuilder.append(text);
                    }
                }
            } else if ("text".equals(nodeType)) {
                String text = node.getString("text");
                if (text != null) {
                    textBuilder.append(text);
                }
            } else if (node.getJSONArray("content") != null) {
                // 递归处理子节点
                extractTextFromTiptapContent(node.getJSONArray("content"), textBuilder);
            }
        }
    }

    /**
     * 从段落内容中提取文本
     */
    private String extractTextFromParagraph(JSONArray paraContent) {
        if (paraContent == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < paraContent.size(); i++) {
            JSONObject item = paraContent.getJSONObject(i);
            if (item != null) {
                String itemType = item.getString("type");
                if ("text".equals(itemType)) {
                    String textValue = item.getString("text");
                    if (textValue != null) {
                        text.append(textValue);
                    }
                } else if ("inlineTemplate".equals(itemType)) {
                    JSONArray inlineContent = item.getJSONArray("content");
                    if (inlineContent != null) {
                        for (int j = 0; j < inlineContent.size(); j++) {
                            JSONObject innerNode = inlineContent.getJSONObject(j);
                            if (innerNode != null && "text".equals(innerNode.getString("type"))) {
                                String textValue = innerNode.getString("text");
                                if (textValue != null) {
                                    text.append(textValue);
                                }
                            }
                        }
                    }
                }
            }
        }
        return text.toString();
    }

    /**
     * 根据 FreeMarker 执行结果确定激活的分支
     * 简化实现：如果执行结果不为空，返回第一个分支（因为 FreeMarker 只返回满足条件的分支内容）
     */
    private int determineActiveBranch(String renderResult, List<ConditionBranch> branches, String replacedText) {
        if (branches.isEmpty()) {
            return -1;
        }

        if (StringUtils.isBlank(renderResult)) {
            return -1;
        }
        return 0;
    }

    /**
     * 判断文本是否为条件表达式的一部分
     */
    private boolean isConditionText(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        return text.contains("<#if") || text.contains("<#else") || text.contains("</#if");
    }

    /**
     * 提取条件表达式文本，包括所有节点类型（paragraph、table、image等）
     * 对于 table 和 image，使用占位符表示
     *
     * @param conditionNode 条件模板节点
     * @param nodePlaceholders 输出参数：占位符到节点索引的映射
     * @param parameters 参数对象，用于检查变量类型
     * @return 条件表达式文本（包含占位符）
     */
    private String extractConditionText(JSONObject conditionNode, Map<String, Integer> nodePlaceholders, JSONObject parameters) {
        try {
            JSONArray content = conditionNode.getJSONArray("content");
            if (content == null || content.isEmpty()) {
                return null;
            }

            StringBuilder conditionText = new StringBuilder();
            int tableIndex = 0;
            int imageIndex = 0;
            int paragraphIndex = 0;

            for (int nodeIndex = 0; nodeIndex < content.size(); nodeIndex++) {
                JSONObject node = content.getJSONObject(nodeIndex);
                if (node == null) {
                    continue;
                }

                String nodeType = node.getString("type");

                if ("paragraph".equals(nodeType)) {
                    JSONArray paraContent = node.getJSONArray("content");
                    if (paraContent == null || paraContent.isEmpty()) {
                        String placeholder = "{{NODE_PARAGRAPH_" + paragraphIndex + "}}";
                        nodePlaceholders.put(placeholder, nodeIndex);
                        conditionText.append(placeholder);
                        paragraphIndex++;
                        continue;
                    }

                    String paragraphText = extractTextFromParagraph(paraContent);
                    boolean isConditionMark = paragraphText != null &&
                            (paragraphText.contains("<#if") || paragraphText.contains("<#else") || paragraphText.contains("</#if"));

                    if (isConditionMark) {
                        for (int i = 0; i < paraContent.size(); i++) {
                            JSONObject paraItem = paraContent.getJSONObject(i);
                            if (paraItem == null) {
                                continue;
                            }

                            String itemType = paraItem.getString("type");
                            if ("text".equals(itemType)) {
                                String text = paraItem.getString("text");
                                if (text != null) {
                                    conditionText.append(text);
                                }
                            } else if ("inlineTemplate".equals(itemType)) {
                                JSONArray inlineContent = paraItem.getJSONArray("content");
                                if (inlineContent != null) {
                                    for (int j = 0; j < inlineContent.size(); j++) {
                                        JSONObject innerNode = inlineContent.getJSONObject(j);
                                        if (innerNode != null && "text".equals(innerNode.getString("type"))) {
                                            String text = innerNode.getString("text");
                                            if (text != null) {
                                                conditionText.append(text);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        String placeholder = "{{NODE_PARAGRAPH_" + paragraphIndex + "}}";
                        nodePlaceholders.put(placeholder, nodeIndex);
                        conditionText.append(placeholder);
                        paragraphIndex++;
                    }
                } else if ("table".equals(nodeType)) {
                    String placeholder = "{{NODE_TABLE_" + tableIndex + "}}";
                    nodePlaceholders.put(placeholder, nodeIndex);
                    conditionText.append(placeholder);
                    tableIndex++;
                } else if ("image".equals(nodeType)) {
                    String placeholder = "{{NODE_IMAGE_" + imageIndex + "}}";
                    nodePlaceholders.put(placeholder, nodeIndex);
                    conditionText.append(placeholder);
                    imageIndex++;
                } else {
                    String placeholder = "{{NODE_PARAGRAPH_" + paragraphIndex + "}}";
                    nodePlaceholders.put(placeholder, nodeIndex);
                    conditionText.append(placeholder);
                    paragraphIndex++;
                }
            }

            String result = conditionText.toString();
            if (StringUtils.isNotBlank(result)) {
                result = result.replace("\\\"", "\"");
                result = result.replace("\\n", "\n");
                result = result.trim();
            }

            LOG.info("提取的条件表达式: {}", result);
            return result;
        } catch (Exception e) {
            LOG.error("提取条件表达式时发生错误", e);
            return null;
        }
    }

    /**
     * 提取条件表达式文本（兼容旧版本，不包含占位符映射）
     */
    private String extractConditionText(JSONObject conditionNode) {
        Map<String, Integer> nodePlaceholders = new HashMap<>();
        return extractConditionText(conditionNode, nodePlaceholders, new JSONObject());
    }

    /**
     * 替换条件表达式中的变量
     * 支持格式：
     * - {{key#value#$pos#path}} - ETL、SQL等数据源
     * - {{开始输入#变量名}} - 全局变量
     * - {{key#value}} - 模块变量
     * 注意：占位符 {{NODE_TABLE_*}} 和 {{NODE_IMAGE_*}} 不会被替换
     */
    private String replaceConditionVariables(String conditionText, JSONObject parameters) {
        if (StringUtils.isBlank(conditionText)) {
            return conditionText;
        }
        
        String result = conditionText;
        LOG.info("替换前的条件表达式: {}", result);

        // 将 <#else if 标准化为 <#elseif，以便 conditionTagPattern 能匹配并替换其中的变量
        result = result.replaceAll("<#else\\s+if\\b", "<#elseif");

        Map<String, String> placeholderMap = new HashMap<>();
        Pattern placeholderPattern = Pattern.compile("\\{\\{NODE_(TABLE|IMAGE|PARAGRAPH)_\\d+\\}\\}");
        Matcher placeholderMatcher = placeholderPattern.matcher(result);
        StringBuffer protectedBuffer = new StringBuffer();
        int placeholderIndex = 0;
        while (placeholderMatcher.find()) {
            String placeholder = placeholderMatcher.group(0);
            String protectedPlaceholder = "{{__PROTECTED_PLACEHOLDER_" + placeholderIndex + "__}}";
            placeholderMap.put(protectedPlaceholder, placeholder);
            placeholderMatcher.appendReplacement(protectedBuffer, Matcher.quoteReplacement(protectedPlaceholder));
            placeholderIndex++;
        }
        placeholderMatcher.appendTail(protectedBuffer);
        result = protectedBuffer.toString();

        result = replaceExistenceChecks(result, parameters);

        result = replaceVariablesInConditionParts(result, parameters);

        for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        LOG.info("最终替换后的条件表达式: {}", result);
        return result;
    }

    private String replaceVariablesInConditionParts(String conditionText, JSONObject parameters) {
        String result = conditionText;

        Pattern conditionTagPattern = Pattern.compile("(<#(?:if|elseif)\\s+[^>]*>)");
        Matcher conditionTagMatcher = conditionTagPattern.matcher(result);
        StringBuffer buffer = new StringBuffer();

        while (conditionTagMatcher.find()) {
            String conditionTag = conditionTagMatcher.group(1);
            String replacedTag = replaceVariablesInTag(conditionTag, parameters);
            conditionTagMatcher.appendReplacement(buffer, Matcher.quoteReplacement(replacedTag));
        }
        conditionTagMatcher.appendTail(buffer);
        result = buffer.toString();

        return result;
    }

    private String replaceVariablesInTag(String tag, JSONObject parameters) {
        String result = tag;
        LOG.debug("处理条件标签: {}", tag);

        Matcher matcher = CONDITION_PATTERN.matcher(result);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            try {
                String key = matcher.group(1);
                String value = matcher.group(2);
                String posGroup = matcher.group(3);
                String jsonDataPath = matcher.group(4);
                String path = buildJsonPathFromColumnKey(jsonDataPath);

                Object resolvedValue = resolveParameterValue(key, value, posGroup, path, parameters);
                LOG.debug("解析变量 {}#{}: {}", key, value, resolvedValue);

                boolean surroundedByQuotes = isSurroundedByQuotes(result, matcher.start(), matcher.end());
                String replacement;
                if (surroundedByQuotes) {
                    if (resolvedValue == null) {
                        replacement = "";
                    } else {
                        replacement = escapeForFreemarker(String.valueOf(resolvedValue));
                    }
                } else {
                    replacement = formatFreemarkerLiteral(resolvedValue);
                }

                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                LOG.warn("替换条件变量时发生错误: {}", matcher.group(0), e);
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(buffer);
        result = buffer.toString();

        Matcher startInputMatcher = CONDITION_START_INPUT_PATTERN.matcher(result);
        buffer = new StringBuffer();
        while (startInputMatcher.find()) {
            try {
                String variableName = startInputMatcher.group(1);
                JSONObject variableObj = parameters.getJSONObject(variableName);
                Object rawValue = variableObj == null ? null : variableObj.get("value");
                LOG.debug("解析开始输入变量 {}: {}", variableName, rawValue);

                boolean surroundedByQuotes = isSurroundedByQuotes(result, startInputMatcher.start(), startInputMatcher.end());
                String replacement;
                if (surroundedByQuotes) {
                    if (rawValue == null) {
                        replacement = "";
                    } else {
                        replacement = escapeForFreemarker(String.valueOf(rawValue));
                    }
                } else {
                    replacement = formatFreemarkerLiteral(rawValue);
                }

                startInputMatcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                LOG.warn("替换开始输入变量时发生错误: {}", startInputMatcher.group(0), e);
                startInputMatcher.appendReplacement(buffer, Matcher.quoteReplacement(startInputMatcher.group(0)));
            }
        }
        startInputMatcher.appendTail(buffer);
        result = buffer.toString();

        Matcher moduleValueMatcher = MODULE_VALUE_PATTERN.matcher(result);
        buffer = new StringBuffer();
        while (moduleValueMatcher.find()) {
            try {
                String key = moduleValueMatcher.group(1);
                String value = moduleValueMatcher.group(2);
                String moduleKey = key + "#" + value;
                JSONObject paramObj = parameters.getJSONObject(moduleKey);
                Object rawValue = paramObj == null ? null : paramObj.get("value");
                LOG.debug("解析模块变量 {}: {}", moduleKey, rawValue);

                boolean surroundedByQuotes = isSurroundedByQuotes(result, moduleValueMatcher.start(), moduleValueMatcher.end());
                String replacement;

                if (paramObj != null) {
                    String nodeType = paramObj.getString("node_type");
                    if (("sub_workflow".equals(nodeType) || "sub_workflow_list".equals(nodeType)) && rawValue != null) {
                        try {
                            JSONObject valueObj = rawValue instanceof JSONObject ?
                                    (JSONObject) rawValue : JsonUtils.parseJsonObjectWithArm(String.valueOf(rawValue));
                            if (valueObj != null) {
                                Object content = JSONPath.eval(valueObj, "$.docOutput.data.content");
                                if (content == null) {
                                    content = JSONPath.eval(valueObj, "$.content.value.data.content");
                                }
                                if (content instanceof JSONArray) {
                                    StringBuilder textContent = new StringBuilder();
                                    extractTextFromTiptapContent((JSONArray) content, textContent);
                                    String textValue = textContent.toString().trim();
                                    if (surroundedByQuotes) {
                                        replacement = escapeForFreemarker(textValue);
                                    } else {
                                        replacement = formatFreemarkerLiteral(textValue);
                                    }
                                } else {
                                    if (surroundedByQuotes) {
                                        replacement = rawValue == null ? "" : escapeForFreemarker(String.valueOf(rawValue));
                                    } else {
                                        replacement = formatFreemarkerLiteral(rawValue);
                                    }
                                }
                            } else {
                                if (surroundedByQuotes) {
                                    replacement = rawValue == null ? "" : escapeForFreemarker(String.valueOf(rawValue));
                                } else {
                                    replacement = formatFreemarkerLiteral(rawValue);
                                }
                            }
                        } catch (Exception e) {
                            LOG.warn("处理子工作流变量时发生错误", e);
                            if (surroundedByQuotes) {
                                replacement = rawValue == null ? "" : escapeForFreemarker(String.valueOf(rawValue));
                            } else {
                                replacement = formatFreemarkerLiteral(rawValue);
                            }
                        }
                    } else {
                        if (surroundedByQuotes) {
                            if (rawValue == null) {
                                replacement = "";
                            } else {
                                replacement = escapeForFreemarker(String.valueOf(rawValue));
                            }
                        } else {
                            replacement = formatFreemarkerLiteral(rawValue);
                        }
                    }
                } else {
                    if (surroundedByQuotes) {
                        replacement = "";
                    } else {
                        replacement = "\"\"";
                    }
                }

                moduleValueMatcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            } catch (Exception e) {
                LOG.warn("条件模板中替换模块变量失败: {}", moduleValueMatcher.group(0), e);
                moduleValueMatcher.appendReplacement(buffer, Matcher.quoteReplacement(moduleValueMatcher.group(0)));
            }
        }
        moduleValueMatcher.appendTail(buffer);
        result = buffer.toString();

        LOG.debug("条件标签处理结果: {}", result);
        return result;
    }

    /**
     * 为 FreeMarker 转义字符串
     */
    private String escapeForFreemarker(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * 处理 {{xxx}}?? 的存在性判断
     */
    private String replaceExistenceChecks(String original, JSONObject parameters) {
        String result = original;

        Matcher matcher = CONDITION_EXIST_PATTERN.matcher(result);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            String posGroup = matcher.group(3);
            String jsonDataPath = matcher.group(4);
            String path = buildJsonPathFromColumnKey(jsonDataPath);

            Object resolvedValue = resolveParameterValue(key, value, posGroup, path, parameters);
            boolean exists = hasValue(resolvedValue);
            matcher.appendReplacement(buffer, String.valueOf(exists));
        }
        matcher.appendTail(buffer);
        result = buffer.toString();

        Matcher startMatcher = CONDITION_START_EXIST_PATTERN.matcher(result);
        buffer = new StringBuffer();
        while (startMatcher.find()) {
            String variableName = startMatcher.group(1);
            Object value = null;
            JSONObject variableObj = parameters.getJSONObject(variableName);
            if (variableObj != null) {
                value = variableObj.get("value");
            }
            boolean exists = hasValue(value);
            startMatcher.appendReplacement(buffer, String.valueOf(exists));
        }
        startMatcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * 解析参数值
     */
    private Object resolveParameterValue(String key, String value, String posGroup, String path, JSONObject parameters) {
        JSONObject dataContainer = parameters.getJSONObject(key + "#" + value);
        if (dataContainer == null) {
            dataContainer = parameters.getJSONObject(key);
        }
        if (dataContainer == null) {
            return null;
        }

        JSONObject objectValue = dataContainer.getJSONObject("value");
        if (objectValue != null && !objectValue.isEmpty()) {
            try {
                return JSONPath.eval(objectValue, path);
            } catch (Exception e) {
                LOG.info("从 JSONObject 获取变量失败，path={}", path, e);
            }
        }

        JSONArray arrayValue = dataContainer.getJSONArray("value");
        if (arrayValue == null || arrayValue.isEmpty()) {
            return null;
        }

        int index = parseIndex(posGroup);

        if (index >= 0 && index < arrayValue.size() && path != null) {
            String fieldName = extractFieldNameFromPath(path);
            if (fieldName != null) {
                Object rowObj = arrayValue.get(index);
                if (rowObj instanceof JSONObject) {
                    return ((JSONObject) rowObj).get(fieldName);
                } else if (rowObj instanceof Map) {
                    return ((Map) rowObj).get(fieldName);
                }
            }
        }

        try {
            String arrayPath = path.startsWith("$['") ? path.replaceFirst("^\\$", "$[" + index + "]") : path.replace("$.", "$[" + index + "].");
            Object eval = JSONPath.eval(arrayValue, arrayPath);
            if (eval instanceof JSONArray) {
                JSONArray evalArray = (JSONArray) eval;
                return (index >= 0 && index < evalArray.size()) ? evalArray.get(index) : null;
            } else if (eval instanceof List) {
                List<?> evalList = (List<?>) eval;
                return (index >= 0 && index < evalList.size()) ? evalList.get(index) : null;
            } else {
                return eval;
            }
        } catch (Exception e) {
            LOG.info("从 JSONArray 获取变量失败，尝试直接按索引取值，path={}", path, e);
        }

        if (index >= 0 && index < arrayValue.size()) {
            Object valueAtIndex = arrayValue.get(index);
            if (valueAtIndex instanceof JSONObject) {
                try {
                    return JSONPath.eval(valueAtIndex, path);
                } catch (Exception e) {
                    LOG.info("对 JSONArray 单元素执行 JSONPath 失败，path={}", path, e);
                }
            }
            return valueAtIndex;
        }

        return null;
    }

    private int parseIndex(String posGroup) {
        if (StringUtils.isBlank(posGroup)) {
            return 0;
        }
        String pos = posGroup.replace("$", "");
        try {
            return Integer.parseInt(pos) - 1;
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    /**
     * 将值转换为 FreeMarker 可识别的字面量
     */
    private String formatFreemarkerLiteral(Object value) {
        if (value == null) {
            return "\"\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Collection || value instanceof Map || value instanceof JSONArray || value instanceof JSONObject) {
            String json = JsonUtils.toJSONStringWithArm(value);
            return "\"" + escapeWithoutQuotes(json) + "\"";
        }

        String text = String.valueOf(value);
        return "\"" + escapeWithoutQuotes(text) + "\"";
    }

    /**
     * 判断占位符是否被一对双引号包裹
     */
    private boolean isSurroundedByQuotes(String text, int start, int end) {
        if (text == null || text.length() == 0) {
            return false;
        }
        if (start <= 0 || end >= text.length()) {
            return false;
        }
        char before = text.charAt(start - 1);
        char after = text.charAt(end);
        return before == '"' && after == '"';
    }

    /**
     * 仅对内容进行转义，不添加外层引号
     */
    private String escapeWithoutQuotes(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String) {
            return StringUtils.isNotBlank((String) value);
        }
        if (value instanceof Collection) {
            return !((Collection<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return !((Map<?, ?>) value).isEmpty();
        }
        if (value instanceof JSONArray) {
            return !((JSONArray) value).isEmpty();
        }
        if (value instanceof JSONObject) {
            return !((JSONObject) value).isEmpty();
        }
        return true;
    }

    /**
     * 执行 FreeMarker 条件表达式
     */
    private String executeConditionExpression(String conditionText) {
        try {
            if (StringUtils.isBlank(conditionText)) {
                return "";
            }

            LOG.info("FreeMarker 表达式内容: {}", conditionText);

            String normalized = conditionText
                    .replaceAll("<#else\\s+if", "<#elseif");
            if (!StringUtils.equals(conditionText, normalized)) {
                conditionText = normalized;
            }

            Configuration cfg = new Configuration(new Version(2, 3, 31));
            cfg.setDefaultEncoding("UTF-8");
            cfg.setClassicCompatible(true);
            cfg.setWhitespaceStripping(true);

            Map<String, Object> dataModel = new HashMap<>();

            Template freemarkerTemplate = new Template("templateName", conditionText, cfg);
            StringWriter out = new StringWriter();
            freemarkerTemplate.process(dataModel, out);

            String result = out.toString();
            return result;
        } catch (TemplateException | IOException e) {
            return "";
        }
    }

    /**
     * 将条件表达式执行结果转换为 Tiptap 节点格式
     * 结果可能是 JSON 对象字符串（需要解析）或纯文本
     */
    private JSONArray convertConditionResultToNodes(String renderResult, JSONObject originalNode) {
        JSONArray result = new JSONArray();

        if (StringUtils.isBlank(renderResult)) {
            return result;
        }

        String trimmed = renderResult.trim();


        if (trimmed.contains("{") && trimmed.contains("}")) {
            try {
                String cleaned = trimmed.replaceAll("}\\s*\\n+\\s*", "}");
                if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
                    if (cleaned.contains("}{")) {
                        String[] parts = cleaned.split("}\\s*\\{");
                        for (int i = 0; i < parts.length; i++) {
                            String part = parts[i];
                            if (!part.startsWith("{")) {
                                part = "{" + part;
                            }
                            if (!part.endsWith("}")) {
                                part = part + "}";
                            }
                            try {
                                JSONObject jsonObject = JsonUtils.parseJsonObjectWithArm(part);
                                result.add(jsonObject);
                            } catch (Exception e) {
                                result.add(createTextNode(part));
                            }
                        }
                    } else {
                        try {
                            JSONObject jsonObject = JsonUtils.parseJsonObjectWithArm(cleaned);
                            result.add(jsonObject);
                        } catch (Exception e) {
                            result.add(createTextNode(trimmed));
                        }
                    }
                } else {
                    result.add(createTextNode(trimmed));
                }
            } catch (Exception e) {
                result.add(createTextNode(trimmed));
            }
        } else {
            result.add(createTextNode(trimmed));
        }

        return result;
    }

    private JSONObject createTextNode(String text) {
        JSONObject paragraph = new JSONObject();
        paragraph.put("type", "paragraph");
        paragraph.put("attrs", new JSONObject());

        JSONObject textNode = new JSONObject();
        textNode.put("type", "text");
        textNode.put("text", text);
        textNode.put("marks", new JSONArray());

        JSONArray content = new JSONArray();
        content.add(textNode);
        paragraph.put("content", content);

        return paragraph;
    }


}


