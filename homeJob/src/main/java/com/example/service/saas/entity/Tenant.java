package com.example.service.saas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 租户（入驻企业），企业微信侧通过 corpid 关联 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tenant")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String contactName;

    private String contactPhone;

    /** 企业微信 corpid */
    private String corpid;

    /** 企微服务商第三方应用 suite_id */
    private String suiteId;

    /** 企业永久授权码 */
    private String permanentCode;

    /** 订阅版本 basic/pro/enterprise */
    private String planType;

    private LocalDateTime expireAt;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
