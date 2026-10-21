package com.example.service.saas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 客户档案（店长维护） */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private String name;

    /** 手机号：小程序登录后据此匹配客户身份 */
    private String phone;

    /** 微信小程序 openid（登录后绑定，用于订阅消息推送） */
    private String openid;

    private Integer gender;

    /** 服务地址 */
    private String address;

    /** 默认服务项目 */
    private Long serviceItemId;

    /** 服务周期描述，如 每周一次 */
    private String cycleDesc;

    private LocalDate contractStart;

    private LocalDate contractEnd;

    private String remark;

    /** 专属服务人员 */
    private Long ownerStaffId;

    private Integer status;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
