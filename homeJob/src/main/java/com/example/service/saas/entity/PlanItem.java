package com.example.service.saas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 月度计划明细：当月每一个计划上门日期 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "plan_item")
public class PlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long planId;

    private Long tenantId;

    private LocalDate serviceDate;

    /** pending 待排班 / scheduled 已排班 / done 已完成 / cancelled 已取消 */
    private String status;

    private LocalDateTime createdAt;
}
