package com.gemantic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 配置bean
 *
 * @author yhye 2016年9月20日下午4:55:17
 */
@SpringBootApplication
@EnableAsync
@EnableFeignClients
@EnableScheduling
public class Server {

  public static void main(String[] args) {

    SpringApplication.run(Server.class,args);
  }
}
