package com.gemantic.workflow.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.client.QDbStruturedDataInfoClient;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.springcloud.model.Response;
import com.gemantic.workflow.consumer.SqlQuery;
import com.gemantic.workflow.support.ExecSqlParams;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;

@RestController
@RequestMapping(path = "/sqlQuery")
@Tag(name = "sql执行", description = "sqlQuery")
public class SqlQueryController {

    @Resource
    private SqlQuery sqlQuery;

    @Resource
    private QDbStruturedDataInfoClient qDbStruturedDataInfoClient;

    @Operation(
            summary = "执行SQL ",
            description = "快捷执行 SQL "
    )
    @PostMapping("/execSql")
    public Response<List<JSONObject>> execSql(@RequestBody ExecSqlParams execSqlParams) throws Exception {
        String appId = execSqlParams.getAppId();
        String sourceName = execSqlParams.getSourceName();
        Long connectionSourceId = qDbStruturedDataInfoClient.getDatabaseInfoByAppIdAndDbName(Long.valueOf(appId), sourceName).getId();
        WorkflowRunResult workflowRunResult = execSqlParams.fillAndGetWorkflowRunResult(connectionSourceId);
        return sqlQuery.getOutputs(workflowRunResult);
    }

}
