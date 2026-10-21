package com.example.service.saas.client;

import cn.binaryflyer.wxjava.cp.api.WxCpService;
import cn.binaryflyer.wxjava.cp.bean.message.WxCpMessage;
import cn.binaryflyer.wxjava.cp.bean.message.WxCpMessageText;
import cn.binaryflyer.wxjava.cp.constants.WxCpConsts;
import com.example.service.saas.config.IntegrationProperties;
import com.example.service.saas.entity.Customer;
import com.example.service.saas.entity.ServiceSchedule;
import com.example.service.saas.entity.SysUser;
import com.example.service.saas.repository.CustomerRepository;
import com.example.service.saas.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 基于 WxJava（weixin-java-cp）的企业微信应用消息客户端。
 *
 * <p>多租户说明：单 corp 演示场景直接注入 WxCpService；
 * SaaS 多租户请改用 WxCpMultiClients，按 tenant.corpid 动态获取 WxCpService，
 * 服务商模式配合 suiteId + suiteSecret 换取企业 access_token。
 *
 * <p>失败处理：返回 false，由 {@code NotificationService} 落 message_log 并重试，
 * 超过阈值（3 次）升级为短信提醒（见设计文档规则 R4）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxJavaWeComMessageClient implements WeComMessageClient {

    private final WxCpService wxCpService;
    private final SysUserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final IntegrationProperties props;

    @Override
    public boolean sendScheduleNotice(ServiceSchedule schedule) {
        SysUser staff = userRepository.findById(schedule.getStaffId()).orElse(null);
        Customer customer = customerRepository.findById(schedule.getCustomerId()).orElse(null);
        if (staff == null || customer == null || staff.getWecomUserid() == null) {
            log.warn("排班通知跳过：员工未绑定企微或客户不存在, scheduleId={}", schedule.getId());
            return true; // 数据缺失属业务问题，重试无意义，视为已处理避免死循环重试
        }
        String content = String.format(
                "【新排班通知】%s %s-%s，服务时长 %d 分钟%n客户：%s%n地址：%s%n请按时上门，到达后点击「上门打卡」。",
                schedule.getServiceDate(),
                schedule.getPlannedStart(),
                schedule.getPlannedEnd(),
                schedule.getDurationMin(),
                customer.getName(),
                customer.getAddress());
        return sendText(staff.getWecomUserid(), content);
    }

    @Override
    public boolean sendReassignNotice(ServiceSchedule schedule, Long oldStaffId) {
        Customer customer = customerRepository.findById(schedule.getCustomerId()).orElse(null);
        SysUser newStaff = userRepository.findById(schedule.getStaffId()).orElse(null);

        boolean ok = true;
        if (newStaff != null && newStaff.getWecomUserid() != null && customer != null) {
            ok &= sendText(newStaff.getWecomUserid(), String.format(
                    "【改派通知】%s %s-%s 的工单已改派给您%n客户：%s%n地址：%s",
                    schedule.getServiceDate(), schedule.getPlannedStart(),
                    schedule.getPlannedEnd(), customer.getName(), customer.getAddress()));
        }
        if (oldStaffId != null) {
            SysUser oldStaff = userRepository.findById(oldStaffId).orElse(null);
            if (oldStaff != null && oldStaff.getWecomUserid() != null) {
                ok &= sendText(oldStaff.getWecomUserid(), String.format(
                        "【工单转出】%s %s 的工单已改派给其他员工，您的排班已更新",
                        schedule.getServiceDate(), schedule.getPlannedStart()));
            }
        }
        return ok;
    }

    private boolean sendText(String toUser, String content) {
        try {
            WxCpMessage message = new WxCpMessage();
            message.setToUser(toUser);
            message.setAgentId(props.getWecom().getAgentId());
            message.setMsgType(WxCpConsts.MsgType.TEXT);
            WxCpMessageText text = new WxCpMessageText();
            text.setContent(content);
            message.setText(text);
            wxCpService.getMessageService().send(message);
            log.info("企微应用消息发送成功, toUser={}", toUser);
            return true;
        } catch (Exception e) {
            // WxErrorException（access_token 失效、userid 不合法等）→ 返回 false 触发重试
            log.error("企微应用消息发送失败, toUser={}", toUser, e);
            return false;
        }
    }
}