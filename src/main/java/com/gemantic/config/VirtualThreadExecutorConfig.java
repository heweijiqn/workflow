package com.gemantic.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 虚拟线程执行器配置（JDK 21+）
 * 
 * 虚拟线程特别适合 IO 密集型任务，如：
 * - SqlEtl: 数据库查询和文件IO操作
 * - ApiRequest: HTTP 网络请求
 * - SqlQuery: 数据库连接和查询操作
 * - SubWorkflowList: 批量创建工作流运行结果
 * 
 * 对于3000节点工作流同时运行10次（共30000节点）的场景，
 * 虚拟线程可以实现近乎无限的并发能力，因为虚拟线程是轻量级的，
 * 不会像平台线程那样消耗大量内存和系统资源。
 * 
 * 启用方式：设置 consumer.virtualThread.enabled=true
 */
@Configuration
public class VirtualThreadExecutorConfig {

    private static final Logger LOG = LoggerFactory.getLogger(VirtualThreadExecutorConfig.class);
    static final String VIRTUAL_THREAD_PARALLELISM = "jdk.virtualThreadScheduler.parallelism";
    static final String VIRTUAL_THREAD_MAX_POOL_SIZE = "jdk.virtualThreadScheduler.maxPoolSize";
    static final String VIRTUAL_THREAD_MIN_RUNNABLE = "jdk.virtualThreadScheduler.minRunnable";

    @Value("${consumer.virtualThread.scheduler.parallelism:0}")
    private Integer schedulerParallelism;

    @Value("${consumer.virtualThread.scheduler.maxPoolSize:0}")
    private Integer schedulerMaxPoolSize;

    @Value("${consumer.virtualThread.scheduler.minRunnable:0}")
    private Integer schedulerMinRunnable;

    @PostConstruct
    void configureVirtualThreadScheduler() {
        configureSchedulerProperty(VIRTUAL_THREAD_PARALLELISM, schedulerParallelism);
        configureSchedulerProperty(VIRTUAL_THREAD_MAX_POOL_SIZE, schedulerMaxPoolSize);
        configureSchedulerProperty(VIRTUAL_THREAD_MIN_RUNNABLE, schedulerMinRunnable);
    }

    private void configureSchedulerProperty(String key, Integer value) {
        if (value == null || value <= 0) {
            return;
        }
        String targetValue = String.valueOf(value);
        String existingValue = System.getProperty(key);
        if (existingValue == null) {
            System.setProperty(key, targetValue);
            LOG.info("配置虚拟线程调度器参数 {}={}", key, targetValue);
            return;
        }
        if (!existingValue.equals(targetValue)) {
            LOG.warn("虚拟线程调度器参数已存在 {}={}, 跳过覆盖目标值 {}", key, existingValue, targetValue);
        }
    }

    private ExecutorService newVirtualExecutor(String threadNamePrefix) {
        ThreadFactory factory = Thread.ofVirtual().name(threadNamePrefix + "-", 0).factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    /**
     * SqlEtl 虚拟线程执行器
     * 虚拟线程可以自动扩展，无需设置核心线程数和最大线程数
     */
    @Bean(name = "sqlEtlExecutorService", destroyMethod = "shutdown")
    @Primary
    public ExecutorService sqlEtlVirtualThreadExecutor() {
        LOG.info("创建 SqlEtl 虚拟线程执行器 - 支持无限并发IO操作");
        return newVirtualExecutor("vt-sql-etl");
    }

    /**
     * ApiRequest 虚拟线程执行器
     * 特别适合高并发HTTP请求场景
     */
    @Bean(name = "apiRequestExecutorService", destroyMethod = "shutdown")
    @Primary
    public ExecutorService apiRequestVirtualThreadExecutor() {
        LOG.info("创建 ApiRequest 虚拟线程执行器 - 支持无限并发HTTP请求");
        return newVirtualExecutor("vt-api-request");
    }

//    /**
//     * DocOutput 虚拟线程执行器
//     * 特别适合高并发文档生成场景（文件IO、模板渲染）
//     */
//    @Bean(name = "docOutputExecutorService")
//    @Primary
//    public ExecutorService docOutputVirtualThreadExecutor() {
//        LOG.info("创建 DocOutput 虚拟线程执行器 - 支持无限并发文档生成");
//        return Executors.newVirtualThreadPerTaskExecutor();
//    }

    /**
     * SubWorkflowList 虚拟线程执行器
     * 特别适合高并发循环调用工作流场景（批量创建工作流运行结果）
     */
    @Bean(name = "subWorkflowListExecutorService", destroyMethod = "shutdown")
    @Primary
    public ExecutorService subWorkflowListVirtualThreadExecutor() {
        LOG.info("创建 SubWorkflowList 虚拟线程执行器 - 支持无限并发循环调用工作流");
        return newVirtualExecutor("vt-sub-workflow-list");
    }

    /**
     * SubWorkflow 虚拟线程执行器
     * 特别适合高并发循环调用工作流场景（批量创建工作流运行结果）
     */
    @Bean(name = "subWorkflowExecutorService", destroyMethod = "shutdown")
    @Primary
    public ExecutorService subWorkflowVirtualThreadExecutor() {
        LOG.info("创建 SubWorkflow 虚拟线程执行器 - 支持无限并发工作流调用");
        return newVirtualExecutor("vt-sub-workflow");
    }

    /**
     * SqlQuery 虚拟线程执行器
     * 特别适合高并发SQL查询场景（数据库连接和查询操作）
     */
    @Bean(name = "sqlQueryExecutorService", destroyMethod = "shutdown")
    @Primary
    public ExecutorService sqlQueryVirtualThreadExecutor() {
        LOG.info("创建 SqlQuery 虚拟线程执行器 - 支持无限并发SQL查询");
        return newVirtualExecutor("vt-sql-query");
    }

    /**
     * 通用消费者虚拟线程执行器
     */
    @Bean(name = "consumerExecutorService", destroyMethod = "shutdown")
    @Primary
    public ExecutorService consumerVirtualThreadExecutor() {
        LOG.info("创建通用消费者虚拟线程执行器");
        return newVirtualExecutor("vt-consumer");
    }

    /**
     * WorkflowRunInMemory 节点并发执行器
     */
    @Bean(name = "nodeExecutorService", destroyMethod = "shutdown")
    @Primary
    public ExecutorService nodeVirtualThreadExecutor() {
        LOG.info("创建 WorkflowRunInMemory 节点并发虚拟线程执行器");
        return newVirtualExecutor("vt-workflow-node");
    }
}
