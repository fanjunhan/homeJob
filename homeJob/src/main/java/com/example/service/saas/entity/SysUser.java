package com.example.service.saas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 系统用户（店长 / 员工），与企业微信 userid 绑定 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sys_user")
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private String name;

    private String phone;

    /** manager 店长 / staff 员工 */
    private String role;

    /** 企业微信 userid，通讯录同步回写 */
    private String wecomUserid;

    private String avatar;

    /** 班组：保洁组/清洗组/综合组 */
    private String team;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
