package com.example.service.saas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 企业微信上门服务 SaaS —— 启动类。
 *
 * <p>关键注解说明：
 * <ul>
 *   <li>{@code @EnableAsync}：企微回调事件（通讯录同步）与消息重试均异步执行，
 *       保证在企微 5 秒超时前返回 success</li>
 *   <li>{@code @EnableScheduling}：开启 {@code NotificationRetryJob} 每分钟扫描失败消息重试</li>
 *   <li>{@code @EnableJpaAuditing}：实体 createdAt 自动填充</li>
 * </ul>
 */
@EnableAsync
@EnableScheduling
@EnableJpaAuditing
@SpringBootApplication
public class ServiceCloudSaasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceCloudSaasApplication.class, args);
    }
}