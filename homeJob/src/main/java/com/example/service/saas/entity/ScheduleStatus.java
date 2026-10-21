package com.example.service.saas.entity;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 服务工单状态机。
 * 状态流转规则集中定义在这里，Service 层通过 check() 强校验，
 * 非法流转直接抛异常，保证业务闭环不被绕过。
 */
public enum ScheduleStatus {

    SCHEDULED,   // 已排班
    NOTIFIED,    // 已通知（企微消息已推送）
    ACCEPTED,    // 已接单
    IN_SERVICE,  // 服务中（上门打卡后）
    FINISHED,    // 已完成（完成打卡后）
    SIGNED,      // 已签名（客户电子签名后）
    EVALUATED,   // 已评价（客户打分后，终态）
    CANCELLED,   // 已取消（终态）
    REASSIGNED;  // 已改派（中间态，随后重新回到 SCHEDULED）

    private static final Map<ScheduleStatus, Set<ScheduleStatus>> ALLOWED = new EnumMap<>(ScheduleStatus.class);

    static {
        ALLOWED.put(SCHEDULED,   Set.of(NOTIFIED, CANCELLED, REASSIGNED));
        ALLOWED.put(NOTIFIED,    Set.of(ACCEPTED, CANCELLED));
        ALLOWED.put(ACCEPTED,    Set.of(IN_SERVICE, CANCELLED));
        ALLOWED.put(IN_SERVICE,  Set.of(FINISHED));
        ALLOWED.put(FINISHED,    Set.of(SIGNED));
        ALLOWED.put(SIGNED,      Set.of(EVALUATED));
        ALLOWED.put(EVALUATED,   Set.of());
        ALLOWED.put(CANCELLED,   Set.of());
        ALLOWED.put(REASSIGNED,  Set.of(SCHEDULED));
    }

    public static void check(ScheduleStatus from, ScheduleStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalStateException("非法状态流转: " + from + " -> " + to);
        }
    }

    /** 未完工状态，用于员工当日负载统计 */
    public static Set<ScheduleStatus> activeStatuses() {
        return Set.of(SCHEDULED, NOTIFIED, ACCEPTED, IN_SERVICE);
    }
}
