package com.example.service.saas.client;

import com.example.service.saas.entity.ServiceSchedule;

/**
 * 企业微信消息客户端：面向员工的应用消息推送。
 *
 * <p>返回 boolean 而非 void，是为了让 {@code NotificationService} 判断是否需要落
 * message_log 重试与短信兜底：true=已成功投递（或按业务规则跳过），false=调用失败需重试。
 */
public interface WeComMessageClient {

    /**
     * 排班通知：服务日期、时间、时长、地址。
     *
     * @return true 已投递；false 失败，调用方应重试
     */
    boolean sendScheduleNotice(ServiceSchedule schedule);

    /**
     * 改派通知：告知新员工接单，并通知原员工工单已转出。
     *
     * @param oldStaffId 原员工 ID，为空表示无原员工（新建排班）
     * @return true 已投递；false 失败，调用方应重试
     */
    boolean sendReassignNotice(ServiceSchedule schedule, Long oldStaffId);
}