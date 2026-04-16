package com.gemantic.utils;

import com.alibaba.fastjson2.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON转换工具类 - ARM架构优化版
 * 专注于ARM架构兼容的字节解析，解决编码和兼容性问题
 *
 * @author elvis
 */
@Slf4j
public class JsonUtils {

    // Jackson对象映射器
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 静态初始化配置
    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        MAPPER.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
        MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        MAPPER.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    }

    /**
     * JSON字符串转对象
     */
    public static <T> T jsonToPojo(String jsonData, Class<T> beanType) {
        try {
            return MAPPER.readValue(jsonData, beanType);
        } catch (Exception e) {
            log.error("JSON转对象失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证JSON格式
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        try {
            MAPPER.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为JSON数组格式
     */
    private static boolean isJsonArray(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        String trimmed = str.trim();
        return trimmed.startsWith("[") && trimmed.endsWith("]");
    }

    /**
     * 判断字符串是否为JSON对象格式
     */
    private static boolean isJsonObject(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        String trimmed = str.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    /**
     * 将JSONArray转换为JSONObject
     */
    private static JSONObject convertJsonArrayToJsonObject(JSONArray jsonArray) {
        JSONObject result = new JSONObject();
        if (jsonArray != null && !jsonArray.isEmpty()) {
            for (int i = 0; i < jsonArray.size(); i++) {
                result.put(String.valueOf(i), jsonArray.get(i));
            }
        }
        return result;
    }

    /**
     * Jackson JsonNode 转换为普通值的辅助方法
     */
    private static Object convertNodeToValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        } else if (node.isBoolean()) {
            return node.booleanValue();
        } else if (node.isInt()) {
            return node.intValue();
        } else if (node.isLong()) {
            return node.longValue();
        } else if (node.isDouble()) {
            return node.doubleValue();
        } else if (node.isTextual()) {
            return node.textValue();
        } else if (node.isArray()) {
            JSONArray array = new JSONArray();
            for (JsonNode child : node) {
                array.add(convertNodeToValue(child));
            }
            return array;
        } else if (node.isObject()) {
            JSONObject object = new JSONObject();
            node.fields().forEachRemaining(entry -> {
                object.put(entry.getKey(), convertNodeToValue(entry.getValue()));
            });
            return object;
        } else {
//            // 修复：对于其他数值类型，使用asText()而不是序列化JsonNode对象
//            if (node.isNumber()) {
//                return node.numberValue();
//            }
            return node.asText();
        }
    }

    /**
     * 核心方法：处理JSONObject中的字段
     * 支持字符串形式的JSON解析，ARM架构优化
     */
    public static JSONObject handleJsonObject(JSONObject jsonObject, String key) {
        if (jsonObject == null) {
            log.warn("输入的JSONObject为null，返回空对象");
            return new JSONObject();
        }

        try {
            Object value = jsonObject.get(key);

            if (value == null) {
                log.warn("字段 {} 不存在，返回空JSONObject", key);
                return new JSONObject();
            }

            // 如果已经是JSONObject，直接返回
            if (value instanceof JSONObject) {
                return (JSONObject) value;
            }

            // 如果是字符串，尝试解析
            if (value instanceof String) {
                String stringValue = (String) value;
                if (stringValue.trim().isEmpty()) {
                    log.warn("字段 {} 的值为空字符串，返回空JSONObject", key);
                    return new JSONObject();
                }

                // 判断是否是JSON格式
                if (isJsonArray(stringValue)) {
                    JSONArray jsonArray = parseArrayWithArm(stringValue);
                    return convertJsonArrayToJsonObject(jsonArray);
                } else if (isJsonObject(stringValue) || stringValue.trim().startsWith("{")) {
                    return parseJsonObjectWithBytes(stringValue);
                } else {
                    // 不是JSON格式，包装成JSONObject返回
                    JSONObject result = new JSONObject();
                    result.put(key, stringValue);
                    return result;
                }
            }

            // 其他类型，包装成JSONObject返回
            JSONObject result = new JSONObject();
            result.put(key, value);
            return result;

        } catch (Exception e) {
            log.error("处理JSON对象字段 {} 时发生错误: {}", key, e.getMessage());
            JSONObject errorObj = new JSONObject();
            errorObj.put("error", "字段处理失败");
            errorObj.put("field", key);
            errorObj.put("errorMessage", e.getMessage());
            return errorObj;
        }
    }

    /**
     * ARM架构兼容的JSON对象解析方法（保持向后兼容）
     */
    public static JSONObject parseJsonObjectWithArm(String jsonString) {
        return parseJsonObjectWithBytes(jsonString);
    }

    /**
     * 核心方法：基于字节的JSON对象解析
     * 避免字符串编码问题，专为ARM架构优化
     */
    public static JSONObject parseJsonObjectWithBytes(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new JSONObject();
        }

        // 第一层：直接使用Fastjson2解析 结合JSON.toJSONBytes
        try {
            return JSON.parseObject(JSON.toJSONBytes(jsonString, JSONWriter.Feature.LargeObject),
                    JSONReader.Feature.UseBigDecimalForFloats,
                    JSONReader.Feature.UseBigDecimalForDoubles,
                    JSONReader.Feature.SupportAutoType,
                    JSONReader.Feature.IgnoreAutoTypeNotMatch,
                    JSONReader.Feature.AllowUnQuotedFieldNames,
                    JSONReader.Feature.SupportArrayToBean);
        } catch (Exception e) {
            log.warn("Fastjson2解析失败，尝试Jackson解析jsonString: {}", jsonString);
            log.warn("Fastjson2解析失败，尝试Jackson解析: {}", e);
        }

        // 第二层：Jackson解析
        try {
            JsonNode jsonNode = MAPPER.readTree(jsonString);
            // 直接使用Jackson转换为JSONObject
            JSONObject result = new JSONObject();
            if (jsonNode.isObject()) {
                jsonNode.fields().forEachRemaining(entry -> {
                    result.put(entry.getKey(), convertNodeToValue(entry.getValue()));
                });
            }
            return result;
        } catch (Exception e) {
            log.error("Jackson解析也失败: {}", e);
        }

        // 最终层：返回安全的错误对象
        JSONObject errorObj = new JSONObject();
        errorObj.put("parseError", true);
        errorObj.put("errorMessage", "解析失败");
        errorObj.put("dataLength", jsonString.length());
        return errorObj;
    }


    /**
     * 核心方法：ARM架构兼容的JSON数组解析
     * 使用双层解析策略：Fastjson2 + Jackson
     */
    public static JSONArray parseArrayWithArm(String json) {
        if (json == null || json.trim().isEmpty()) {
            log.warn("输入的JSON字符串为空，返回空数组");
            return new JSONArray();
        }

        JSONArray result = new JSONArray();

        // 第一层：直接使用Fastjson2解析
        try {
            String convertedJson;

            // 检查输入是否已经是有效的JSON数组格式
            if (json.trim().startsWith("[") && json.trim().endsWith("]")) {
                // 如果已经是JSON数组格式，直接使用
                convertedJson = json;
                log.debug("输入已是JSON数组格式，直接使用: {}", json);
            } else {
                // 如果不是JSON格式，进行字节转换（ARM兼容性处理）
                byte[] jsonBytes = JSON.toJSONBytes(json, JSONWriter.Feature.LargeObject);
//                convertedJson = new String(jsonBytes);
                // 明确指定UTF-8编码
                convertedJson = new String(jsonBytes, StandardCharsets.UTF_8);
                log.debug("原始JSON: {}", json);
                log.debug("转换后JSON: {}", convertedJson);
            }

            result= JSON.parseArray(convertedJson,
                    JSONReader.Feature.UseBigDecimalForFloats,
                    JSONReader.Feature.UseBigDecimalForDoubles,
                    JSONReader.Feature.SupportAutoType,
                    JSONReader.Feature.IgnoreAutoTypeNotMatch,
                    JSONReader.Feature.AllowUnQuotedFieldNames,
                    JSONReader.Feature.SupportArrayToBean);

            log.info("调用--->JsonUtils转化后:{}", result);
            return result;
        } catch (Exception e) {
            // 这类失败通常是“输入不是数组”，属于预期回退，降级为 warn，避免污染错误日志
            log.warn("Fastjson2数组解析失败，尝试Jackson解析,解析的json长度: {}, 预览: {}",
                    json.length(), json.substring(0, Math.min(500, json.length())), e);
        }

        // 第二层：Jackson解析
        try {
            JsonNode jsonNode = MAPPER.readTree(json);
            if (jsonNode.isArray()) {
                for (JsonNode node : jsonNode) {
                    Object convertedValue = convertNodeToValue(node);
                    // 确保不会添加JsonNode对象本身
                    if (convertedValue instanceof JsonNode) {
                        log.warn("检测到未转换的JsonNode对象，进行二次转换");
                        convertedValue = convertJsonNodeToValue((JsonNode) convertedValue);
                    }
                    result.add(convertedValue);
                }
                log.debug("Jackson成功解析数组，包含 {} 个元素", result.size());

                log.info("调用--->JsonUtils转化后:{}", result);
                return result;
            } else {
                // 详细记录非数组类型的情况
                String nodeType = jsonNode.getNodeType().toString();
                log.error("JSON字符串不是数组格式，解析出的类型为: {}，JSON长度: {}，内容预览: {}",
                        nodeType, json.length(), json.substring(0, Math.min(200, json.length())));

                // 特别处理可能的边界情况
                if (jsonNode.isObject() && json.trim().startsWith("[")) {
                    log.warn("JSON以数组开头但解析为对象，可能存在格式问题");
                }

                return new JSONArray();
            }
        } catch (Exception e) {
            log.error("Jackson JSON数组解析失败，JSON长度: {}，错误: {}", json.length(), e.getMessage());
            return new JSONArray();
        }
    }

    /**
     * 确保JsonNode完全转换为Java对象，避免返回JsonNode本身
     */
    public static Object convertJsonNodeToValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isBoolean()) {
            return node.asBoolean();
        }

        if (node.isInt()) {
            return node.asInt();
        }

        if (node.isLong()) {
            return node.asLong();
        }

        if (node.isDouble() || node.isFloat()) {
            return node.asDouble();
        }

        if (node.isTextual()) {
            return node.asText();
        }

        if (node.isArray()) {
            JSONArray array = new JSONArray();
            for (JsonNode item : node) {
                array.add(convertJsonNodeToValue(item));
            }
            return array;
        }

        if (node.isObject()) {
            JSONObject object = new JSONObject();
            node.fields().forEachRemaining(entry -> {
                object.put(entry.getKey(), convertJsonNodeToValue(entry.getValue()));
            });
            return object;
        }

        // 最后的保险措施：转换为字符串
        return node.asText();
    }


    /**
     * 生成JSON Schema
     */
    public static List<SchemaNode> generateJsonSchema(Object json, String parentPath) {
        List<SchemaNode> schemaList = new ArrayList<>();
        if (json instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) json;
            for (String key : jsonObject.keySet()) {
                String fullPath = parentPath.isEmpty() ? key : parentPath + "-" + key;
                schemaList.addAll(generateJsonSchema(jsonObject.get(key), fullPath));
            }
        } else if (json instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) json;
            // Assume all elements in the array are of the same type
            if (!jsonArray.isEmpty()) {
                schemaList.addAll(generateJsonSchema(jsonArray.getFirst(), parentPath));
            }
        } else {
            String fullPath = parentPath.isEmpty() ? "root" : parentPath;
            SchemaNode schemaNode = new SchemaNode();
            schemaNode.setName(fullPath);
            if (json instanceof String) {
                schemaNode.setType("String");
            } else if (json instanceof Number) {
                schemaNode.setType("Number");
            } else if (json instanceof Boolean) {
                schemaNode.setType("Boolean");
            } else if (json == null) {
                schemaNode.setType("null");
            }
            schemaList.add(schemaNode);
        }
        return schemaList;
    }

    @Setter
    @Getter
    public static class SchemaNode {
        private String name;
        private String type;

        @Override
        public String toString() {
            return "SchemaNode{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }
    }

    /**
     * 核心方法：ARM架构兼容的字节转对象方法
     * 使用双层解析策略：Fastjson2 + Jackson
     */
    public static <T> T jsonToPojoWithBytes(String jsonData, Class<T> beanType) {
        if (jsonData == null || jsonData.trim().isEmpty()) {
            log.warn("JSON数据为空，无法转换为对象");
            return null;
        }

        // 第一层：Fastjson2直接解析
        try {
            return JSON.parseObject(JSON.toJSONBytes(jsonData, JSONWriter.Feature.LargeObject), beanType,
                    JSONReader.Feature.UseBigDecimalForFloats,
                    JSONReader.Feature.UseBigDecimalForDoubles,
                    JSONReader.Feature.SupportAutoType,
                    JSONReader.Feature.IgnoreAutoTypeNotMatch,
                    JSONReader.Feature.AllowUnQuotedFieldNames,
                    JSONReader.Feature.SupportArrayToBean);
        } catch (Exception e) {
            log.warn("Fastjson2直接解析失败，尝试Jackson解析: {}", e.getMessage());
        }

        // 第二层：Jackson解析
        try {
            JsonNode jsonNode = MAPPER.readTree(jsonData);
            return MAPPER.treeToValue(jsonNode, beanType);
        } catch (Exception e) {
            log.error("所有字节解析方法都失败，JSON转对象失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 核心方法：对象转JSON字符串 - ARM架构优化版
     * 使用双层转换策略：Jackson + Fastjson2
     */
    public static String objectToJson(Object data) {
        if (data == null) {
            return null;
        }

        // 第一层：使用Jackson转换
        try {
            return MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("Jackson转换失败，尝试Fastjson2转换: {}", e.getMessage());
        }

        // 第二层：使用Fastjson2转换
        try {
            return JSON.toJSONString(data, JSONWriter.Feature.LargeObject);
        } catch (Exception e) {
            log.error("对象转JSON字符串失败: {}", e.getMessage());
            return null;
        }
    }

    // ARM兼容的序列化方法，整合了toJSONStringWithArm的逻辑
    public static String toJSONStringWithArm(Object obj) {
        if (obj == null) {
            return "null";
        }

        // 如果已经是字符串，检查是否已经被序列化过
        if (obj instanceof String) {
            String str = (String) obj;
            // 检查是否已经是JSON格式的字符串（包含转义字符或以引号开头结尾）
            if (isAlreadyJsonSerialized(str)) {
                return str;
            }
            // 对于普通字符串，直接返回
            return str;
        }

        // 对于数字类型，直接返回字符串表示，避免JSON序列化添加引号
        if (obj instanceof Number) {
            return obj.toString();
        }

        // 对于布尔类型，直接返回字符串表示
        if (obj instanceof Boolean) {
            return obj.toString();
        }

        try {
            byte[] jsonBytes = JSON.toJSONBytes(obj,
                    JSONWriter.Feature.WriteNulls,
                    JSONWriter.Feature.WriteMapNullValue,
                    JSONWriter.Feature.WriteBigDecimalAsPlain,
                    JSONWriter.Feature.BrowserCompatible,
                    JSONWriter.Feature.LargeObject,
                    JSONWriter.Feature.FieldBased);

            String result = new String(jsonBytes, StandardCharsets.UTF_8);

            return result;
        } catch (Exception e) {
            log.warn("ARM兼容序列化失败，对象类型: {}, 错误: {}",
                    obj.getClass().getSimpleName(), e.getMessage());

            // 尝试Jackson作为备选方案
            try {
                return MAPPER.writeValueAsString(obj);
            } catch (Exception jacksonE) {
                log.error("Jackson序列化也失败，使用基础序列化: {}", jacksonE.getMessage());
                return JSON.toJSONString(obj, JSONWriter.Feature.LargeObject);
            }
        }
    }

    /**
     * 检查字符串是否已经被JSON序列化过
     * 通过检测转义字符和JSON格式特征来判断
     */
    private static boolean isAlreadyJsonSerialized(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        // 检查是否包含JSON转义字符
        if (str.contains("\\\"") || str.contains("\\\\") || str.contains("\\n") ||
                str.contains("\\r") || str.contains("\\t") || str.contains("\\f") ||
                str.contains("\\b")) {
            return true;
        }

        // 检查是否以引号开头和结尾（可能是已序列化的字符串）
        if (str.length() >= 2 && str.startsWith("\"") && str.endsWith("\"")) {
            return true;
        }

        // 检查是否是JSON对象或数组格式
        String trimmed = str.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return true;
        }

        return false;
    }

    /**
     * 专门处理DuckDB JsonNode的转换方法
     * DuckDB的JsonNode.toString()返回的是内部描述，需要特殊处理
     */
    public static Object convertDuckDBJsonNodeToValue(org.duckdb.JsonNode duckdbNode) {
        if (duckdbNode == null) {
            return null;
        }

        try {
            // 获取DuckDB JsonNode的字符串表示
            String nodeString = duckdbNode.toString();
            log.debug("DuckDB JsonNode toString(): {}", nodeString);

            // 尝试解析为JSON
            if (isValidJson(nodeString)) {
                // 如果是有效的JSON字符串，解析
                if (isJsonObject(nodeString)) {
                    return parseJsonObjectWithArm(nodeString);
                } else if (isJsonArray(nodeString)) {
                    return parseArrayWithArm(nodeString);
                } else {
                    // 基本类型的JSON值
                    return JSON.parse(nodeString);
                }
            } else {
                // 如果不是有效JSON，直接返回字符串值
                // 去除可能的引号
                if (nodeString.startsWith("\"") && nodeString.endsWith("\"")) {
                    return nodeString.substring(1, nodeString.length() - 1);
                }
                return nodeString;
            }
        } catch (Exception e) {
            log.warn("DuckDB JsonNode转换失败，返回toString()结果: {}", e.getMessage());
            return duckdbNode.toString();
        }
    }

}