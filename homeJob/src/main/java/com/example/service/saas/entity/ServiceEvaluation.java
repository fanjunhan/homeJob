package com.example.service.saas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 服务评价：tags 为客户勾选的服务项目（长护险服务项目目录），最多 5 项 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service_evaluation")
public class ServiceEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long scheduleId;

    private Long tenantId;

    private Long customerId;

    private Long staffId;

    /** 评分 1-5 星 */
    private Integer score;

    /** 勾选的服务项目 JSON 数组，最多 5 项 */
    private String tags;

    private String comment;

    private LocalDateTime createdAt;
}
