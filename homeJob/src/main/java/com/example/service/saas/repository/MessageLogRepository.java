package com.example.service.saas.repository;

import com.example.service.saas.entity.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {

    /** 待重试的消息：发送失败且重试次数未超限 */
    List<MessageLog> findBySendStatusAndRetryCountLessThan(String sendStatus, Integer retryCount);

    /** 长时间未完成的待发送消息（用于兜底扫描） */
    List<MessageLog> findBySendStatusAndCreatedAtBefore(String sendStatus, LocalDateTime before);
}
