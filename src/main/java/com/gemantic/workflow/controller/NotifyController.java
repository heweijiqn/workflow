package com.gemantic.workflow.controller;


import com.gemantic.gpt.model.NotifyLog;
import com.gemantic.springcloud.model.Response;
import com.gemantic.workflow.repository.NotifyRepository;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/notify")
@Tag(name = "通知日志")
public class NotifyController {

    private static final Logger LOG = LoggerFactory.getLogger(NotifyController.class);

    @Resource
    private NotifyRepository notifyRepository;


    @Operation(description = "提交通知", summary = "提交通知")
    @ApiOperationSupport(order = 1)
    @PostMapping
    public Response<List<NotifyLog>> save(
            @Parameter(description  = "对象", required = true) @RequestBody List<NotifyLog> data
    ) throws Exception {
        return notifyRepository.push(data);
    }
}