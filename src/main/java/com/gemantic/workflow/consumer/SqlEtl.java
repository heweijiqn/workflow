package com.gemantic.workflow.consumer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.*;
import com.gemantic.utils.JsonUtils;
import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.StringUtils;
import org.duckdb.DuckDBArray;
import org.duckdb.DuckDBStruct;
import org.duckdb.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.workflow.WorkflowException;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.StringUtil;
import com.gemantic.utils.SqlUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.sf.jsqlparser.JSQLParserException;

import jakarta.annotation.Resource;
import java.util.concurrent.ExecutorService;

import static com.alibaba.fastjson2.JSONReader.Feature.AllowUnQuotedFieldNames;
import static com.gemantic.gpt.util.WorkflowUtil.getNotEmptyInputsByParameterKeys;
import static com.gemantic.utils.JsonUtils.generateJsonSchema;
import static com.gemantic.utils.JsonUtils.isValidJson;

/**
 * SQL ETL，对上游输入的 JSON 数据进行 ETL 处理
 */
@Component("sqlEtl")
public class SqlEtl extends BaseConsumer {

    public static final String JDBC_URL = "jdbc:duckdb:";

    @Resource(name = "sqlEtlExecutorService")
    private ExecutorService sqlEtlExecutorService;

    @Override
    protected ExecutorService getExecutorService() {
        return sqlEtlExecutorService != null ? sqlEtlExecutorService : super.getExecutorService();
    }

    @Value("${CONSUMER_SQL_ETL_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize(){
        return MoreObjects.firstNonNull(consumerBatchSize,super.getConsumerBatchSize());
    }

    @Override
    public Response<List<JSONObject>> getOutputs(WorkflowRunResult workflowRunResult) throws Exception {

        JSONObject inputs = workflowRunResult.getInputs();
        String sqlTemplate = StringUtil.trim(DataParametersUtil.getValueByTemplate("sql_template", inputs, String.class, StringUtils.EMPTY));
        String sqlTemplateDisplayName = DataParametersUtil.getDisplayNameByTemplate("sql_template", inputs);
        if (StringUtils.isBlank(sqlTemplateDisplayName)) {
            sqlTemplateDisplayName = "sql_template";
        }
        Response<List<JSONObject>> errorResponse = Response.error(null);
        // String sql_template_error = String.format("（%s）组件%s输入为空，请检查连接是否正确", workflowRunResult.getNodeName(), sqlTemplateDisplayName);
        if (StringUtils.isBlank(sqlTemplate)) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件%s输入为空，请检查连接是否正确", workflowRunResult.getNodeName(), sqlTemplateDisplayName));
            return errorResponse;
            // throw new WorkflowException(sql_template_error);
        }

        // 获上游输入
        List<String> parameterKeys = WorkflowUtil.getPromptParameterKeys(sqlTemplate);
        JSONObject jsonDataObject = getNotEmptyInputsByParameterKeys(inputs, parameterKeys);

        // 执行 ETL
        try {
            sqlTemplate = SqlUtils.removeComments(sqlTemplate);
            return Response.ok(getSqlResult(workflowRunResult, jsonDataObject, sqlTemplate));
        } catch (Exception e) {
            LOG.error(" ({}）组件执行错误 {}", workflowRunResult.getNodeName(), e);
            errorResponse.getMessage().setMessage(String.format("（%s）组件执行错误 %s", workflowRunResult.getNodeName(), e.getMessage()));
        }
        return errorResponse;
    }

    private List<JSONObject> getSqlResult(WorkflowRunResult workflowRunResult, JSONObject jsonDataObject, String sql) throws Exception {

        Map<String, Object> map = jsonDataObject;
        List<JSONObject> result = Lists.newArrayList();
        Map<String, Path> tempFileMap = Maps.newHashMap();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            // Object o = ((JSONObject) entry.getValue()).get("value");
            // String jsonData = null;
            // if (o instanceof JSONObject) {
            //     jsonData = JSON.toJSONString(o, JSONWriter.Feature.WriteNulls);
            // } else if (o instanceof JSONArray) {
            //     jsonData = JSON.toJSONString((JSONArray) o, JSONWriter.Feature.WriteNulls);
            // }
            // String jsonData = JSON.toJSONString(((JSONObject) entry.getValue()).get("value"), JSONWriter.Feature.WriteNulls);

            String jsonData = new String(JSON.toJSONBytes(((JSONObject) entry.getValue()).get("value"), JSONWriter.Feature.WriteNulls, JSONWriter.Feature.WriteBigDecimalAsPlain));
            // System.out.println("===========");
            // System.out.println(JSON.toJSONString(((JSONObject)entry.getValue()).getJSONObject("value")));
            // System.out.println("===========");
            // String jsonData = "{\"error_code\":\"0\",\"reason\":\"ok\",\"result\":{\"items\":[{\"alias\":\"罗\",\"capital\":[{\"amomon\":\"1834万元人民币\",\"paymet\":\"\",\"percent\":\"91.7%\",\"time\":\"2030-10-20\"}],\"capitalActl\":[{\"amomon\":\"1834.0万元人民币\",\"paymet\":\"\",\"percent\":\"\",\"time\":\"\"}],\"cgid\":null,\"hcgid\":\"2112658565-c7859093\",\"id\":\"2112658565\",\"logo\":\"\",\"name\":\"罗彤\",\"type\":\"2\"},{\"alias\":\"顾\",\"capital\":[{\"amomon\":\"100万元人民币\",\"paymet\":\"\",\"percent\":\"5%\",\"time\":\"2014-04-21\"}],\"capitalActl\":[{\"amomon\":\"100.0万元人民币\",\"paymet\":\"\",\"percent\":\"\",\"time\":\"\"}],\"cgid\":null,\"hcgid\":\"2273458990-c7859093\",\"id\":\"2273458990\",\"logo\":\"\",\"name\":\"顾平\",\"type\":\"2\"},{\"alias\":\"潘\",\"capital\":[{\"amomon\":\"66万元人民币\",\"paymet\":\"\",\"percent\":\"3.3%\",\"time\":\"2014-04-21\"}],\"capitalActl\":[{\"amomon\":\"66.0万元人民币\",\"paymet\":\"\",\"percent\":\"\",\"time\":\"\"}],\"cgid\":null,\"hcgid\":\"2057238558-c7859093\",\"id\":\"2057238558\",\"logo\":\"\",\"name\":\"潘邻霖\",\"type\":\"2\"}],\"total\":\"3\"}}";
            // 创建临时文件
            Path tempFile = Files.createTempFile("temp", ".json");

            // 将JSON数据写入临时文件
            try (OutputStream os = Files.newOutputStream(tempFile, StandardOpenOption.WRITE);
                 Writer writer = new BufferedWriter(new OutputStreamWriter(os))) {
                writer.write(jsonData);
                tempFileMap.put(key, tempFile.toAbsolutePath());
            } catch (IOException e) {
                Files.delete(tempFile);
                String error = String.format("（%s）组件临时文件处理失败", workflowRunResult.getNodeName());
                throw new WorkflowException(error);
            }

        }

        // 使用 DuckDB 读取临时文件中的 JSON 数据
        // if (StringUtils.isBlank(sql)) {
        //     sql = String.format("SELECT * FROM read_json('%s')", tempFile.toAbsolutePath());
        // }
        StringBuilder contentBuilder = new StringBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            String filePath = tempFileMap.get(key).toString();
            try {
                // 读取所有行并将它们添加到StringBuilder中
                Files.lines(Paths.get(filePath), StandardCharsets.UTF_8).forEach(contentBuilder::append);
                // 验证内容是否为合法的 json
                boolean validJson = isValidJson(contentBuilder.toString());
                if (!validJson) {
                    LOG.warn("{} 组件 unvalid json {}, json file {}", workflowRunResult.getNodeName(), contentBuilder, filePath);
                    contentBuilder.setLength(0);
                    continue;
                }
                String replaceKey = String.join("", "\\{\\{", key, "\\}\\}");
                Pattern pattern = Pattern.compile("from\\s*" + replaceKey, Pattern.CASE_INSENSITIVE);
                sql = pattern.matcher(sql).replaceAll(" from '" + filePath + "'");
                sql = sql.replaceAll(replaceKey, "'" + contentBuilder.toString().replaceAll("^\"|\"$", "") + "'");
            } catch (IOException e) {
                LOG.error(String.format("（%s）组件获取文件失败 \n %s", workflowRunResult.getNodeName(), e.getMessage()));
            } finally {
                // 清空内容
                contentBuilder.setLength(0);
            }

            // sql = sql.replace(replaceKey, "'" + filePath + "'");
            // String forValidateSql = sql;
            // forValidateSql = forValidateSql.replace(replaceKey, "tableName");
            // LOG.info("sql : {}", sql);
            // LOG.info("forValidateSql : {}", forValidateSql);
            try {
                SqlUtils.validateJoin(sql);
            } catch (JSQLParserException e) {
                String message = e.getMessage();
                LOG.warn("forValidateSql {}", sql);
                if ("不支持的语法，忽略检查".equals(message)) {
                    // LOG.warn(message);
                } else if ("JOIN 条件没有实际字段或没有意义，请检查 SQL".equals(message)) {
                    // LOG.warn(message);
                } else {
                    throw new WorkflowException(String.format("（%s）组件 %s", workflowRunResult.getNodeName(), message));
                }
            }
        }

        if (sql.contains("{{") && sql.contains("}}")) {
            LOG.warn("{} 组件 json 输入中有不存在的变量 SQL {}", workflowRunResult.getNodeName(), sql);
            throw new WorkflowException(String.format("（%s）组件 json 输入中有不存在的变量", workflowRunResult.getNodeName()));
        }

        // 添加类型替换逻辑 修复
        String optimizedSql = sql.replaceAll("(?i)\\s+as\\s+float\\b", " as DECIMAL(20,4) ")
                .replaceAll("(?i)\\s+as\\s+double\\b", " as DECIMAL(20,4) ");

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA disable_object_cache; " + optimizedSql)) {

            String json = convertResultSetToJson(rs);
            // LOG.info("---- json : {}", json);
            if (json.startsWith("[") && json.endsWith("]")) {
                // ARM架构兼容性配置
                try {
                    JSONArray array = JsonUtils.parseArrayWithArm(json);
//                    LOG.info("调用jsonUtils解析后 : {}", array);
                    JSONObject outputContent = WorkflowUtil.createOutput("output_standard_chart", WorkflowJSONKey.dict.name(), array);
                    result.add(outputContent);
                    JSONObject outputJsonSchema = WorkflowUtil.createOutput("output_json_schema", WorkflowJSONKey.dict.name(), generateJsonSchema(array, ""));
                    result.add(outputJsonSchema);
                } catch (Exception e) {
                    LOG.error("ARM架构JSON解析失败，尝试降级处理: {}", e.getMessage());
                    // 降级处理：逐字符解析或使用字符串替换
                    String processedJson = json.replaceAll("(\\d+\\.\\d+)E([+-]?\\d+)", "\"$1E$2\"");
                    JSONArray array = JsonUtils.parseArrayWithArm(processedJson);
                    JSONObject outputContent = WorkflowUtil.createOutput("output_standard_chart", WorkflowJSONKey.dict.name(), array);
                    result.add(outputContent);
                    JSONObject outputJsonSchema = WorkflowUtil.createOutput("output_json_schema", WorkflowJSONKey.dict.name(), generateJsonSchema(array, ""));
                    result.add(outputJsonSchema);
                }
            } else {
                JSONObject object = JsonUtils.parseJsonObjectWithArm(json);
                JSONObject outputContent = WorkflowUtil.createOutput("output_standard_chart", WorkflowJSONKey.dict.name(), object);
                result.add(outputContent);
                JSONObject outputJsonSchema = WorkflowUtil.createOutput("output_json_schema", WorkflowJSONKey.dict.name(), generateJsonSchema(object, ""));
                result.add(outputJsonSchema);
            }
        } catch (SQLException e) {
            String error = String.format("（%s）组件 SQL 执行失败 \n %s", workflowRunResult.getNodeName(), e.getMessage());
            throw new WorkflowException(error);
        } finally {
            for (Map.Entry<String, Path> pathEntry : tempFileMap.entrySet()) {
                Files.delete(pathEntry.getValue());
            }
        }

        // LOG.info("---- result : {}", result);
        return result;
    }

    private String convertResultSetToJson(ResultSet rs) throws SQLException {
        JSON.config(AllowUnQuotedFieldNames, true);

        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                try {
                    Object value = rs.getObject(i);
                    // LOG.warn("columnName " + metaData.getColumnName(i) + " --> class " + rs.getObject(i).getClass());
                    if (value == null) {
                        row.put(metaData.getColumnName(i), null);
                    } else if (value instanceof Boolean) {
                        // 显式处理布尔值
                        row.put(metaData.getColumnName(i), value);
                    } else if (value instanceof DuckDBArray) {
                        // row.put(metaData.getColumnName(i), rs.getArray(i).getArray());
                        row.put(metaData.getColumnName(i), processDuckDBArray((DuckDBArray) value));

                    } else if (value instanceof Struct) {
                        Map<String, Object> map = ((DuckDBStruct) value).getMap();
                        Map<String, Object> structResult = processDuckDBStructMap(map);
                        if (structResult.containsValue("Error processing DuckDBStruct") || structResult.containsValue("Error processing SQL Array")) {
                            return "Error processing";
                        }
                        row.put(metaData.getColumnName(i), structResult);
                    } else if (value instanceof JsonNode) {
                        // 使用专门的DuckDB JsonNode转换方法
                        Object convertedValue = JsonUtils.convertDuckDBJsonNodeToValue((JsonNode) value);
//                        LOG.info("DuckDB JsonNode转换结果: {} -> {}", value.getClass().getSimpleName(), convertedValue);
                        row.put(metaData.getColumnName(i), convertedValue);
                    } else if (value instanceof Double) {
                        row.put(metaData.getColumnName(i), rs.getBigDecimal(i));
                    } else if (value instanceof Float) {
                        row.put(metaData.getColumnName(i), rs.getBigDecimal(i));
                    } else if (value instanceof BigDecimal) {
                        // 将BigDecimal转换为字符串，避免序列化问题
                        row.put(metaData.getColumnName(i), ((BigDecimal) value).toPlainString());
                    } else {
                        row.put(metaData.getColumnName(i), value);
                    }
                } catch (Exception e) {
                    row.put(metaData.getColumnName(i), null);
                    LOG.error("Error processing column: " + metaData.getColumnName(i) +
                            "columnType: " + metaData.getColumnTypeName(i) +
                            "value: " + rs.getObject(i), e);
                }
            }
            rows.add(row);
        }

        // return JSON.toJSONString(rows, JSONWriter.Feature.WriteNulls, JSONWriter.Feature.LargeObject);
//        return new String(JSON.toJSONBytes(rows, JSONWriter.Feature.WriteNulls, JSONWriter.Feature.WriteBigDecimalAsPlain, JSONWriter.Feature.LargeObject));
        // 使用JsonUtils的ARM兼容方法
        return JsonUtils.toJSONStringWithArm(rows);
    }

    private Map<String, Object> processDuckDBStructMap(Map<String, Object> map) {
        return map.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> processValue(entry.getValue()),
                                (existing, replacement) -> existing, // 在值冲突时保留现有值
                                HashMap::new
                        )
                );

    }

    private Object processValue(Object value) {
        if (value instanceof DuckDBStruct) {
            // 递归调用，处理 DuckDBStruct
            try {
                return processDuckDBStructMap(((DuckDBStruct) value).getMap());
            } catch (Exception e) {
                LOG.error("Error processing DuckDBStruct: " + e.getMessage());
                return "Error processing DuckDBStruct";
            }
        } else if (value instanceof DuckDBArray) {
            try {
                // return  ((DuckDBArray) value).getArray();
                return processDuckDBArray((DuckDBArray) value);
            } catch (SQLException e) {
                LOG.error("Error processing SQL Array: " + e.getMessage());
                return "Error processing SQL Array";
            }
        } else {
            // 非 DuckDBStruct，直接返回值
            return value == null ? "" : value;
        }
    }

    private List<Object> processDuckDBArray(DuckDBArray array) throws SQLException {
        List<Object> arrayList = new ArrayList<>();
        Object[] elements = (Object[]) array.getArray();
        for (Object element : elements) {
            arrayList.add(processValue(element));
        }
        return arrayList;
    }

}
