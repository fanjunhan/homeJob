package com.example.service.saas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 电子签名（客户小程序手写确认） */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service_signature")
public class ServiceSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long scheduleId;

    private Long tenantId;

    private Long customerId;

    /** 签名图片 OSS 地址 */
    private String signImage;

    private LocalDateTime signedAt;

    private String clientIp;

    private LocalDateTime createdAt;
}
