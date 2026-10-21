package com.example.service.saas.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信网关占位实现：仅打印日志。
 * 生产环境替换为阿里云短信 / 腾讯云短信 SDK 实现。
 */
@Slf4j
@Component
public class LogSmsClient implements SmsClient {

    @Override
    public void send(String phone, String content) {
        log.warn("[短信兜位-待接入真实网关] phone={} content={}", phone, content);
    }
}
