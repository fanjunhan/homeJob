package com.example.service.saas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 月度服务计划：每客户每月一条 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "monthly_plan")
public class MonthlyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tenantId;

    private Long customerId;

    /** 计划月份 YYYY-MM */
    private String planMonth;

    /** active 生效 / closed 关闭 */
    private String status;

    private Long createdBy;

    private LocalDateTime createdAt;
}
