package com.gemantic.workflow.controller;


import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.constant.*;
import com.gemantic.gpt.model.TraceSession;
import com.gemantic.gpt.model.Workflow;
import com.gemantic.gpt.model.WorkflowRun;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.gpt.support.workflow.*;
import com.gemantic.gpt.util.DataParametersUtil;
import com.gemantic.gpt.util.UserUtil;
import com.gemantic.gpt.util.WorkflowUtil;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.MsgUtil;
import com.gemantic.springcloud.utils.SpringBeanUtil;
import com.gemantic.workflow.consumer.BaseConsumer;
import com.gemantic.workflow.job.ScheduledFutureJob;
import com.gemantic.workflow.job.SubWorkflowStatusJob;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/scheduled")
@Tag(name = "定时器", description = "定时器")
public class ScheduledController {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledController.class);

    @Resource
    private ScheduledFutureJob scheduledFutureJob;

    @Resource
    private SubWorkflowStatusJob subWorkflowStatusJob;

    @Operation(
            summary = "定时器done状态",
            description = "定时器done状态"
    )
    @GetMapping
    public Response<Map<String, JSONObject>> run(
    ) throws Exception {
        return Response.ok(scheduledFutureJob.getScheduledFutureDoneStatus());
    }

    @Operation(
            summary = "创建定时器",
            description = "创建定时器"
    )
    @PostMapping("/create")
    public Response<List<String>> create(
            @Parameter(description = "需要重新创建的定时任务key") @RequestParam(required = false) List<String> consumerKeys,
            @Parameter(description = "如果正在运行是否取消后重新创建") @RequestParam(required = false,defaultValue = "false") Boolean cancelBeforeCreate
    ) throws Exception {
        if(CollectionUtils.isEmpty(consumerKeys)){
            return Response.ok(scheduledFutureJob.scan());
        }
        return Response.ok(scheduledFutureJob.create(consumerKeys,cancelBeforeCreate));
    }

    @Operation(
            summary = "取消、停止定时器",
            description = "取消、停止定时器"
    )
    @PostMapping("/cancel")
    public Response<Map<String,JSONObject>> cancel(
            @Parameter(description = "需要重新创建的定时任务key") @RequestParam List<String> consumerKeys,
            @Parameter(description = "是否从定时任务列表移除") @RequestParam(required = false,defaultValue = "false") Boolean remove
    ) throws Exception {

        return Response.ok(scheduledFutureJob.cancel(consumerKeys,remove));
    }

    @Operation(
            summary = "创建定时器",
            description = "创建定时器"
    )
    @PostMapping("/sub/workflow/job")
    public Response<Void> subWorkflowStatusJob(
            @Parameter(description = "最小更新时间") @RequestParam(required = false) Long minUpdateAt,
            @Parameter(description = "最大更新时间") @RequestParam(required = false) Long maxUpdateAt
    ) throws Exception {
        subWorkflowStatusJob.runByUpdateAt(minUpdateAt,maxUpdateAt);
        return Response.ok();
    }
}