package com.gemantic.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * OkHttp 连接池配置
 * 针对高并发场景优化，支持3000节点工作流同时运行10次
 */
@Configuration
public class OkHttpConfig {

    private static final Logger LOG = LoggerFactory.getLogger(OkHttpConfig.class);

    /**
     * 连接池最大空闲连接数
     * 对于10次并发运行3000节点（含大量ApiRequest），需要较大连接池
     */
    @Value("${okhttp.pool.maxIdleConnections:500}")
    private Integer maxIdleConnections;

    /**
     * 连接保持活跃时间（秒）
     */
    @Value("${okhttp.pool.keepAliveDuration:300}")
    private Integer keepAliveDuration;

    /**
     * 连接超时（秒）
     */
    @Value("${okhttp.connectTimeout:30}")
    private Integer connectTimeout;

    /**
     * 读取超时（秒）
     */
    @Value("${okhttp.readTimeout:600}")
    private Integer readTimeout;

    /**
     * 写入超时（秒）
     */
    @Value("${okhttp.writeTimeout:600}")
    private Integer writeTimeout;

    /**
     * 调用超时（秒）- 整个请求的超时时间
     */
    @Value("${okhttp.callTimeout:1200}")
    private Integer callTimeout;

    @Bean
    public ConnectionPool connectionPool() {
        ConnectionPool pool = new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.SECONDS);
        LOG.info("创建 OkHttp 连接池: maxIdleConnections={}, keepAliveDuration={}s", 
                maxIdleConnections, keepAliveDuration);
        return pool;
    }

    @Bean
    public OkHttpClient okHttpClient(ConnectionPool connectionPool) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .callTimeout(callTimeout, TimeUnit.SECONDS)
                // 允许重试
                .retryOnConnectionFailure(true)
                .build();
        
        LOG.info("创建 OkHttpClient: connectTimeout={}s, readTimeout={}s, writeTimeout={}s, callTimeout={}s",
                connectTimeout, readTimeout, writeTimeout, callTimeout);
        return client;
    }
}







