package com.example.service.saas.client;

import cn.binaryflyer.wxjava.miniapp.api.WxMaService;
import cn.binaryflyer.wxjava.miniapp.bean.subscribe.WxMaSubscribeMessage;
import com.example.service.saas.config.IntegrationProperties;
import com.example.service.saas.entity.Customer;
import com.example.service.saas.entity.ServiceSchedule;
import com.example.service.saas.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 WxJava（weixin-java-miniapp）的小程序订阅消息客户端。
 *
 * <p>订阅消息为一次性订阅：客户进入小程序后需先 requestSubscribeMessage 申请推送权限，
 * 建议在「服务详情页」引导授权；有资质后可申请长期订阅模板。
 *
 * <p>失败处理：客户未授权订阅消息时发送会失败，返回 false 交给
 * {@code NotificationService} 重试，超阈值降级为短信通知。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxJavaMiniProgramMessageClient implements MiniProgramMessageClient {

    private final WxMaService wxMaService;
    private final CustomerRepository customerRepository;
    private final IntegrationProperties props;

    @Override
    public boolean sendServiceFinishNotice(ServiceSchedule schedule) {
        Customer customer = customerRepository.findById(schedule.getCustomerId()).orElse(null);
        if (customer == null || customer.getOpenid() == null) {
            log.warn("订阅消息跳过：客户未绑定 openid, scheduleId={}", schedule.getId());
            return true; // 无 openid 无法再投递，重试无意义
        }
        WxMaSubscribeMessage message = WxMaSubscribeMessage.builder()
                .templateId(props.getMiniapp().getTemplateIdServiceFinish())
                .toUser(customer.getOpenid())
                .page("pages/service/service?id=" + schedule.getId())
                .data(List.of(
                        new WxMaSubscribeMessage.MsgData("thing1", "您的上门服务已完成"),
                        new WxMaSubscribeMessage.MsgData("time2", String.valueOf(schedule.getServiceDate())),
                        new WxMaSubscribeMessage.MsgData("thing3", "点击查看详情，完成电子签名与评价")))
                .build();
        try {
            wxMaService.getSubscribeMessageService().sendSubscribeMsg(message);
            log.info("小程序订阅消息发送成功, openid={}", customer.getOpenid());
            return true;
        } catch (Exception e) {
            log.error("小程序订阅消息发送失败, openid={}", customer.getOpenid(), e);
            return false;
        }
    }
}