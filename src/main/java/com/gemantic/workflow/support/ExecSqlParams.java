package com.gemantic.workflow.support;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.utils.JsonUtils;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ExecSqlParams {

    // 应用 ID
    @Schema(description = "应用 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private String appId;
    // sql 片段
    @Schema(description = "sql 片段", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private String sql;
    // 连接名称
    @Schema(description = "连接名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private String sourceName;

    // 见 SqlQuery_datedisplay.json
    private final String jsonTemplate = "{\n" +
            "\t\"appId\": \"984\",\n" +
            "\t\"inputs\": {\n" +
            "\t\t\"database\": {\n" +
            "\t\t\t\"clear_after_run\": true,\n" +
            "\t\t\t\"display_index\": 1001,\n" +
            "\t\t\t\"display_name\": \"选择数据库连接\",\n" +
            "\t\t\t\"field_type\": \"select\",\n" +
            "\t\t\t\"hide_copy\": true,\n" +
            "\t\t\t\"hide_left_handle\": true,\n" +
            "\t\t\t\"hide_right_handle\": true,\n" +
            "\t\t\t\"hide_show\": true,\n" +
            "\t\t\t\"list\": false,\n" +
            "\t\t\t\"multiline\": true,\n" +
            "\t\t\t\"name\": \"database\",\n" +
            "\t\t\t\"password\": false,\n" +
            "\t\t\t\"placeholder\": \"\",\n" +
            "\t\t\t\"required\": true,\n" +
            "\t\t\t\"show\": false,\n" +
            "\t\t\t\"show_editor\": false,\n" +
            "\t\t\t\"type\": \"Database\",\n" +
            "\t\t\t\"value\": {\n" +
            "\t\t\t\t\"label\": \"hc_data_center\",\n" +
            "\t\t\t\t\"value\": \"829\"\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\t\t\"ignore_error\": {\n" +
            "\t\t\t\"clear_after_run\": true,\n" +
            "\t\t\t\"display_index\": 1100,\n" +
            "\t\t\t\"display_name\": \"忽略运行错误\",\n" +
            "\t\t\t\"field_type\": \"checkbox\",\n" +
            "\t\t\t\"hide_copy\": true,\n" +
            "\t\t\t\"hide_left_handle\": true,\n" +
            "\t\t\t\"hide_right_handle\": true,\n" +
            "\t\t\t\"hide_show\": true,\n" +
            "\t\t\t\"list\": false,\n" +
            "\t\t\t\"multiline\": false,\n" +
            "\t\t\t\"name\": \"ignore_error\",\n" +
            "\t\t\t\"password\": false,\n" +
            "\t\t\t\"placeholder\": \"\",\n" +
            "\t\t\t\"required\": false,\n" +
            "\t\t\t\"show\": false,\n" +
            "\t\t\t\"type\": \"bool\",\n" +
            "\t\t\t\"value\": true\n" +
            "\t\t},\n" +
            "\t\t\"output_standard_chart\": {\n" +
            "\t\t\t\"clear_after_run\": true,\n" +
            "\t\t\t\"display_index\": 10000,\n" +
            "\t\t\t\"display_name\": \"输出：原始数据\",\n" +
            "\t\t\t\"field_type\": \"\",\n" +
            "\t\t\t\"is_output\": true,\n" +
            "\t\t\t\"list\": false,\n" +
            "\t\t\t\"multiline\": true,\n" +
            "\t\t\t\"name\": \"output_standard_chart\",\n" +
            "\t\t\t\"password\": false,\n" +
            "\t\t\t\"placeholder\": \"\",\n" +
            "\t\t\t\"required\": true,\n" +
            "\t\t\t\"show\": false,\n" +
            "\t\t\t\"type\": \"dict\",\n" +
            "\t\t\t\"value\": \"\"\n" +
            "\t\t},\n" +
            "\t\t\"sql_template\": {\n" +
            "\t\t\t\"clear_after_run\": true,\n" +
            "\t\t\t\"display_index\": 1002,\n" +
            "\t\t\t\"display_name\": \"SQL\",\n" +
            "\t\t\t\"field_type\": \"button\",\n" +
            "\t\t\t\"hide_copy\": true,\n" +
            "\t\t\t\"hide_left_handle\": true,\n" +
            "\t\t\t\"hide_right_handle\": true,\n" +
            "\t\t\t\"hide_show\": true,\n" +
            "\t\t\t\"list\": false,\n" +
            "\t\t\t\"multiline\": false,\n" +
            "\t\t\t\"name\": \"sql_template\",\n" +
            "\t\t\t\"password\": false,\n" +
            "\t\t\t\"placeholder\": \"编辑 SQL\",\n" +
            "\t\t\t\"required\": true,\n" +
            "\t\t\t\"show\": false,\n" +
            "\t\t\t\"show_editor\": false,\n" +
            "\t\t\t\"type\": \"str\",\n" +
            "\t\t\t\"value\": \"SELECT SUM(vat_amount) AS total_meal_cost FROM er_busitem_other WHERE jkbxr_detail = '屈庆' AND YEAR(startdate) = 2025;\"\n" +
            "\t\t}\n" +
            "\t},\n" +
            "\t\"nodeId\": \"fe1ad745-632d-4993-bc61-e099a1e87c9c\",\n" +
            "\t\"nodeName\": \"SqlQuery\",\n" +
            "\t\"nodeType\": \"sql_query\",\n" +
            "\t\"nodeTypeName\": \"SqlQuery\",\n" +
            "\t\"workflowId\": \"1994\"\n" +
            "}";

    public WorkflowRunResult fillAndGetWorkflowRunResult(Long connectionSourceId) {
        WorkflowRunResult workflowRunResult = JsonUtils.jsonToPojo(jsonTemplate, WorkflowRunResult.class);
        workflowRunResult.setAppId(Long.valueOf(this.getAppId()));
        workflowRunResult.getInputs().getJSONObject("sql_template").put("value", this.getSql());
        JSONObject value = new JSONObject();
        value.put("label", this.getSourceName());
        value.put("value", connectionSourceId);
        workflowRunResult.getInputs().getJSONObject("database").put("value", value);
        return workflowRunResult;
    }


}
