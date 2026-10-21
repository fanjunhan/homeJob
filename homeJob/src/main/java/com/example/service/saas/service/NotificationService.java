package com.example.service.saas.service;

import com.example.service.saas.client.MiniProgramMessageClient;
import com.example.service.saas.client.SmsClient;
import com.example.service.saas.client.WeComMessageClient;
import com.example.service.saas.entity.Customer;
import com.example.service.saas.entity.MessageLog;
import com.example.service.saas.entity.ServiceSchedule;
import com.example.service.saas.entity.SysUser;
import com.example.service.saas.repository.CustomerRepository;
import com.example.service.saas.repository.MessageLogRepository;
import com.example.service.saas.repository.ScheduleRepository;
import com.example.service.saas.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 消息中心：统一承接企微应用消息与小程序订阅消息。
 * 对应业务规则 R4「通知必达」：所有通知先落 message_log，
 * 发送失败由定时任务重试，超过上限自动升级为短信提醒。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /** 失败重试上限，超过后升级短信 */
    public static final int MAX_RETRY = 3;

    private final MessageLogRepository messageLogRepository;
    private final ScheduleRepository scheduleRepository;
    private final CustomerRepository customerRepository;
    private final SysUserRepository userRepository;
    private final WeComMessageClient weComMessageClient;
    private final MiniProgramMessageClient miniProgramMessageClient;
    private final SmsClient smsClient;

    /** 排班通知（企微应用消息） */
    public void sendScheduleNotice(ServiceSchedule schedule) {
        MessageLog msg = createLog(schedule, "wecom_app", "staff", schedule.getStaffId(),
                "新排班通知",
                schedule.getServiceDate() + " " + schedule.getPlannedStart() + " 起 " + schedule.getDurationMin() + " 分钟");
        updateStatus(msg, weComMessageClient.sendScheduleNotice(schedule));
    }

    /** 改派通知（企微应用消息） */
    public void sendReassignNotice(ServiceSchedule schedule, Long oldStaffId) {
        MessageLog msg = createLog(schedule, "wecom_app", "staff", schedule.getStaffId(),
                "改派通知",
                schedule.getServiceDate() + " " + schedule.getPlannedStart() + " 起，工单已改派");
        updateStatus(msg, weComMessageClient.sendReassignNotice(schedule, oldStaffId));
    }

    /** 服务完成提醒（小程序订阅消息） */
    public void sendServiceFinishNotice(ServiceSchedule schedule) {
        MessageLog msg = createLog(schedule, "miniapp_sub", "customer", schedule.getCustomerId(),
                "服务完成提醒", "服务已完成，请在小程序确认并评价");
        updateStatus(msg, miniProgramMessageClient.sendServiceFinishNotice(schedule));
    }

    /** 重试失败消息；达到上限后升级短信 */
    @Transactional
    public void retry(MessageLog msg) {
        boolean ok = resend(msg);
        msg.setRetryCount(msg.getRetryCount() + 1);
        if (ok) {
            msg.setSendStatus("success");
            msg.setSentAt(LocalDateTime.now());
            msg.setErrMsg(null);
        } else {
            msg.setSendStatus("fail");
            msg.setErrMsg("第 " + msg.getRetryCount() + " 次重试仍失败");
            if (msg.getRetryCount() >= MAX_RETRY) {
                sendSmsFallback(msg);
            }
        }
        messageLogRepository.save(msg);
    }

    private boolean resend(MessageLog msg) {
        if (msg.getScheduleId() == null) {
            return false;
        }
        Optional<ServiceSchedule> schedule = scheduleRepository.findById(msg.getScheduleId());
        if (schedule.isEmpty()) {
            return false;
        }
        return switch (msg.getMsgType()) {
            case "wecom_app" -> weComMessageClient.sendScheduleNotice(schedule.get());
            case "miniapp_sub" -> miniProgramMessageClient.sendServiceFinishNotice(schedule.get());
            default -> false;
        };
    }

    /** 短信兜底：员工取 sys_user 手机号，客户取 customer 手机号 */
    private void sendSmsFallback(MessageLog msg) {
        String phone = null;
        if ("staff".equals(msg.getReceiverType()) && msg.getReceiverId() != null) {
            phone = userRepository.findById(msg.getReceiverId()).map(SysUser::getPhone).orElse(null);
        } else if ("customer".equals(msg.getReceiverType()) && msg.getReceiverId() != null) {
            phone = customerRepository.findById(msg.getReceiverId()).map(Customer::getPhone).orElse(null);
        }
        if (phone == null || phone.isBlank()) {
            log.warn("短信兜底跳过：未找到接收人手机号, messageLogId={}", msg.getId());
            return;
        }
        smsClient.send(phone, msg.getTitle() + "：" + msg.getContent());
    }

    private MessageLog createLog(ServiceSchedule schedule, String msgType, String receiverType,
                                  Long receiverId, String title, String content) {
        MessageLog msg = MessageLog.builder()
                .tenantId(schedule.getTenantId())
                .msgType(msgType)
                .receiverType(receiverType)
                .receiverId(receiverId)
                .scheduleId(schedule.getId())
                .title(title)
                .content(content)
                .sendStatus("pending")
                .retryCount(0)
                .build();
        return messageLogRepository.save(msg);
    }

    private void updateStatus(MessageLog msg, boolean success) {
        msg.setSendStatus(success ? "success" : "fail");
        msg.setSentAt(success ? LocalDateTime.now() : null);
        if (!success) {
            msg.setErrMsg("首次发送失败，等待重试");
        }
        messageLogRepository.save(msg);
    }
}
