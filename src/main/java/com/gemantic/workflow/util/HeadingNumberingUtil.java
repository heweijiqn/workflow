package com.gemantic.workflow.util;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tiptap 文档标题自动编号工具类
 * <p>
 * 在 Tiptap 文档变量替换完成后，对 heading 节点的 attrs.headingNumber 进行自动填充。
 * 与前端 useHeadingNumbering.js / HeadingNumbering 扩展行为保持一致。
 * </p>
 *
 * <h3>配置来源</h3>
 * <ul>
 *   <li>路径：parameters.content.value.headingNumbering.style</li>
 *   <li>合法值：NONE | CHINESE | NUMERIC</li>
 * </ul>
 *
 * <h3>不处理条件（以下任一均跳过编号）</h3>
 * <ul>
 *   <li>无 headingNumbering 对象</li>
 *   <li>有 headingNumbering 但无 style 字段</li>
 *   <li>style 为 NONE 或空</li>
 * </ul>
 *
 * <h3>编号样式</h3>
 * <ul>
 *   <li>NUMERIC：1、1.1、1.1.1、…</li>
 *   <li>CHINESE：一级 一、二、…十、十一、…十九、二十…；二级 （一）（二）…；三级 1、2、…</li>
 * </ul>
 *
 * <h3>遍历范围</h3>
 * 顶层 content、blockquote、bulletList、orderedList、listItem、table 单元格内的 heading。
 */
public final class HeadingNumberingUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HeadingNumberingUtil.class);

    private HeadingNumberingUtil() {
    }

    /** 无编号 */
    public static final String STYLE_NONE = "NONE";
    /** 中文编号：一、（一）、1 */
    public static final String STYLE_CHINESE = "CHINESE";
    /** 数字层级：1、1.1、1.1.1 */
    public static final String STYLE_NUMERIC = "NUMERIC";

    /** 中文数字 1-9，下标 0 预留（标题从 1 起无零） */
    private static final String[] CN_DIGITS = {"", "一", "二", "三", "四", "五", "六", "七", "八", "九"};

    /**
     * 对 Tiptap 文档节点列表应用标题自动编号（若配置启用）
     *
     * @param nodes      顶层 content 节点列表（会被就地修改，heading 节点 attrs.headingNumber 被填充）
     * @param parameters 参数对象，从中读取 headingNumbering.style
     */
    public static void applyHeadingNumberingIfEnabled(List<JSONObject> nodes, JSONObject parameters) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        String style = getHeadingNumberingStyle(parameters);
        // 未配置时默认 NUMERIC，保证同级标题从上到下连续编号（1、2、3…）
        if (!isHeadingNumberingEnabled(style)) {
            style = STYLE_NUMERIC;
        }
        // 两遍扫描：先按文档流收集所有 heading，再统一编号，保证 level 1 的 1、2、3… 严格按出现顺序
        List<JSONObject> headingsInOrder = new ArrayList<>();
        collectHeadingsInOrder(nodes, headingsInOrder);
        int[] counters = new int[7];
        for (JSONObject heading : headingsInOrder) {
            int level = getHeadingLevel(heading);
            String number = generateNumber(level, style, counters);
            JSONObject attrs = heading.getJSONObject("attrs");
            if (attrs == null) {
                attrs = new JSONObject();
                heading.put("attrs", attrs);
            }
            attrs.put("headingNumber", number);
            LOG.debug("标题编号: level={}, number={}", level, number);
        }
    }

    /** 按文档流收集所有 heading 到 list，保证顺序与阅读顺序一致 */
    private static void collectHeadingsInOrder(List<JSONObject> nodes, List<JSONObject> out) {
        for (JSONObject node : nodes) {
            if (node == null) continue;
            String type = node.getString("type");
            if ("heading".equals(type)) {
                out.add(node);
            } else if ("blockquote".equals(type) || "bulletList".equals(type) || "orderedList".equals(type)) {
                JSONArray content = node.getJSONArray("content");
                if (content != null) collectHeadingsInOrder(toList(content), out);
            } else if ("listItem".equals(type)) {
                JSONArray content = node.getJSONArray("content");
                if (content != null) {
                    for (int i = 0; i < content.size(); i++) {
                        JSONObject item = content.getJSONObject(i);
                        if (item != null) {
                            collectHeadingsInOrder(Collections.singletonList(item), out);
                            JSONArray itemContent = item.getJSONArray("content");
                            if (itemContent != null) collectHeadingsInOrder(toList(itemContent), out);
                        }
                    }
                }
            } else if ("table".equals(type)) {
                collectHeadingsFromTable(node, out);
            } else if ("conditionTemplate".equals(type)) {
                JSONArray content = node.getJSONArray("content");
                if (content != null) collectHeadingsInOrder(toList(content), out);
            } else {
                JSONArray content = node.getJSONArray("content");
                if (content != null && !content.isEmpty()) collectHeadingsInOrder(toList(content), out);
            }
        }
    }

    private static void collectHeadingsFromTable(JSONObject tableNode, List<JSONObject> out) {
        JSONArray rows = tableNode.getJSONArray("content");
        if (rows == null) return;
        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (row == null || !"tableRow".equals(row.getString("type"))) continue;
            JSONArray cells = row.getJSONArray("content");
            if (cells == null) continue;
            for (int j = 0; j < cells.size(); j++) {
                JSONObject cell = cells.getJSONObject(j);
                if (cell == null) continue;
                JSONArray cellContent = cell.getJSONArray("content");
                if (cellContent != null) collectHeadingsInOrder(toList(cellContent), out);
            }
        }
    }

    /**
     * 从 parameters.content.value.headingNumbering.style 读取配置
     */
    private static String getHeadingNumberingStyle(JSONObject parameters) {
        if (parameters == null) {
            return null;
        }
        JSONObject template = parameters.getJSONObject("content");
        if (template == null) {
            return null;
        }
        template = template.getJSONObject("value");
        if (template == null) {
            return null;
        }
        JSONObject hn = template.getJSONObject("headingNumbering");
        if (hn == null) {
            return null;
        }
        String style = hn.getString("style");
        return StringUtils.isNotBlank(style) ? style.trim().toUpperCase() : null;
    }

    /** 仅 CHINESE、NUMERIC 时返回 true */
    private static boolean isHeadingNumberingEnabled(String style) {
        return STYLE_CHINESE.equals(style) || STYLE_NUMERIC.equals(style);
    }

    /** Tiptap heading level 1-6，非法时默认 1 */
    private static int getHeadingLevel(JSONObject node) {
        JSONObject attrs = node.getJSONObject("attrs");
        if (attrs == null) {
            return 1;
        }
        Integer level = attrs.getInteger("level");
        if (level == null || level < 1 || level > 6) {
            return 1;
        }
        return level;
    }

    private static String generateNumber(int level, String style, int[] counters) {
        if (STYLE_NUMERIC.equals(style)) {
            return generateNumberNumeric(level, counters);
        }
        if (STYLE_CHINESE.equals(style)) {
            return generateNumberChinese(level, counters);
        }
        return "";
    }

    /**
     * 数字层级：进入更深层级时重置子级，同级递增。如 1、1.1、1.1.1、1.2、2、…
     */
    private static String generateNumberNumeric(int level, int[] counters) {
        // 若直接从更深层级开始（如首个标题就是 level=2/3），将缺失父级补到 1，避免出现 0.1 / 1.0.1
        for (int i = 1; i < level; i++) {
            if (counters[i] <= 0) {
                counters[i] = 1;
            }
        }
        for (int i = level + 1; i < counters.length; i++) {
            counters[i] = 0;
        }
        counters[level]++;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= level; i++) {
            if (i > 1) {
                sb.append(".");
            }
            sb.append(counters[i]);
        }
        sb.append("、");
        return sb.toString();
    }

    /**
     * 中文编号：一级 一、二、…；二级 （一）（二）…；三级及以上 1、2、…
     */
    private static String generateNumberChinese(int level, int[] counters) {
        for (int i = level + 1; i < counters.length; i++) {
            counters[i] = 0;
        }
        counters[level]++;
        int n = counters[level];
        return switch (level) {
            case 1 -> toChinese(n) + "、";
            case 2 -> "（" + toChinese(n) + "）";
            default -> n + "、";
        };
    }

    /**
     * 中文数字转换（标题编号从 1 起，无零）
     * <ul>
     *   <li>1-9：一、二、…、九</li>
     *   <li>10：十</li>
     *   <li>11-19：十一、十二、…、十九</li>
     *   <li>20-99：二十、二十一、…、九十九</li>
     *   <li>≥100：阿拉伯数字</li>
     * </ul>
     */
    private static String toChinese(int n) {
        if (n < 1) {
            return "";
        }
        if (n < 10) {
            return CN_DIGITS[n];
        }
        if (n < 20) {
            return "十" + (n == 10 ? "" : CN_DIGITS[n % 10]);
        }
        if (n < 100) {
            int tens = n / 10;
            int ones = n % 10;
            String s = (tens == 1 ? "" : CN_DIGITS[tens]) + "十";
            return ones == 0 ? s : s + CN_DIGITS[ones];
        }
        return String.valueOf(n);
    }

    private static List<JSONObject> toList(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return Collections.emptyList();
        }
        List<JSONObject> list = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            list.add(arr.getJSONObject(i));
        }
        return list;
    }
}
