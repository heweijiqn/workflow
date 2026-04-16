package com.gemantic.config;

import com.gemantic.springcloud.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.logging.LogLevel;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/logback/config")
@Tag(name = "logback配置", description = "logback配置")
public class LogbackConfigController {

    @Autowired
    private LoggersEndpoint loggersEndpoint;

    /**
     * 获取指定logger的日志级别
     */
    @Operation(
            summary = "查询日志级别",
            description = "查询日志级别",
            operationId = "getLoggerLevel"
    )
    @GetMapping("/level")
    public Response<LoggersEndpoint.LoggerLevelsDescriptor> getLoggerLevel(@Parameter(description = "日志名称，不传默认ROOT")@RequestParam(required = false,defaultValue = "ROOT") String name) {
        return Response.ok(loggersEndpoint.loggerLevels(name));
    }

    /**
     * 动态修改日志级别
     */
    @Operation(
            summary = "修改日志级别",
            description = "修改日志级别",
            operationId = "updateLoggerLevel"
    )
    @PostMapping("/level")
    public Response<String> updateLoggerLevel(
            @Parameter(description = "日志名称，不传默认ROOT")@RequestParam(required = false,defaultValue = "ROOT") String name,
            @RequestParam String level) {
        if (StringUtils.isBlank(level)) {
            return Response.error(null,"Level is required");
        }
        try {
            LogLevel logLevel = LogLevel.valueOf(level.toUpperCase());
            loggersEndpoint.configureLogLevel(name, logLevel);
            return Response.ok("Logger level updated successfully");
        } catch (Exception e) {
            return Response.error(null,"Failed to update logger level: " + e.getMessage());
        }
    }
}
