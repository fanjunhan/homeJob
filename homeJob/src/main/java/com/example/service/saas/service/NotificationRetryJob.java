package com.example.service.saas.service;

import com.example.service.saas.entity.MessageLog;
import com.example.service.saas.repository.MessageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** 消息重试任务：扫描失败消息重发，超过上限由 NotificationService 升级短信 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryJob {

    private final MessageLogRepository messageLogRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void retryFailedMessages() {
        List<MessageLog> list = messageLogRepository
                .findBySendStatusAndRetryCountLessThan("fail", NotificationService.MAX_RETRY);
        if (list.isEmpty()) {
            return;
        }
        log.info("待重试消息数: {}", list.size());
        for (MessageLog msg : list) {
            try {
                notificationService.retry(msg);
            } catch (Exception e) {
                log.error("消息重试异常, messageLogId={}", msg.getId(), e);
            }
        }
    }
}
