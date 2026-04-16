package com.gemantic.workflow.support;

import com.alibaba.fastjson2.JSONObject;
import com.gemantic.gpt.model.WorkflowPlan;
import com.gemantic.gpt.model.WorkflowRunResult;
import com.gemantic.springcloud.model.Response;
import com.gemantic.springcloud.utils.MsgUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkflowRunInMemoryTest {

    @Test
    public void runSubWorkflowTasks_shouldRunConcurrentlyAndKeepOrder() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        try {
            WorkflowRunInMemory runner = new WorkflowRunInMemory();
            runner.setNodeExecutorService(executorService);
            runner.setNodeConcurrent(true);

            List<WorkflowRunInMemory> tasks = List.of(
                    new SleepWorkflowRun(250, "0"),
                    new SleepWorkflowRun(250, "1"),
                    new SleepWorkflowRun(250, "2")
            );

            long begin = System.currentTimeMillis();
            List<WorkflowRunInMemory.SubWorkflowTaskResult> results = runner.runSubWorkflowTasks(tasks);
            long cost = System.currentTimeMillis() - begin;

            Assert.assertEquals(3, results.size());
            Assert.assertEquals("0", results.get(0).getResponse().getData().getString("idx"));
            Assert.assertEquals("1", results.get(1).getResponse().getData().getString("idx"));
            Assert.assertEquals("2", results.get(2).getResponse().getData().getString("idx"));
            Assert.assertTrue("expected concurrent execution, cost=" + cost, cost < 550);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void call_shouldRunNodesConcurrentlyWhenExecutorConfigured() {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        try {
            ConcurrentNodeWorkflowRun runner = new ConcurrentNodeWorkflowRun(250L);
            runner.setNodeExecutorService(executorService);
            runner.setNodeConcurrent(true);
            WorkflowPlan workflowPlan = new WorkflowPlan();
            workflowPlan.setAppId("1");
            runner.setWorkflowPlan(workflowPlan);

            Map<String, WorkflowRunResult> plan = new LinkedHashMap<>();
            plan.put("A", createNode("A"));
            plan.put("B", createNode("B"));
            plan.put("C", createNode("C"));

            long begin = System.currentTimeMillis();
            Response<Void> response = runner.call(plan, List.of("A", "B", "C"));
            long cost = System.currentTimeMillis() - begin;

            Assert.assertTrue(MsgUtil.isValidMessage(response));
            Assert.assertTrue("expected concurrent node execution, cost=" + cost, cost < 550);
            Assert.assertEquals(3, runner.getCallCount());
        } finally {
            executorService.shutdownNow();
        }
    }

    private WorkflowRunResult createNode(String nodeId) {
        WorkflowRunResult result = new WorkflowRunResult();
        result.setNodeId(nodeId);
        result.setNodeName(nodeId);
        return result;
    }

    private static class SleepWorkflowRun extends WorkflowRunInMemory {
        private final long sleepMillis;
        private final String idx;

        private SleepWorkflowRun(long sleepMillis, String idx) {
            this.sleepMillis = sleepMillis;
            this.idx = idx;
        }

        @Override
        public Response<JSONObject> call() {
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            JSONObject data = new JSONObject();
            data.put("idx", idx);
            return Response.ok(data);
        }
    }

    private static class ConcurrentNodeWorkflowRun extends WorkflowRunInMemory {
        private final long sleepMillis;
        private final AtomicInteger callCount = new AtomicInteger(0);

        private ConcurrentNodeWorkflowRun(long sleepMillis) {
            this.sleepMillis = sleepMillis;
        }

        @Override
        protected Response<Void> call(Map<String, WorkflowRunResult> plan, WorkflowRunResult workflowRunResult) {
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            callCount.incrementAndGet();
            return Response.ok();
        }

        @Override
        protected List<String> getTagetNodeIds(WorkflowRunResult workflowRunResult) {
            return List.of();
        }

        public int getCallCount() {
            return callCount.get();
        }
    }
}
