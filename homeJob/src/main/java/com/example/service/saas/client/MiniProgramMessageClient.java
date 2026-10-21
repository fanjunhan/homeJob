package com.example.service.saas.client;

import com.example.service.saas.entity.ServiceSchedule;

/**
 * 微信小程序消息客户端：面向客户的订阅消息推送。
 *
 * <p>订阅消息为一次性资源，客户未授权时发送会失败，调用方需据返回值做重试/兜底。
 */
public interface MiniProgramMessageClient {

    /**
     * 服务完成提醒：员工完成打卡后触发，引导客户签名 + 评价。
     *
     * @return true 已投递；false 失败或客户未授权订阅消息
     */
    boolean sendServiceFinishNotice(ServiceSchedule schedule);
}