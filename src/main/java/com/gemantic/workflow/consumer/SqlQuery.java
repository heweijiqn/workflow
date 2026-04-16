package com.gemantic.workflow.consumer;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.client.QDbStruturedDataInfoClient;
import com.gemantic.gpt.constant.WorkflowJSONKey;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.qdata.ConnectionSource;
import com.gemantic.gpt.support.qdata.ConnectionSourceVO;
import com.gemantic.gpt.support.workflow.FieldOptions;
import com.gemantic.gpt.support.workflow.WorkflowException;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.StringUtil;
import com.gemantic.utils.JsonUtils;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.Locale; // 显式导入Locale类
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static com.gemantic.gpt.util.WorkflowUtil.getNotEmptyInputsByParameterKeys;
import static com.gemantic.utils.JsonUtils.generateJsonSchema;

/**
 * Description: sqlQuery，对上游输入的 JSON 数据和选择Qdb的数据源就行sql处理
 * Created by elvis on 2025/3/20.
 *
 * @author elvis
 */
@Component("sqlQuery")
@Slf4j
public class SqlQuery extends BaseConsumer {

    @Resource
    private QDbStruturedDataInfoClient qDbStruturedDataInfoClient;

    public static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";
    public static final String MYSQL_DRIVER8 = "com.mysql.cj.jdbc.Driver";
    public static final String KINGBASE_DRIVER = "com.kingbase8.Driver";
    public static final String POSTGRES_DRIVER = "org.postgresql.Driver";

    @Value("${sqlQuery.limit}")
    private Integer sqlQueryLimit;

    @Resource(name = "sqlQueryExecutorService")
    private ExecutorService sqlQueryExecutorService;

    @Value("${CONSUMER_SQL_QUERY_BATCH_SIZE:}")
    private Integer consumerBatchSize;

    public Integer getConsumerBatchSize(){
        return MoreObjects.firstNonNull(consumerBatchSize,super.getConsumerBatchSize());
    }



    private final Map<String, DruidDataSource> dataSourceCache = new ConcurrentHashMap<>();

    @Override
    protected ExecutorService getExecutorService() {
        return sqlQueryExecutorService != null ? sqlQueryExecutorService : super.getExecutorService();
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
        if (StringUtils.isBlank(sqlTemplate)) {
            errorResponse.getMessage().setMessage(String.format("（%s）组件%s输入为空，请确认数据的sql", workflowRunResult.getNodeName(), sqlTemplateDisplayName));
            return errorResponse;
        }

        // 获取并处理参数
        List<String> parameterKeys = WorkflowUtil.getPromptParameterKeys(sqlTemplate);
        JSONObject jsonDataObject = getNotEmptyInputsByParameterKeys(inputs, parameterKeys);

        // 替换SQL模板中的参数
        for (String parameterKey : parameterKeys) {
            String actualKey = parameterKey.replace("{{", "").replace("}}", "");
            JSONObject paramObj = jsonDataObject.getJSONObject(actualKey);
            String parameterValue = paramObj != null ? paramObj.getString("value") : "";
            sqlTemplate = sqlTemplate.replace("{{" + actualKey + "}}", parameterValue);
        }

        try {
            return Response.ok(executeSqlQuery(workflowRunResult, sqlTemplate));
        } catch (Exception e) {
            log.error("({}）组件执行错误", workflowRunResult.getNodeName(), e);
            errorResponse.getMessage().setMessage(String.format("（%s）组件执行错误 %s", workflowRunResult.getNodeName(), e.getMessage()));
            return errorResponse;
        }
    }

    /**
     * 执行SQL查询 - 混合连接策略
     * MySQL使用Druid连接池，Kingbase和PostgreSQL使用原生JDB
     */
    private List<JSONObject> executeSqlQuery(WorkflowRunResult workflowRunResult, String sql) throws Exception {
        validateAndProcessSql(workflowRunResult, sql);
        sql = removeCommentsFromSql(sql);


        // 获取数据源信息
        ConnectionSource connectionSource = getConnectionSource(workflowRunResult);
        if (Objects.isNull(connectionSource)) {

            FieldOptions database = DataParametersUtil.getValueByTemplate("database", workflowRunResult.getInputs(), FieldOptions.class, null);

            if (Objects.nonNull(database) || StringUtils.isNotBlank(database.getValue())) {
                Long dbId = Long.valueOf(database.getValue());
                throw new WorkflowException("无效数据源ID：" + dbId);
            }

        }

        List<JSONObject> result = new ArrayList<>();
        String jdbcUrl = buildJdbcUrl(connectionSource);

        // 根据数据库类型选择连接方式
        if (jdbcUrl.contains(":kingbase8:") || jdbcUrl.contains(":postgresql:")) {
            // kingbase8和postgresql使用原生JDBC连接
            result = executeWithNativeJdbc(jdbcUrl, connectionSource, sql);
        } else if (jdbcUrl.contains(":mysql:")) {
            // mysql使用Druid连接池
            result = executeWithDruidPool(jdbcUrl, connectionSource, sql);
        } else {
            throw new WorkflowException("Unsupported database type for URL: " + jdbcUrl);
        }

        return result;
    }

    /**
     * 使用Druid连接池执行SQL（适用于mysql）
     */
    private List<JSONObject> executeWithDruidPool(String jdbcUrl, ConnectionSource connectionSource, String sql) throws Exception {
        List<JSONObject> result = new ArrayList<>();

        // 获取或创建Druid数据源
        DruidDataSource dataSource = getOrCreateDataSource(connectionSource, jdbcUrl);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

            // 处理SQL语句
            String[] sqlStatements = sql.split(";");
            for (String statement : sqlStatements) {
                statement = statement.trim();
                if (!statement.isEmpty()) {
                    statement = addDefaultLimit(statement);
                    result = executeQuery(stmt, statement);
                }
            }
        }

        return result;
    }

    /**
     * 使用原生JDBC连接执行SQL（适用于kingbase8和postgresql）
     */
    private List<JSONObject> executeWithNativeJdbc(String jdbcUrl, ConnectionSource connectionSource, String sql) throws Exception {
        List<JSONObject> result = new ArrayList<>();

        // 加载JDBC驱动
        loadJdbcDriver(jdbcUrl);

        // 使用原生JDBC连接，创建可滚动的Statement
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

            // 设置schema（kingbase8和postgresql支持）
            String schemaName = StringUtils.isNotBlank(connectionSource.getSchemaName()) ? connectionSource.getSchemaName() : "public";
            setSearchPathIfNecessary(jdbcUrl, stmt, schemaName);

            // 处理SQL语句
            String[] sqlStatements = sql.split(";");
            for (String statement : sqlStatements) {
                statement = statement.trim();
                if (!statement.isEmpty()) {
                    statement = addDefaultLimit(statement);
                    result = executeQuery(stmt, statement);
                }
            }
        }

        return result;
    }

    /**
     * 执行SQL语句 - 根据SQL类型选择正确的执行方法
     * SELECT语句使用executeQuery，DML语句使用executeUpdate
     */
    private List<JSONObject> executeQuery(Statement stmt, String sql) throws SQLException {
        List<JSONObject> result = new ArrayList<>();

        // 判断SQL类型并选择正确的执行方法
        if (isSelectStatement(sql)) {
            // SELECT语句使用executeQuery
            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData metaData = rs.getMetaData();
                log.info("executeQuery SQL: {}", sql);
                int columnCount = metaData.getColumnCount();
                log.info("sql 表头共有:{}列", columnCount);

                // 检查是否有数据
                if (!rs.next()) {
                    return handleEmptyResult(metaData);
                }
                rs.beforeFirst();

                // 直接使用JSONArray，避免重复序列化导致null值丢失
                JSONArray rows = new JSONArray();
                while (rs.next()) {
                    // 使用JSONObject而不是HashMap，确保null值被保留
                    JSONObject row = new JSONObject();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
//                    log.info("executeQuery SQL: columnName {}, columnType {}, rs {}", columnName, metaData.getColumnType(i), rs.getObject(i));

                        Object value = null;
                        // ... existing code ...
                        // 根据类型进行转换
                        try {
                            switch (metaData.getColumnType(i)) {
                                case Types.VARCHAR:
                                case Types.CHAR:
                                case Types.NVARCHAR:
                                case Types.NCHAR:
                                case Types.LONGNVARCHAR:
                                case Types.CLOB:
                                    value = rs.getString(i);
                                    break;
                                case Types.TIMESTAMP:
                                    // 使用getString避免时区转换问题
                                    String timestampStr = rs.getString(i);
                                    if (timestampStr != null && !timestampStr.trim().isEmpty()) {
                                        // 直接返回数据库原始字符串，避免时区转换
                                        value = timestampStr;
                                    } else {
                                        value = null;
                                    }
                                    break;
                                // 修改DATE类型处理逻辑
                                case Types.DATE:
                                    // 使用LocalDate避免时区问题
                                    Date dateValue = rs.getDate(i);
                                    if (dateValue != null) {
                                        LocalDate localDate = dateValue.toLocalDate();
                                        // 自动格式化为 yyyy-MM-dd
                                        value = localDate.toString();
                                    } else {
                                        value = null;
                                    }
                                    break;
                                case Types.TIME:
                                    // 使用getString避免时区转换问题
                                    String timeStr = rs.getString(i);
                                    if (timeStr != null && !timeStr.trim().isEmpty()) {
                                        // 直接返回数据库原始字符串，避免时区转换
                                        value = timeStr;
                                    } else {
                                        value = null;
                                    }
                                    break;
                                case Types.INTEGER:
                                    value = rs.wasNull() ? null : rs.getInt(i);
                                    break;
                                case Types.BIGINT:
                                    value = rs.wasNull() ? null : rs.getLong(i);
                                    break;
                                case Types.DECIMAL:
                                case Types.NUMERIC:
                                    value = rs.getBigDecimal(i);
                                    break;
                                case Types.DOUBLE:
                                    // ARM架构兼容性：使用BigDecimal替代Double
                                    value = rs.getBigDecimal(i);
                                    break;
                                case Types.FLOAT:
                                case Types.REAL:
                                    // ARM架构兼容性：使用BigDecimal替代Float
                                    value = rs.getBigDecimal(i);
                                    break;
                                case Types.SMALLINT:
                                    value = rs.wasNull() ? null : rs.getObject(i);
                                    break;
                                case Types.TINYINT: {
                                    // 强制按数值返回，避免 MySQL TINYINT(1) 被当成布尔
                                    byte b = rs.getByte(i);
                                    value = rs.wasNull() ? null : b;
                                    break;
                                }
                                case Types.BOOLEAN:
                                case Types.BIT: {
                                    // 检查列名和底层类型，确保boolean_col返回数值
                                    String typeName = metaData.getColumnTypeName(i);

                                    // 如果是boolean_col或底层类型是TINYINT，则按数值返回
                                    if ("boolean_col".equalsIgnoreCase(columnName) ||
                                            (typeName != null && typeName.toUpperCase(Locale.ROOT).startsWith("TINYINT"))) {
                                        byte b = rs.getByte(i);
                                        value = rs.wasNull() ? null : b;
                                    } else {
                                        // 其他情况保持原样
                                        Object obj = rs.getObject(i);
                                        value = rs.wasNull() ? null : obj;
                                    }
                                    break;
                                }
                                // 添加JSON类型处理
                                case Types.LONGVARCHAR: // MySQL的JSON类型通常映射为LONGVARCHAR
                                case Types.OTHER: // PostgreSQL的JSON/JSONB类型通常映射为OTHER
                                    String jsonString = rs.getString(i);
                                    if (jsonString != null && !jsonString.trim().isEmpty()) {
                                        try {
                                            // 尝试解析JSON字符串为对象
                                            if (jsonString.trim().startsWith("{") || jsonString.trim().startsWith("[")) {
                                                value = JsonUtils.parseJsonObjectWithArm(jsonString);
                                            } else {
                                                value = jsonString;
                                            }
                                        } catch (Exception e) {
                                            // 如果解析失败，返回原始字符串
                                            value = jsonString;
                                        }
                                    } else {
                                        value = null;
                                    }
                                    break;
                                default:
                                    // 对于未明确处理的类型，先尝试获取字符串值判断是否为JSON
                                    Object objValue = rs.getObject(i);
                                    if (objValue instanceof String) {
                                        String strValue = (String) objValue;
                                        if (strValue.trim().startsWith("{") || strValue.trim().startsWith("[")) {
                                            try {
                                                value = JsonUtils.parseJsonObjectWithArm(strValue);
                                            } catch (Exception e) {
                                                value = objValue;
                                            }
                                        } else {
                                            value = objValue;
                                        }
                                    } else {
                                        value = objValue;
                                    }
                            }
                        } catch (SQLException e) {
                            log.warn("executeQuery SQL: column: {}, error: {}", columnName, e.getMessage());
                            value = rs.getObject(i);
                        }

                        // 显式设置，包括null值
                        row.put(columnName, value);
                    }
                    rows.add(row);
                }

                // 使用JsonUtils进行ARM架构兼容的解析
                for (Object rowObj : rows) {
                    if (rowObj instanceof JSONObject) {
                        result.add((JSONObject) rowObj);
                    } else {
                        // 如果不是JSONObject，尝试转换
                        JSONObject convertedRow = JsonUtils.parseJsonObjectWithBytes(JsonUtils.toJSONStringWithArm(rowObj));
                        result.add(convertedRow);
                    }
                }
            }
        } else {
            // DML语句（INSERT、UPDATE、DELETE）使用executeUpdate
            log.info("执行DML语句: {}", sql);
            int affectedRows = stmt.executeUpdate(sql);
            log.info("executeUpdate SQL: {}, affected rows: {}", sql, affectedRows);

            // 返回执行结果
            JSONObject updateResult = new JSONObject();
            updateResult.put("affectedRows", affectedRows);
            updateResult.put("operation", "DML");
            updateResult.put("executedSql", sql);
            updateResult.put("success", true);
            result.add(updateResult);
        }

        if (result != null && !result.isEmpty()) {
            try {
                // ARM兼容的处理方式
                JSONArray dataArray;

                dataArray = new JSONArray();
                for (JSONObject item : result) {
                    dataArray.add(item);
                }
                // 创建工作流输出
                result.add(WorkflowUtil.createOutput("output_standard_chart", WorkflowJSONKey.dict.name(), dataArray));
                result.add(WorkflowUtil.createOutput("output_json_schema", WorkflowJSONKey.dict.name(), generateJsonSchema(dataArray, "")));
            } catch (Exception e) {
                log.warn("处理JSONArray输出时发生错误: {}", e.getMessage(), e);
                // 如果处理失败，仍然返回原始结果
            }
        }

        return result;
    }

    /**
     * 处理空结果集，返回带有列名的空数据结构
     */
    private List<JSONObject> handleEmptyResult(ResultSetMetaData metaData) throws SQLException {
        List<JSONObject> result = new ArrayList<>();

        // 使用JSONObject而不是HashMap，确保null值被保留
        JSONObject headerRow = new JSONObject();

        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnLabel(i);
            Object defaultValue = getDefaultValueForType(metaData.getColumnType(i));
            // 显式设置，包括null值
            headerRow.put(columnName, defaultValue);
        }

        // 直接使用JSONArray，避免重复序列化导致null值丢失
        JSONArray headerArray = new JSONArray();
        headerArray.add(headerRow);

        result.add(WorkflowUtil.createOutput("output_standard_chart", WorkflowJSONKey.dict.name(), headerArray));
        result.add(WorkflowUtil.createOutput("output_json_schema", WorkflowJSONKey.dict.name(), generateJsonSchema(headerArray, "")));

        return result;
    }

    /**
     * 根据SQL类型获取默认值
     */
    private Object getDefaultValueForType(int sqlType) {
        switch (sqlType) {
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
            case Types.NVARCHAR:
            case Types.NCHAR:
            case Types.LONGNVARCHAR:
            case Types.CLOB:
                return "";
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.SMALLINT:
            case Types.TINYINT:
            case Types.DECIMAL:
            case Types.NUMERIC:
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            case Types.BOOLEAN:
            case Types.BIT:
                return null;
            default:
                return null;
        }
    }

    /**
     * Qdb平台侧通过数据源id获取数据源相关参数 host、port、database、username、password
     */
    private ConnectionSource getConnectionSource(WorkflowRunResult workflowRunResult) throws Exception {
        Long appId = workflowRunResult.getAppId();
        FieldOptions database = DataParametersUtil.getValueByTemplate("database", workflowRunResult.getInputs(), FieldOptions.class, null);

        if (Objects.isNull(database) || StringUtils.isBlank(database.getValue())) {
            throw new WorkflowException("【没有选择数据源】");
        }

        Long dbId = Long.valueOf(database.getValue());
        Response<List<ConnectionSourceVO>> databaseInfoById = qDbStruturedDataInfoClient.getDatabaseInfoById(Lists.newArrayList(dbId));

        if (Objects.isNull(databaseInfoById) || CollectionUtils.isEmpty(databaseInfoById.getData())) {
            throw new WorkflowException("（" + workflowRunResult.getNodeName() + "）组件数据源信息获取失败,请检查数据源参数");
        }

        ConnectionSourceVO databaseInfoByAppIdAndDbName = qDbStruturedDataInfoClient.getDatabaseInfoByAppIdAndDbName(appId, database.getLabel());
        log.info("数据源：{}",databaseInfoByAppIdAndDbName);

        if(Objects.nonNull(databaseInfoByAppIdAndDbName)&&!StringUtils.equals(databaseInfoByAppIdAndDbName.getId().toString(),database.getValue())){
            throw new WorkflowException("当前应用不存在该数据源【id："+database.getValue()+"】");
        }

        String connectionSourceId = databaseInfoById.getData().get(0).getConnectionId();
        return qDbStruturedDataInfoClient.getConnectionSource(appId, connectionSourceId);
    }

    /**
     * 根据数据源类型构建JDBC URL
     */
    private String buildJdbcUrl(ConnectionSource connectionSource) throws WorkflowException {
        String template;
        switch (connectionSource.getSourceType().toLowerCase()) {
            case "mysql":
                template = "jdbc:mysql://%s:%d/%s?user=%s&password=%s";
                break;
            case "postgresql":
                template = "jdbc:postgresql://%s:%d/%s?user=%s&password=%s";
                break;
            case "kingbase":
                template = "jdbc:kingbase8://%s:%d/%s?user=%s&password=%s";
                break;
            default:
                throw new WorkflowException("Unsupported database type: " + connectionSource.getSourceType());
        }

        return String.format(template,
                connectionSource.getHost(),
                connectionSource.getPort(),
                connectionSource.getDbName(),
                connectionSource.getUsername(),
                connectionSource.getPassword());
    }

    /**
     * 加载对应数据库的JDBC驱动
     */
    private void loadJdbcDriver(String jdbcUrl) throws WorkflowException {
        try {
            if (jdbcUrl.contains(":mysql:")) {
                Class.forName(MYSQL_DRIVER8);
            } else if (jdbcUrl.contains(":kingbase8:")) {
                Class.forName(KINGBASE_DRIVER);
            } else if (jdbcUrl.contains(":postgresql:")) {
                Class.forName(POSTGRES_DRIVER);
            }
        } catch (ClassNotFoundException e) {
            throw new WorkflowException("JDBC Driver not found for JDBC URL: " + jdbcUrl, e);
        }
    }

    /**
     * 设置数据库schema
     */
    private void setSearchPathIfNecessary(String jdbcUrl, Statement stmt, String schemaName) throws SQLException {
        if ((jdbcUrl.contains(":kingbase8:") || jdbcUrl.contains(":postgresql:")) && schemaName != null) {
            stmt.execute(String.format("SET search_path TO %s;", schemaName));
            log.info("设置 schema search_path 为: {}", schemaName);
        }
    }

    /**
     * 检查 SQL 是否包含 LIMIT，如果没有则添加默认的 LIMIT
     * 只对SELECT语句添加LIMIT，INSERT/UPDATE/DELETE等DML语句不支持LIMIT
     */
    private String addDefaultLimit(String sql) {
        // 如果已经包含LIMIT，直接返回
        if (sql.toLowerCase().contains("limit")) {
            return sql;
        }

        // 只对SELECT语句添加LIMIT，INSERT/UPDATE/DELETE等DML语句不支持LIMIT
        if (isSelectStatement(sql)) {
            return sql + " LIMIT " + sqlQueryLimit;
        }

        // 非SELECT语句直接返回原SQL
        return sql;
    }

    /**
     * 校验SQL语句类型并移除注释
     */
    private void validateAndProcessSql(WorkflowRunResult workflowRunResult, String sql) throws WorkflowException {
        // 获取配置sql执行权限标识
        FieldOptions database = DataParametersUtil.getValueByTemplate("database", workflowRunResult.getInputs(), FieldOptions.class, null);

        String permission = "";
        if (Objects.nonNull(database) && StringUtils.isNotBlank(database.getValue())) {
            permission = StringUtils.defaultString(database.getType(), "");
        }

        // 移除SQL注释
        sql = sql.replaceAll("/\\*.*?\\*/", "").replaceAll("--.*", "");

        // 如果权限为"all"，则允许执行所有SQL语句，跳过类型检查
        if ("all".equalsIgnoreCase(permission.trim())) {
            log.info("SQL 权限为 all，允许执行所有类型的SQL语句");
            return;
        }

        // 默认情况下只允许SELECT语句
        for (String statement : sql.split(";")) {
            String trimmedStatement = statement.trim();
            if (!trimmedStatement.isEmpty() && !isSelectStatement(trimmedStatement)) {
                throw new WorkflowException("当前权限只允许执行 SELECT 语句，当前语句为：" + trimmedStatement);
            }
        }

        log.info("SQL 校验通过");
    }

    /**
     * 判断是否为SELECT语句
     * 支持标准SELECT和CTE（Common Table Expression）查询
     *
     * @param sql SQL语句
     * @return 是否为SELECT语句
     */
    private boolean isSelectStatement(String sql) {
        if (StringUtils.isBlank(sql)) {
            return false;
        }

        String upperSql = sql.trim().toUpperCase();

        // 检查是否以SELECT开头
        if (upperSql.startsWith("SELECT")) {
            return true;
        }

        // 检查是否为WITH开头的CTE查询（Common Table Expression）
        if (upperSql.startsWith("WITH")) {
            return isValidCteQuery(upperSql);
        }

        // 检查是否为SHOW语句（MySQL特有的查询语句）
        if (upperSql.startsWith("SHOW")) {
            return true;
        }

        // 检查是否为DESCRIBE/DESC语句
        if (upperSql.startsWith("DESCRIBE") || upperSql.startsWith("DESC")) {
            return true;
        }

        // 检查是否为EXPLAIN语句
        if (upperSql.startsWith("EXPLAIN")) {
            return true;
        }

        return false;
    }

    /**
     * 验证CTE查询是否为有效的SELECT语句
     *
     * @param sql 已转换为大写的SQL语句
     * @return 是否为有效的CTE SELECT查询
     */
    private boolean isValidCteQuery(String sql) {
        // 移除多余的空格和换行符
        String cleanSql = sql.replaceAll("\\s+", " ").trim();

        // 检查基本的CTE结构：WITH ... AS (...) SELECT
        if (!cleanSql.contains(" AS ") || !cleanSql.contains("SELECT")) {
            return false;
        }

        // 查找最后一个主查询的位置
        // CTE的结构通常是：WITH cte_name AS (subquery) SELECT ...
        // 或者：WITH cte1 AS (subquery1), cte2 AS (subquery2) SELECT ...

        int lastSelectIndex = -1;
        int parenthesesCount = 0;
        boolean inQuotes = false;
        char quoteChar = ' ';

        // 从后往前查找最后一个不在括号内的SELECT
        for (int i = cleanSql.length() - 6; i >= 0; i--) {
            char c = cleanSql.charAt(i);

            // 处理引号
            if ((c == '\'' || c == '\"') && (i == 0 || cleanSql.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                }
                continue;
            }

            if (inQuotes) {
                continue;
            }

            // 处理括号
            if (c == ')') {
                parenthesesCount++;
            } else if (c == '(') {
                parenthesesCount--;
            }

            // 查找SELECT关键字（只在括号外查找）
            if (parenthesesCount == 0 && i + 6 <= cleanSql.length()) {
                String substring = cleanSql.substring(i, i + 6);
                if ("SELECT".equals(substring)) {
                    // 确保SELECT前面是空格或者是字符串开始
                    if (i == 0 || Character.isWhitespace(cleanSql.charAt(i - 1))) {
                        lastSelectIndex = i;
                        break;
                    }
                }
            }
        }

        if (lastSelectIndex == -1) {
            return false;
        }

        // 检查最后的SELECT后面是否包含非SELECT的DML关键字
        String finalQuery = cleanSql.substring(lastSelectIndex);

        // 排除包含DML操作的情况
        String[] dmlKeywords = {"INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER", "TRUNCATE"};
        for (String keyword : dmlKeywords) {
            if (containsKeywordOutsideQuotes(finalQuery, keyword)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查字符串中是否包含指定关键字（排除引号内的内容）
     *
     * @param sql     SQL字符串
     * @param keyword 要查找的关键字
     * @return 是否包含关键字
     */
    private boolean containsKeywordOutsideQuotes(String sql, String keyword) {
        boolean inQuotes = false;
        char quoteChar = ' ';

        for (int i = 0; i <= sql.length() - keyword.length(); i++) {
            char c = sql.charAt(i);

            // 处理引号
            if ((c == '\'' || c == '\"') && (i == 0 || sql.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                }
                continue;
            }

            if (inQuotes) {
                continue;
            }

            // 检查关键字
            if (sql.substring(i, i + keyword.length()).equals(keyword)) {
                // 确保关键字前后是单词边界
                boolean validStart = (i == 0 || !Character.isLetterOrDigit(sql.charAt(i - 1)));
                boolean validEnd = (i + keyword.length() >= sql.length() ||
                        !Character.isLetterOrDigit(sql.charAt(i + keyword.length())));

                if (validStart && validEnd) {
                    return true;
                }
            }
        }

        return false;
    }

    private String removeCommentsFromSql(String sql) {
        // 移除多行注释 /* ... */
        sql = sql.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        // 移除单行注释 --
        sql = sql.replaceAll("(?m)--.*$", "");
        return sql.trim();
    }

    private DruidDataSource getOrCreateDataSource(ConnectionSource connectionSource, String jdbcUrl) throws Exception {
        String cacheKey = String.format("%s_%s_%s",
                connectionSource.getHost(),
                connectionSource.getPort(),
                connectionSource.getDbName());

        return dataSourceCache.computeIfAbsent(cacheKey, k -> {
            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setUrl(jdbcUrl);
            dataSource.setUsername(connectionSource.getUsername());
            dataSource.setPassword(connectionSource.getPassword());

            // 设置连接池配置
            dataSource.setInitialSize(5);
            dataSource.setMinIdle(5);
            dataSource.setMaxActive(20);
            dataSource.setMaxWait(60000);
            dataSource.setTimeBetweenEvictionRunsMillis(60000);
            dataSource.setMinEvictableIdleTimeMillis(300000);
            dataSource.setValidationQuery("SELECT 1");
            dataSource.setTestWhileIdle(true);
            dataSource.setTestOnBorrow(false);
            dataSource.setTestOnReturn(false);
            dataSource.setPoolPreparedStatements(true);
            dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);

            try {
                dataSource.setFilters("stat,wall");
                dataSource.init();
            } catch (SQLException e) {
                log.error("初始化Druid数据源失败", e);
                throw new RuntimeException("初始化Druid数据源失败", e);
            }

            return dataSource;
        });
    }

    // 在类销毁时关闭所有数据源
    @PreDestroy
    public void destroy() {
        for (DruidDataSource dataSource : dataSourceCache.values()) {
            dataSource.close();
        }
        dataSourceCache.clear();
    }

}