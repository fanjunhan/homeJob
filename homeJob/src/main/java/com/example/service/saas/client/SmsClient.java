package com.example.service.saas.client;

/** 短信网关：企微消息多次重试仍失败时的兜底通道 */
public interface SmsClient {

    /**
     * @param phone   接收手机号
     * @param content 短信内容
     */
    void send(String phone, String content);
}
