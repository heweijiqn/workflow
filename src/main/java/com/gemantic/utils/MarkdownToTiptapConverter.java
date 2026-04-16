package com.gemantic.utils;

import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.ListBlock;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 将 Markdown 转换为 Tiptap JSON 结构的工具类。
 */
public final class MarkdownToTiptapConverter {

    private static final Parser PARSER;

    static {
        MutableDataSet parserOptions = new MutableDataSet();
        parserOptions.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));
        parserOptions.set(Parser.HTML_BLOCK_PARSER, true);
        parserOptions.set(Parser.LISTS_END_ON_DOUBLE_BLANK, true);
        PARSER = Parser.builder(parserOptions).build();
    }

    private MarkdownToTiptapConverter() {
        // Utility class
    }

    /**
     * 预处理 Markdown 文本，规范化表格格式以确保正确解析。
     *
     * @param markdown 原始 Markdown 文本
     * @return 预处理后的 Markdown 文本
     */
    private static String preprocessMarkdown(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return markdown;
        }

        String[] lines = markdown.split("\\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            boolean isTableLine = trimmed.contains("|") && !trimmed.isEmpty();
            boolean isPrevTableLine = i > 0 && lines[i - 1].trim().contains("|") && !lines[i - 1].trim().isEmpty();
            boolean isNextTableLine = i < lines.length - 1 && lines[i + 1].trim().contains("|") && !lines[i + 1].trim().isEmpty();
            boolean isPrevBlank = i > 0 && lines[i - 1].trim().isEmpty();
            boolean isNextBlank = i < lines.length - 1 && lines[i + 1].trim().isEmpty();

            if (isTableLine && !isPrevTableLine && !isPrevBlank && i > 0) {
                result.append("\n");
            }

            result.append(line);

            if (isTableLine && !isNextTableLine && !isNextBlank && i < lines.length - 1) {
                result.append("\n");
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * 使用默认选项将 Markdown 转换为 Tiptap。
     *
     * @param markdown Markdown 文本
     * @return Tiptap 文档 Map
     */
    public static Map<String, Object> convertMarkdownToTiptap(String markdown) {
        return convertMarkdownToTiptap(markdown, ConvertOptions.builder().build());
    }

    /**
     * 根据传入的选项将 Markdown 转换为 Tiptap。
     *
     * @param markdown Markdown 文本
     * @param options  转换选项
     * @return Tiptap 文档 Map
     */
    public static Map<String, Object> convertMarkdownToTiptap(String markdown, ConvertOptions options) {
        ConvertOptions resolvedOptions = options == null ? ConvertOptions.builder().build() : options;
        if (isBlank(markdown)) {
            return createDocumentWithSingleParagraph(resolvedOptions);
        }

        String preprocessed = preprocessMarkdown(markdown);

        Document document = PARSER.parse(preprocessed);
        List<Map<String, Object>> content = new ArrayList<>();
        Node child = document.getFirstChild();
        while (child != null) {
            content.addAll(convertBlockNode(child, resolvedOptions));
            child = child.getNext();
        }

        if (content.isEmpty()) {
            content.add(createEmptyParagraph(resolvedOptions));
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("type", "doc");
        doc.put("content", content);
        return doc;
    }

    /**
     * 将 Markdown 转换为 Tiptap 并附带页面元数据，生成完整的文档级数据。
     *
     * @param markdown Markdown 文本
     * @param options  文档级配置
     * @return 包含全部 Tiptap 数据的 Map
     */
    public static Map<String, Object> convertMarkdownDocumentToTiptap(String markdown, DocumentOptions options) {
        DocumentOptions resolved = options == null ? DocumentOptions.builder().build() : options;
        Map<String, Object> document = convertMarkdownToTiptap(markdown, resolved.getConvertOptions());

        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("data", document);
        wrapper.put("pageMarginState", resolved.getPageMarginState().toMap());
        wrapper.put("showLock", resolved.isShowLock());
        wrapper.put("dpi", resolved.getDpi());
        wrapper.put("paperSizeState", resolved.getPaperSizeState());
        return wrapper;
    }
    /**
     * 构建一个仅包含空段落的基础 Tiptap 文档。
     *
     * @param options 转换选项
     * @return 文档 Map
     */
    private static Map<String, Object> createDocumentWithSingleParagraph(ConvertOptions options) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("type", "doc");
        doc.put("content", Collections.singletonList(createEmptyParagraph(options)));
        return doc;
    }

    /**
     * 将 AST 块节点映射为对应的 Tiptap 节点。
     *
     * @param node    AST 节点
     * @param options 转换选项
     * @return 转换后节点列表
     */
    private static List<Map<String, Object>> convertBlockNode(Node node, ConvertOptions options) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        if (node instanceof Heading) {
            nodes.add(createHeadingNode((Heading) node, options));
        } else if (node instanceof Paragraph) {
            nodes.addAll(processParagraphNode((Paragraph) node, options));
        } else if (node instanceof FencedCodeBlock) {
            FencedCodeBlock codeBlock = (FencedCodeBlock) node;
            nodes.add(processCodeBlock(codeBlock.getContentChars().toString(), codeBlock.getInfo().toString(), options));
        } else if (node instanceof IndentedCodeBlock) {
            nodes.add(processCodeBlock(((IndentedCodeBlock) node).getContentChars().toString(), null, options));
        } else if (node instanceof BulletList || node instanceof OrderedList) {
            nodes.add(processList((ListBlock) node, options));
        } else if (node instanceof BlockQuote) {
            nodes.add(processBlockquote((BlockQuote) node, options));
        } else if (node instanceof ThematicBreak) {
            nodes.add(processHorizontalRule(options));
        } else if (node instanceof TableBlock) {
            nodes.add(processTable((TableBlock) node, options));
        } else if (node instanceof HtmlBlock) {
            nodes.add(processHtmlBlock((HtmlBlock) node, options));
        } else if (node instanceof Image) {
            nodes.add(processImage((Image) node, options));
        } else {
            Map<String, Object> fallbackText = createTextNode(node.getChars().toString(), new ArrayList<>(), options, true);
            if (fallbackText != null) {
                nodes.add(createParagraph(Collections.singletonList(fallbackText), new LinkedHashMap<>(), options));
            } else {
                nodes.add(createEmptyParagraph(options));
            }
        }
        return nodes;
    }

    /**
     * 构建标题节点，并避免继承的字号影响标题。
     *
     * @param heading 标题 AST 节点
     * @param options 转换选项
     * @return 标题节点 Map
     */
    private static Map<String, Object> createHeadingNode(Heading heading, ConvertOptions options) {
        ConvertOptions safeOptions = options == null ? ConvertOptions.builder().build() : options;
        List<Map<String, Object>> content = processInlineChildren(heading, safeOptions.withoutSize(), Collections.emptyList());
        return createHeading(heading.getLevel(), content, safeOptions);
    }

    /**
     * 处理段落节点，并将内联图片拆成单独的块节点。
     *
     * @param paragraph 段落 AST 节点
     * @param options   转换选项
     * @return 描述段落内容的节点集合
     */
    private static List<Map<String, Object>> processParagraphNode(Paragraph paragraph, ConvertOptions options) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> inlineBuffer = new ArrayList<>();
        Node child = paragraph.getFirstChild();
        while (child != null) {
            if (child instanceof Image) {
                if (!inlineBuffer.isEmpty()) {
                    nodes.add(createParagraph(new ArrayList<>(inlineBuffer), new LinkedHashMap<>(), options));
                    inlineBuffer.clear();
                }
                nodes.add(processImage((Image) child, options));
            } else {
                List<Map<String, Object>> inlineNodes = processInlineNode(child, options, Collections.emptyList());
                for (Map<String, Object> inlineNode : inlineNodes) {
                    if (inlineNode == null) {
                        continue;
                    }
                    if ("image".equals(inlineNode.get("type"))) {
                        if (!inlineBuffer.isEmpty()) {
                            nodes.add(createParagraph(new ArrayList<>(inlineBuffer), new LinkedHashMap<>(), options));
                            inlineBuffer.clear();
                        }
                        nodes.add(inlineNode);
                    } else {
                        inlineBuffer.add(inlineNode);
                    }
                }
            }
            child = child.getNext();
        }

        if (!inlineBuffer.isEmpty()) {
            nodes.add(createParagraph(new ArrayList<>(inlineBuffer), new LinkedHashMap<>(), options));
            inlineBuffer.clear();
        }

        if (nodes.isEmpty()) {
            nodes.add(createEmptyParagraph(options));
        }

        return nodes;
    }

    /**
     * 结合上下文 mark 处理节点的所有内联子节点。
     *
     * @param parent      父节点
     * @param options     转换选项
     * @param activeMarks 继承自父节点的 mark
     * @return 内联节点列表
     */
    private static List<Map<String, Object>> processInlineChildren(Node parent, ConvertOptions options, List<Map<String, Object>> activeMarks) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        Node child = parent.getFirstChild();
        while (child != null) {
            nodes.addAll(processInlineNode(child, options, activeMarks));
            child = child.getNext();
        }
        return nodes;
    }

    /**
     * 转换单个内联节点。
     *
     * @param node        内联节点
     * @param options     转换选项
     * @param activeMarks 已应用的 mark
     * @return 内联节点列表
     */
    private static List<Map<String, Object>> processInlineNode(Node node, ConvertOptions options, List<Map<String, Object>> activeMarks) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        if (node instanceof Text) {
            Map<String, Object> textNode = createTextNode(((Text) node).getChars().toString(), activeMarks, options, true);
            if (textNode != null) {
                nodes.add(textNode);
            }
        } else if (node instanceof SoftLineBreak || node instanceof HardLineBreak) {
            nodes.add(createHardBreak());
        } else if (node instanceof StrongEmphasis) {
            nodes.addAll(processInlineChildren(node, options, appendMark(activeMarks, createSimpleMark("bold"))));
        } else if (node instanceof Emphasis) {
            nodes.addAll(processInlineChildren(node, options, appendMark(activeMarks, createSimpleMark("italic"))));
        } else if (node instanceof Strikethrough) {
            nodes.addAll(processInlineChildren(node, options, appendMark(activeMarks, createSimpleMark("strike"))));
        } else if (node instanceof Code) {
            Map<String, Object> codeNode = createCodeTextNode((Code) node, options);
            if (codeNode != null) {
                nodes.add(codeNode);
            }
        } else if (node instanceof Link) {
            nodes.addAll(processInlineChildren(node, options, appendMark(activeMarks, createLinkMark((Link) node))));
        } else if (node instanceof Image) {
            nodes.add(processImage((Image) node, options));
        } else {
            Map<String, Object> fallbackText = createTextNode(node.getChars().toString(), activeMarks, options, true);
            if (fallbackText != null) {
                nodes.add(fallbackText);
            }
        }
        return nodes;
    }

    /**
     * 构建 `code` 节点的内联表示。
     *
     * @param code    行内代码节点
     * @param options 转换选项
     * @return 文本节点 Map
     */
    private static Map<String, Object> createCodeTextNode(Code code, ConvertOptions options) {
        Map<String, Object> codeAttrs = new LinkedHashMap<>();
        codeAttrs.put("fontFamily", isBlank(options.getFont()) ? "monospace" : options.getFont());
        codeAttrs.put("backgroundColor", isBlank(options.getBackgroundColor()) ? "#f5f5f5" : options.getBackgroundColor());
        if (!isBlank(options.getSourceId())) {
            codeAttrs.put("sourceId", options.getSourceId());
        }
        if (!isBlank(options.getPosition())) {
            codeAttrs.put("position", options.getPosition());
        }
        if (!isBlank(options.getRawFontSize())) {
            codeAttrs.put("fontSize", options.getRawFontSize());
        } else if (options.getSize() != null) {
            codeAttrs.put("fontSize", toPointSize(options.getSize()));
        }
        if (!isBlank(options.getColor())) {
            codeAttrs.put("color", options.getColor());
        }
        Map<String, Object> textStyle = new LinkedHashMap<>();
        textStyle.put("type", "textStyle");
        textStyle.put("attrs", codeAttrs);
        List<Map<String, Object>> marks = new ArrayList<>();
        marks.add(textStyle);
        Map<String, Object> node = createTextNode(code.getChars().toString(), marks, null, false);
        if (node == null) {
            node = createTextNode(" ", marks, null, false);
        }
        return node;
    }

    /**
     * 创建不带属性的基础 mark。
     *
     * @param type mark 类型
     * @return mark Map
     */
    private static Map<String, Object> createSimpleMark(String type) {
        Map<String, Object> mark = new LinkedHashMap<>();
        mark.put("type", type);
        return mark;
    }

    /**
     * 创建包含 href 与 title 的链接 mark。
     *
     * @param link Markdown 链接节点
     * @return mark Map
     */
    private static Map<String, Object> createLinkMark(Link link) {
        Map<String, Object> mark = new LinkedHashMap<>();
        mark.put("type", "link");
        Map<String, Object> attrs = new LinkedHashMap<>();
        String href = isBlank(link.getUrl().toString()) ? "#" : link.getUrl().toString();
        attrs.put("href", href);
        attrs.put("target", "_blank");
        String title = link.getTitle().isEmpty() ? null : link.getTitle().toString();
        attrs.put("title", title);
        mark.put("attrs", attrs);
        return mark;
    }

    /**
     * 返回包含现有 mark 及新增 mark 的列表。
     *
     * @param activeMarks 当前生效的 mark 列表
     * @param mark        需要附加的 mark
     * @return 合并后的 mark 列表
     */
    private static List<Map<String, Object>> appendMark(List<Map<String, Object>> activeMarks, Map<String, Object> mark) {
        List<Map<String, Object>> marks = cloneMarks(activeMarks);
        if (mark != null) {
            marks.add(deepCopyMark(mark));
        }
        return marks;
    }

    /**
     * 深拷贝 mark 列表以避免递归时被修改。
     *
     * @param marks mark 列表
     * @return 拷贝后的列表
     */
    private static List<Map<String, Object>> cloneMarks(List<Map<String, Object>> marks) {
        if (marks == null || marks.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> cloned = new ArrayList<>(marks.size());
        for (Map<String, Object> mark : marks) {
            cloned.add(deepCopyMark(mark));
        }
        return cloned;
    }

    /**
     * 创建 mark 定义的防御性拷贝。
     *
     * @param mark 已有的 mark
     * @return 拷贝后的 mark
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepCopyMark(Map<String, Object> mark) {
        if (mark == null) {
            return null;
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        copy.put("type", mark.get("type"));
        Object attrs = mark.get("attrs");
        if (attrs instanceof Map) {
            copy.put("attrs", new LinkedHashMap<>((Map<String, Object>) attrs));
        } else if (attrs != null) {
            copy.put("attrs", attrs);
        }
        return copy;
    }

    /**
     * 创建硬换行节点。
     *
     * @return 换行节点 Map
     */
    private static Map<String, Object> createHardBreak() {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "hardBreak");
        return node;
    }
    /**
     * 转换围栏或缩进代码块。
     *
     * @param code     代码块内容
     * @param language 可选的语言标识
     * @param options  转换选项
     * @return 代码块节点 Map
     */
    private static Map<String, Object> processCodeBlock(String code, String language, ConvertOptions options) {
        List<Map<String, Object>> content = new ArrayList<>();
        if (!isBlank(code)) {
            String[] lines = code.split("\\r?\\n", -1);
            for (String line : lines) {
                Map<String, Object> text = createTextNode(line, new ArrayList<>(), null, false);
                if (text != null) {
                    content.add(text);
                }
            }
        }
        if (content.isEmpty()) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("type", "text");
            fallback.put("text", " ");
            content.add(fallback);
        }

        ConvertOptions safeOptions = options == null ? ConvertOptions.builder().build() : options;
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", generateId());
        attrs.put("sourceId", safeOptions.getSourceId());
        attrs.put("position", safeOptions.getPosition());
        attrs.put("diffStatus", null);
        attrs.put("language", isBlank(language) ? null : language);

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "codeBlock");
        node.put("attrs", attrs);
        node.put("content", content);
        return node;
    }

    /**
     * 转换有序与无序列表块。
     *
     * @param listBlock 列表块节点
     * @param options   转换选项
     * @return 列表节点 Map
     */
    private static Map<String, Object> processList(ListBlock listBlock, ConvertOptions options) {
        List<Map<String, Object>> items = new ArrayList<>();
        Node child = listBlock.getFirstChild();
        while (child != null) {
            if (child instanceof ListItem) {
                List<Map<String, Object>> itemContent = new ArrayList<>();
                Node itemChild = child.getFirstChild();
                while (itemChild != null) {
                    itemContent.addAll(convertBlockNode(itemChild, options));
                    itemChild = itemChild.getNext();
                }
                if (itemContent.isEmpty()) {
                    itemContent.add(createEmptyParagraph(options));
                } else {
                    // 如果listItem的content第一个元素不是paragraph类型，需要在前面插入一个空的paragraph
                    Map<String, Object> firstElement = itemContent.get(0);
                    String firstType = firstElement != null ? (String) firstElement.get("type") : null;
                    if (!"paragraph".equals(firstType)) {
                        itemContent.add(0, createEmptyParagraph(options));
                    }
                }
                Map<String, Object> listItem = new LinkedHashMap<>();
                listItem.put("type", "listItem");
                Map<String, Object> attrs = new LinkedHashMap<>();
                attrs.put("id", generateId());
                listItem.put("attrs", attrs);
                listItem.put("content", itemContent);
                items.add(listItem);
            }
            child = child.getNext();
        }

        ConvertOptions safeOptions = options == null ? ConvertOptions.builder().build() : options;
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", generateId());
        attrs.put("sourceId", safeOptions.getSourceId());
        attrs.put("position", safeOptions.getPosition());
        if (listBlock instanceof OrderedList) {
            attrs.put("start", ((OrderedList) listBlock).getStartNumber());
        } else {
            attrs.put("start", null);
        }

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", listBlock instanceof OrderedList ? "orderedList" : "bulletList");
        node.put("attrs", attrs);
        node.put("content", items);
        return node;
    }

    /**
     * 将引用节点转换为 Tiptap 结构。
     *
     * @param blockQuote 引用节点
     * @param options    转换选项
     * @return 引用节点 Map
     */
    private static Map<String, Object> processBlockquote(BlockQuote blockQuote, ConvertOptions options) {
        List<Map<String, Object>> content = new ArrayList<>();
        Node child = blockQuote.getFirstChild();
        while (child != null) {
            content.addAll(convertBlockNode(child, options));
            child = child.getNext();
        }
        if (content.isEmpty()) {
            content.add(createEmptyParagraph(options));
        }

        ConvertOptions safeOptions = options == null ? ConvertOptions.builder().build() : options;
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", generateId());
        attrs.put("sourceId", safeOptions.getSourceId());
        attrs.put("position", safeOptions.getPosition());
        attrs.put("diffStatus", null);

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "blockquote");
        node.put("attrs", attrs);
        node.put("content", content);
        return node;
    }

    /**
     * 创建用于表示分割线的段落节点。
     *
     * @param options 转换选项
     * @return 段落节点
     */
    private static Map<String, Object> processHorizontalRule(ConvertOptions options) {
        ConvertOptions safeOptions = options == null ? ConvertOptions.builder().build() : options;
        Map<String, Object> textStyle = new LinkedHashMap<>();
        textStyle.put("type", "textStyle");
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("textAlign", "center");
        if (!isBlank(safeOptions.getSourceId())) {
            attrs.put("sourceId", safeOptions.getSourceId());
        }
        if (!isBlank(safeOptions.getPosition())) {
            attrs.put("position", safeOptions.getPosition());
        }
        textStyle.put("attrs", attrs);
        List<Map<String, Object>> marks = new ArrayList<>();
        marks.add(textStyle);
        Map<String, Object> textNode = createTextNode("---", marks, null, false);
        return createParagraph(Collections.singletonList(textNode), new LinkedHashMap<>(), safeOptions);
    }

    /**
     * 将 Markdown 图片转换为 Tiptap 节点。
     *
     * @param image   图片节点
     * @param options 转换选项
     * @return 图片节点 Map
     */
    private static Map<String, Object> processImage(Image image, ConvertOptions options) {
        ConvertOptions safeOptions = options == null ? ConvertOptions.builder().build() : options;
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", generateId());
        attrs.put("sourceId", safeOptions.getSourceId());
        attrs.put("position", safeOptions.getPosition());
        attrs.put("annotationId", null);
        attrs.put("annotationGroup", null);
        attrs.put("src", image.getUrl().toString());
        attrs.put("alt", image.getText().toString());
        attrs.put("title", image.getTitle().isEmpty() ? null : image.getTitle().toString());
        attrs.put("width", null);
        attrs.put("height", null);
        attrs.put("data-placeholder-id", null);

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "image");
        node.put("attrs", attrs);
        return node;
    }

    /**
     * 将 Markdown 表格转换为 Tiptap 结构。
     *
     * @param tableBlock 表格节点
     * @param options    转换选项
     * @return 表格节点 Map
     */
    private static Map<String, Object> processTable(TableBlock tableBlock, ConvertOptions options) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Node child = tableBlock.getFirstChild();
        while (child != null) {
            rows.addAll(processTableSection(child, options));
            child = child.getNext();
        }

        ConvertOptions safeOptions = options == null ? ConvertOptions.builder().build() : options;
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", generateId());
        attrs.put("annotationId", null);
        attrs.put("annotationGroup", null);
        attrs.put("sourceId", safeOptions.getSourceId());
        attrs.put("position", safeOptions.getPosition());
        attrs.put("diffStatus", null);
        attrs.put("width", null);
        attrs.put("columnWidths", null);

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "table");
        node.put("attrs", attrs);
        node.put("content", rows);
        return node;
    }

    /**
     * 递归处理表头与表体等表格区域。
     *
     * @param node    表格区域节点
     * @param options 转换选项
     * @return 行节点列表
     */
    private static List<Map<String, Object>> processTableSection(Node node, ConvertOptions options) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (node instanceof TableRow) {
            rows.add(processTableRow((TableRow) node, options));
        } else if (node instanceof TableHead || node instanceof TableBody) {
            Node child = node.getFirstChild();
            while (child != null) {
                rows.addAll(processTableSection(child, options));
                child = child.getNext();
            }
        }
        return rows;
    }

    /**
     * 转换表格行及其所有单元格。
     *
     * @param row     表格行节点
     * @param options 转换选项
     * @return 表格行节点 Map
     */
    private static Map<String, Object> processTableRow(TableRow row, ConvertOptions options) {
        List<Map<String, Object>> cells = new ArrayList<>();
        Node child = row.getFirstChild();
        while (child != null) {
            if (child instanceof TableCell) {
                cells.add(processTableCell((TableCell) child, options));
            }
            child = child.getNext();
        }

        ConvertOptions safeOptions = options == null ? ConvertOptions.builder().build() : options;
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", generateId());
        attrs.put("annotationId", null);
        attrs.put("annotationGroup", null);
        attrs.put("sourceId", safeOptions.getSourceId());
        attrs.put("position", safeOptions.getPosition());
        attrs.put("diffStatus", null);
        attrs.put("loop", null);
        attrs.put("startLoopIndex", null);
        attrs.put("endLoopIndex", null);

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "tableRow");
        node.put("attrs", attrs);
        node.put("content", cells);
        return node;
    }

    /**
     * 转换表格单元，并用段落包裹其内联内容。
     *
     * @param cell    表格单元节点
     * @param options 转换选项
     * @return 表格单元节点 Map
     */
    private static Map<String, Object> processTableCell(TableCell cell, ConvertOptions options) {
        List<Map<String, Object>> content = new ArrayList<>();
        List<Map<String, Object>> inlineNodes = processInlineChildren(cell, options, Collections.emptyList());
        
        // 获取单元格对齐方式并转换为 Tiptap 格式
        Map<String, Object> paragraphAttrs = new LinkedHashMap<>();
        String textAlign = convertAlignment(cell.getAlignment());
        if (textAlign != null) {
            paragraphAttrs.put("textAlign", textAlign);
        }
        
        if (inlineNodes.isEmpty()) {
            content.add(createParagraph(Collections.emptyList(), paragraphAttrs, options));
        } else {
            content.add(createParagraph(new ArrayList<>(inlineNodes), paragraphAttrs, options));
        }

        ConvertOptions safeOptions = options == null ? ConvertOptions.builder().build() : options;
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", generateId());
        attrs.put("annotationId", null);
        attrs.put("annotationGroup", null);
        attrs.put("sourceId", safeOptions.getSourceId());
        attrs.put("position", safeOptions.getPosition());
        attrs.put("diffStatus", null);
        attrs.put("colspan", cell.getSpan());
        attrs.put("rowspan", 1);
        attrs.put("colwidth", null);
        attrs.put("markInsert", null);
        attrs.put("rowMerged", null);
        attrs.put("verticalAlign", null);
        attrs.put("inLoopRange", null);

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "tableCell");
        node.put("attrs", attrs);
        node.put("content", content);
        return node;
    }

    /**
     * 将 HTML 区块的原始文本包裹在段落内输出。
     *
     * @param block   HTML 区块节点
     * @param options 转换选项
     * @return 段落节点
     */
    private static Map<String, Object> processHtmlBlock(HtmlBlock block, ConvertOptions options) {
        Map<String, Object> textNode = createTextNode(block.getChars().toString(), new ArrayList<>(), options, true);
        if (textNode == null) {
            return createEmptyParagraph(options);
        }
        return createParagraph(Collections.singletonList(textNode), new LinkedHashMap<>(), options);
    }
    /**
     * 根据文本与 mark 构建文本节点。
     *
     * @param text             文本内容
     * @param marks            需要应用的 mark 集合
     * @param options          转换选项
     * @param applyOptionMarks 是否附加来源于配置的 mark 属性
     * @return 文本节点，若文本为空返回 null
     */
    private static Map<String, Object> createTextNode(String text, List<Map<String, Object>> marks, ConvertOptions options, boolean applyOptionMarks) {
        if (text == null) {
            return null;
        }
        if (text.trim().isEmpty()) {
            return null;
        }

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "text");
        node.put("text", text);

        List<Map<String, Object>> resolvedMarks = cloneMarks(marks);
        if (applyOptionMarks && options != null) {
            Map<String, Object> textStyleAttrs = buildTextStyleAttrs(options);
            if (!textStyleAttrs.isEmpty()) {
                mergeTextStyleMark(resolvedMarks, textStyleAttrs);
            }
        }

        if (!resolvedMarks.isEmpty()) {
            node.put("marks", resolvedMarks);
        }
        return node;
    }

    /**
     * 确保 textStyle 属性被合并而不是重复添加。
     *
     * @param marks          现有 mark 列表
     * @param textStyleAttrs 需要合并的文本样式属性
     */
    private static void mergeTextStyleMark(List<Map<String, Object>> marks, Map<String, Object> textStyleAttrs) {
        for (Map<String, Object> mark : marks) {
            if ("textStyle".equals(mark.get("type"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> attrs = (Map<String, Object>) mark.get("attrs");
                if (attrs == null) {
                    attrs = new LinkedHashMap<>();
                    mark.put("attrs", attrs);
                }
                attrs.putAll(textStyleAttrs);
                return;
            }
        }
        Map<String, Object> mark = new LinkedHashMap<>();
        mark.put("type", "textStyle");
        mark.put("attrs", new LinkedHashMap<>(textStyleAttrs));
        marks.add(mark);
    }

    /**
     * 根据传入选项组装 textStyle mark 的属性。
     *
     * @param options 转换选项
     * @return 属性 Map
     */
    private static Map<String, Object> buildTextStyleAttrs(ConvertOptions options) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        if (!isBlank(options.getSourceId())) {
            attrs.put("sourceId", options.getSourceId());
        }
        if (!isBlank(options.getPosition())) {
            attrs.put("position", options.getPosition());
        }
        if (!isBlank(options.getFont())) {
            attrs.put("fontFamily", options.getFont());
        }
        if (!isBlank(options.getRawFontSize())) {
            // 优先使用原始字号字符串（不经过px到pt转换）
            attrs.put("fontSize", options.getRawFontSize());
        } else if (options.getSize() != null) {
            attrs.put("fontSize", toPointSize(options.getSize()));
        }
        if (!isBlank(options.getColor())) {
            attrs.put("color", options.getColor());
        }
        if (!isBlank(options.getBackgroundColor())) {
            attrs.put("backgroundColor", options.getBackgroundColor());
        }
        return attrs;
    }

    /**
     * 创建带生成 ID 的段落节点。
     *
     * @param content 段落内容
     * @param attrs   可选属性覆盖
     * @param options 转换选项
     * @return 段落节点 Map
     */
    private static Map<String, Object> createParagraph(List<Map<String, Object>> content, Map<String, Object> attrs, ConvertOptions options) {
        ConvertOptions safeOptions = options == null ? ConvertOptions.builder().build() : options;
        Map<String, Object> paragraph = new LinkedHashMap<>();
        paragraph.put("type", "paragraph");
        Map<String, Object> paragraphAttrs = new LinkedHashMap<>();
        paragraphAttrs.put("id", generateId());
        paragraphAttrs.put("sourceId", safeOptions.getSourceId());
        paragraphAttrs.put("position", safeOptions.getPosition());
        paragraphAttrs.put("diffStatus", null);
        paragraphAttrs.put("textAlign", attrs != null ? attrs.getOrDefault("textAlign", null) : null);
        paragraph.put("attrs", paragraphAttrs);

        List<Map<String, Object>> filteredContent = new ArrayList<>();
        if (content != null) {
            for (Map<String, Object> item : content) {
                if (item != null) {
                    filteredContent.add(item);
                }
            }
        }
        if (!filteredContent.isEmpty()) {
            paragraph.put("content", filteredContent);
        }
        return paragraph;
    }

    /**
     * 构建空段落占位节点。
     *
     * @param options 转换选项
     * @return 段落节点 Map
     */
    private static Map<String, Object> createEmptyParagraph(ConvertOptions options) {
        return createParagraph(Collections.emptyList(), new LinkedHashMap<>(), options);
    }

    /**
     * 创建带完整属性的标题节点。
     *
     * @param level   标题级别
     * @param content 内联节点列表
     * @param options 转换选项
     * @return 标题节点 Map
     */
    private static Map<String, Object> createHeading(int level, List<Map<String, Object>> content, ConvertOptions options) {
        ConvertOptions safeOptions = options == null ? ConvertOptions.builder().build() : options;
        Map<String, Object> heading = new LinkedHashMap<>();
        heading.put("type", "heading");
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", generateId());
        attrs.put("sourceId", safeOptions.getSourceId());
        attrs.put("position", safeOptions.getPosition());
        attrs.put("diffStatus", null);
        attrs.put("level", Math.max(1, Math.min(level, 6)));
        attrs.put("textAlign", null);
        heading.put("attrs", attrs);

        List<Map<String, Object>> filteredContent = new ArrayList<>();
        if (content != null) {
            for (Map<String, Object> node : content) {
                if (node != null) {
                    filteredContent.add(node);
                }
            }
        }
        if (!filteredContent.isEmpty()) {
            heading.put("content", filteredContent);
        }
        return heading;
    }

    /**
     * 生成随机 ID，保证每个节点可被客户端唯一引用。
     *
     * @return UUID 字符串
     */
    private static String generateId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 把像素值换算为编辑器 schema 需要的 pt 单位。
     *
     * @param px 像素值
     * @return 字体单位 pt 的字符串
     */
    private static String toPointSize(int px) {
        BigDecimal value = BigDecimal.valueOf(px).multiply(BigDecimal.valueOf(3)).divide(BigDecimal.valueOf(4));
        return value.stripTrailingZeros().toPlainString() + "pt";
    }

    /**
     * 判断给定字符串是否为空。
     *
     * @param value 字符串值
     * @return 为空时返回 true
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 将 flexmark 的 TableCell.Alignment 转换为 Tiptap 的 textAlign 值。
     *
     * @param alignment flexmark 的对齐方式枚举
     * @return Tiptap 的对齐方式字符串（left、center、right），如果是默认对齐则返回 null
     */
    private static String convertAlignment(TableCell.Alignment alignment) {
        if (alignment == null) {
            return null;
        }
        switch (alignment) {
            case LEFT:
                return "left";
            case CENTER:
                return "center";
            case RIGHT:
                return "right";
            default:
                return null;
        }
    }

    /**
     * 控制内联 mark 生成方式的配置。
     */
    public static final class ConvertOptions {
        private final String sourceId;
        private final String position;
        private final String font;
        private final Integer size;
        private final String color;
        private final String backgroundColor;
        private final String rawFontSize;

        private ConvertOptions(Builder builder) {
            this.sourceId = builder.sourceId;
            this.position = builder.position;
            this.font = builder.font;
            this.size = builder.size;
            this.color = builder.color;
            this.backgroundColor = builder.backgroundColor;
            this.rawFontSize = builder.rawFontSize;
        }

        /**
         * 创建新的构造器实例。
         *
         * @return 构造器
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * @return 已配置的溯源标识
         */
        public String getSourceId() {
            return sourceId;
        }

        /**
         * @return 位置标记
         */
        public String getPosition() {
            return position;
        }

        /**
         * @return 字体名称
         */
        public String getFont() {
            return font;
        }

        /**
         * @return 像素为单位的字号
         */
        public Integer getSize() {
            return size;
        }

        /**
         * @return 文本颜色
         */
        public String getColor() {
            return color;
        }

        /**
         * @return 背景颜色
         */
        public String getBackgroundColor() {
            return backgroundColor;
        }

        /**
         * @return 原始字号字符串（不经过px到pt转换）
         */
        public String getRawFontSize() {
            return rawFontSize;
        }

        /**
         * 复制当前选项但清除字号设置。
         *
         * @return 去除字号后的选项拷贝
         */
        public ConvertOptions withoutSize() {
            return builder()
                    .sourceId(this.sourceId)
                    .position(this.position)
                    .font(this.font)
                    .color(this.color)
                    .backgroundColor(this.backgroundColor)
                    .build();
        }

        /**
         * {@link ConvertOptions} 的构造器。
         */
        public static final class Builder {
            private String sourceId;
            private String position;
            private String font;
            private Integer size;
            private String color;
            private String backgroundColor;
            private String rawFontSize;

            /**
             * 设置溯源 ID。
             *
             * @param sourceId 溯源 ID
             * @return 构造器
             */
            public Builder sourceId(String sourceId) {
                this.sourceId = sourceId;
                return this;
            }

            /**
             * 设置位置标识。
             *
             * @param position 位置 ID
             * @return 构造器
             */
            public Builder position(String position) {
                this.position = position;
                return this;
            }

            /**
             * 设置字体。
             *
             * @param font 字体名称
             * @return 构造器
             */
            public Builder font(String font) {
                this.font = font;
                return this;
            }

            /**
             * 设置像素单位的字号。
             *
             * @param size 字号
             * @return 构造器
             */
            public Builder size(Integer size) {
                this.size = size;
                return this;
            }

            /**
             * 设置文本颜色。
             *
             * @param color 颜色值
             * @return 构造器
             */
            public Builder color(String color) {
                this.color = color;
                return this;
            }

            /**
             * 设置背景颜色。
             *
             * @param backgroundColor 背景颜色值
             * @return 构造器
             */
            public Builder backgroundColor(String backgroundColor) {
                this.backgroundColor = backgroundColor;
                return this;
            }

            /**
             * 设置原始字号字符串（不经过px到pt转换，直接作为fontSize输出）。
             *
             * @param rawFontSize 原始字号字符串
             * @return 构造器
             */
            public Builder rawFontSize(String rawFontSize) {
                this.rawFontSize = rawFontSize;
                return this;
            }

            /**
             * 创建转换选项实例。
             *
             * @return 选项
             */
            public ConvertOptions build() {
                return new ConvertOptions(this);
            }
        }
    }

    /**
     * 包装文档数据时使用的页面级配置，如边距与 DPI。
     */
    public static final class DocumentOptions {
        private final ConvertOptions convertOptions;
        private final PageMarginState pageMarginState;
        private final boolean showLock;
        private final int dpi;
        private final Object paperSizeState;

        private DocumentOptions(Builder builder) {
            this.convertOptions = builder.convertOptions == null ? ConvertOptions.builder().build() : builder.convertOptions;
            this.pageMarginState = builder.pageMarginState == null ? PageMarginState.defaultState() : builder.pageMarginState;
            this.showLock = builder.showLock;
            this.dpi = builder.dpi;
            this.paperSizeState = builder.paperSizeState;
        }

        /**
         * 创建新的构造器实例。
         *
         * @return 构造器
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * @return 转换选项
         */
        public ConvertOptions getConvertOptions() {
            return convertOptions;
        }

        /**
         * @return 当前页边距配置
         */
        public PageMarginState getPageMarginState() {
            return pageMarginState;
        }

        /**
         * @return 需要显示锁图标时为 true
         */
        public boolean isShowLock() {
            return showLock;
        }

        /**
         * @return DPI 数值
         */
        public int getDpi() {
            return dpi;
        }

        /**
         * @return 自定义纸张描述
         */
        public Object getPaperSizeState() {
            return paperSizeState;
        }

        /**
         * {@link DocumentOptions} 的构造器。
         */
        public static final class Builder {
            private ConvertOptions convertOptions;
            private PageMarginState pageMarginState;
            private boolean showLock;
            private int dpi = 96;
            private Object paperSizeState;

            /**
             * 配置转换选项。
             *
             * @param convertOptions 内联内容的转换选项
             * @return 构造器
             */
            public Builder convertOptions(ConvertOptions convertOptions) {
                this.convertOptions = convertOptions;
                return this;
            }

            /**
             * 设置页边距状态。
             *
             * @param pageMarginState 页边距设置
             * @return 构造器
             */
            public Builder pageMarginState(PageMarginState pageMarginState) {
                this.pageMarginState = pageMarginState;
                return this;
            }

            /**
             * 设置是否展示锁定标识。
             *
             * @param showLock 是否显示锁图标
             * @return 构造器
             */
            public Builder showLock(boolean showLock) {
                this.showLock = showLock;
                return this;
            }

            /**
             * 设置 DPI 值。
             *
             * @param dpi DPI 数值
             * @return 构造器
             */
            public Builder dpi(int dpi) {
                this.dpi = dpi;
                return this;
            }

            /**
             * 设置纸张尺寸数据。
             *
             * @param paperSizeState 纸张尺寸信息
             * @return 构造器
             */
            public Builder paperSizeState(Object paperSizeState) {
                this.paperSizeState = paperSizeState;
                return this;
            }

            /**
             * 构建文档级配置对象。
             *
             * @return 文档配置
             */
            public DocumentOptions build() {
                return new DocumentOptions(this);
            }
        }
    }

    /**
     * 以厘米表示的页边距数据。
     */
    public static final class PageMarginState {
        private final double top;
        private final double bottom;
        private final double left;
        private final double right;

        private PageMarginState(Builder builder) {
            this.top = builder.top;
            this.bottom = builder.bottom;
            this.left = builder.left;
            this.right = builder.right;
        }

        /**
         * 创建一个构造器。
         *
         * @return 构造器
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * 创建 UI 默认使用的页边距设置。
         *
         * @return 默认状态
         */
        public static PageMarginState defaultState() {
            return builder()
                    .top(2.54)
                    .bottom(2.54)
                    .left(3.17)
                    .right(3.17)
                    .build();
        }

        /**
         * 将页边距对象转换为可序列化的 Map。
         *
         * @return 含上下左右的 Map
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("top", top);
            map.put("bottom", bottom);
            map.put("left", left);
            map.put("right", right);
            return map;
        }

        /**
         * {@link PageMarginState} 的构造器。
         */
        public static final class Builder {
            private double top;
            private double bottom;
            private double left;
            private double right;

            /**
             * 设置上边距。
             *
             * @param top 上边距
             * @return 构造器
             */
            public Builder top(double top) {
                this.top = top;
                return this;
            }

            /**
             * 设置下边距。
             *
             * @param bottom 下边距
             * @return 构造器
             */
            public Builder bottom(double bottom) {
                this.bottom = bottom;
                return this;
            }

            /**
             * 设置左边距。
             *
             * @param left 左边距
             * @return 构造器
             */
            public Builder left(double left) {
                this.left = left;
                return this;
            }

            /**
             * 设置右边距。
             *
             * @param right 右边距
             * @return 构造器
             */
            public Builder right(double right) {
                this.right = right;
                return this;
            }

            /**
             * 构建页边距状态对象。
             *
             * @return 页边距状态
             */
            public PageMarginState build() {
                return new PageMarginState(this);
            }
        }
    }
}
