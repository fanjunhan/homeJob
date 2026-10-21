package com.example.service.saas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 服务工单（排班结果）——系统核心单据。
 * 一次排班生成一张工单，贯穿 打卡 -> 签名 -> 评价 全流程。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service_schedule")
public class ServiceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tenantId;

    /** 来源月度计划明细；临时加单可为空 */
    private Long planItemId;

    private Long customerId;

    /** 服务员工 */
    private Long staffId;

    private LocalDate serviceDate;

    /** 计划开始时间，如 09:00 */
    private String plannedStart;

    /** 计划结束时间，如 11:00 */
    private String plannedEnd;

    /** 计划时长（分钟） */
    private Integer durationMin;

    @Enumerated(EnumType.STRING)
    private ScheduleStatus status;

    /** 上门打卡时间 */
    private LocalDateTime actualStart;

    /** 完成打卡时间 */
    private LocalDateTime actualEnd;

    /** 实际时长（分钟），由系统根据两次打卡计算 */
    private Integer actualDuration;

    private String cancelReason;

    /** 改派前员工 */
    private Long reassignedFrom;

    private String remark;

    @Version
    private Integer version;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 状态流转（强校验） */
    public void transit(ScheduleStatus next) {
        ScheduleStatus.check(this.status, next);
        this.status = next;
    }
}
