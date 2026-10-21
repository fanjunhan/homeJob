package com.example.service.saas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 消息通知日志：支撑「通知必达」——失败重试与短信兜底 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "message_log")
public class MessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tenantId;

    /** wecom_app 企微应用消息 / miniapp_sub 小程序订阅消息 / sms 短信 */
    private String msgType;

    /** staff 员工 / customer 客户 */
    private String receiverType;

    private Long receiverId;

    private Long scheduleId;

    private String title;

    private String content;

    /** pending 待发送 / success 成功 / fail 失败 */
    private String sendStatus;

    private Integer retryCount;

    private String errMsg;

    private LocalDateTime sentAt;

    private LocalDateTime createdAt;
}
