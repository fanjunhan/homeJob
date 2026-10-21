package com.example.service.saas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 打卡记录：员工上门 / 完成双打卡 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clock_record")
public class ClockRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long scheduleId;

    private Long tenantId;

    private Long staffId;

    /** in 上门打卡 / out 完成打卡 */
    private String clockType;

    private LocalDateTime clockTime;

    /** 定位地址描述（企微 JSSDK getLocation 返回） */
    private String locationName;

    private Double latitude;

    private Double longitude;

    /** 现场照片 URL 数组（JSON） */
    private String photos;

    /** jssdk 企微JSSDK / wecom 企微打卡 */
    private String source;

    private LocalDateTime createdAt;

    public enum ClockType {
        IN, OUT
    }
}
