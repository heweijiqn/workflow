package com.gemantic.workflow.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * HeadingNumberingUtil 验证脚本
 * 从 JSON 文件读取 Tiptap 文档，应用标题编号后输出到另一文件
 * <p>
 * 用法：IDE 右键运行 main，或通过 -Dinput、-Dstyle 指定
 * 输出路径根据输入路径自动生成：同目录 + 原文件名_编号后_时间戳.txt
 */
public class HeadingNumberingVerifyRunnerTest {

    private static final String DEFAULT_INPUT = "/Users/wangtianming/Documents/output/新编辑器导入/青岛银行～授信调查报告_编号.txt";
    private static final String DEFAULT_STYLE = "NUMERIC";

    public static void main(String[] args) throws Exception {
        String inputPath = System.getProperty("input", DEFAULT_INPUT);
        // 根据输入路径自动生成输出路径：同目录 + 原文件名_编号后_时间戳.txt
        Path inputFile = Paths.get(inputPath);
        String baseName = inputFile.getFileName() != null
                ? inputFile.getFileName().toString().replaceFirst("\\.txt$", "") : "output";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path outputPath = inputFile.getParent() != null
                ? inputFile.getParent().resolve(baseName + "_编号后_" + timestamp + ".txt")
                : Paths.get(baseName + "_编号后_" + timestamp + ".txt");

        String jsonStr = Files.readString(Paths.get(inputPath), StandardCharsets.UTF_8);
        JSONObject doc = JSON.parseObject(jsonStr);

        // style 优先级：-Dstyle > 输入 JSON 根级 headingNumbering.style > DEFAULT_STYLE
        String style = System.getProperty("style");
        if (style == null || style.trim().isEmpty()) {
            JSONObject headingNumberingInDoc = doc.getJSONObject("headingNumbering");
            if (headingNumberingInDoc != null) {
                style = headingNumberingInDoc.getString("style");
            }
        }
        if (style == null || style.trim().isEmpty()) {
            style = DEFAULT_STYLE;
        }

        System.out.println("输入: " + inputPath);
        System.out.println("输出: " + outputPath);
        System.out.println("样式: " + style);

        JSONObject data = doc.getJSONObject("data");
        if (data == null) {
            throw new IllegalArgumentException("JSON 缺少 data 字段");
        }
        JSONArray content = data.getJSONArray("content");
        if (content == null) {
            throw new IllegalArgumentException("JSON 缺少 data.content 字段");
        }

        List<JSONObject> nodes = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            nodes.add(content.getJSONObject(i));
        }

        // 构建 parameters：content.value.headingNumbering.style
        JSONObject parameters = new JSONObject();
        JSONObject contentValue = new JSONObject();
        JSONObject headingNumbering = new JSONObject();
        headingNumbering.put("style", style);
        contentValue.put("headingNumbering", headingNumbering);
        parameters.put("content", new JSONObject().fluentPut("value", contentValue));

        HeadingNumberingUtil.applyHeadingNumberingIfEnabled(nodes, parameters);

        // 将修改后的 nodes 写回 content
        data.put("content", new JSONArray(nodes));

        String result = doc.toJSONString(JSONWriter.Feature.PrettyFormat, JSONWriter.Feature.WriteNulls);

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.writeString(outputPath, result, StandardCharsets.UTF_8);

        System.out.println("完成，已写入: " + outputPath);
    }
}
