package com.example.service.saas.service;

import com.example.service.saas.common.BizException;
import com.example.service.saas.entity.*;
import com.example.service.saas.repository.ClockRecordRepository;
import com.example.service.saas.repository.PlanItemRepository;
import com.example.service.saas.repository.ScheduleRepository;
import com.example.service.saas.repository.ServiceEvaluationRepository;
import com.example.service.saas.repository.ServiceSignatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * 排班调度核心服务：排班 -> 接单 -> 双打卡 -> 签名 -> 评价。
 *
 * 依赖说明：
 *  - ScheduleRepository / PlanItemRepository / ClockRecordRepository 等为 Spring Data JPA 接口；
 *  - NotificationService：消息中心（企微应用消息 / 小程序订阅消息，含失败重试与短信兜底）；
 *  - BizException：统一业务异常，由 @ControllerAdvice 转换为错误码响应。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final PlanItemRepository planItemRepository;
    private final ClockRecordRepository clockRecordRepository;
    private final ServiceSignatureRepository signatureRepository;
    private final ServiceEvaluationRepository evaluationRepository;
    private final NotificationService notificationService;

    /**
     * 店长排班：必须在服务日前一天完成；成功后自动推送企微通知给员工。
     */
    @Transactional
    public ServiceSchedule createSchedule(Long tenantId, Long customerId, Long staffId,
                                          LocalDate serviceDate, LocalTime plannedStart,
                                          int durationMin, Long operatorId) {
        // 1. 业务规则：服务日前一天（含）之后不可再排班
        if (serviceDate.minusDays(1).isBefore(LocalDate.now())) {
            throw new BizException("排班须在服务日前一天完成");
        }
        // 2. 员工当日负载校验（示例：未完工工单不超过 3 单）
        long load = scheduleRepository.countByStaffIdAndServiceDateAndStatusIn(
                staffId, serviceDate, ScheduleStatus.activeStatuses());
        if (load >= 3) {
            throw new BizException("该员工当日排班已满，请更换员工");
        }
        // 3. 创建工单（排班与通知一气呵成，状态直接到「已通知」）
        ServiceSchedule schedule = ServiceSchedule.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .staffId(staffId)
                .serviceDate(serviceDate)
                .plannedStart(plannedStart.toString())
                .plannedEnd(plannedStart.plusMinutes(durationMin).toString())
                .durationMin(durationMin)
                .status(ScheduleStatus.NOTIFIED)
                .createdBy(operatorId)
                .build();
        scheduleRepository.save(schedule);
        // 4. 回写月度计划明细状态：待排班 -> 已排班
        planItemRepository.findByCustomerIdAndServiceDate(customerId, serviceDate)
                .ifPresent(pi -> {
                    pi.setStatus("scheduled");
                    planItemRepository.save(pi);
                });
        // 5. 推送企业微信应用消息（落库 + 失败重试，见 NotificationService）
        notificationService.sendScheduleNotice(schedule);
        return schedule;
    }

    /** 员工接单：已通知 -> 已接单 */
    @Transactional
    public void accept(Long scheduleId, Long staffId) {
        ServiceSchedule s = getAndCheckStaff(scheduleId, staffId);
        s.transit(ScheduleStatus.ACCEPTED);
        scheduleRepository.save(s);
    }

    /**
     * 打卡：in = 上门打卡（进入服务中），out = 完成打卡（记录实际时长）。
     * 位置信息来自企业微信 JSSDK getLocation。
     */
    @Transactional
    public ClockRecord clock(Long scheduleId, Long staffId, ClockRecord.ClockType type,
                             Double lat, Double lng, String locationName, String photosJson) {
        ServiceSchedule s = getAndCheckStaff(scheduleId, staffId);
        ClockRecord record = ClockRecord.builder()
                .scheduleId(scheduleId)
                .tenantId(s.getTenantId())
                .staffId(staffId)
                .clockType(type.name())
                .clockTime(LocalDateTime.now())
                .latitude(lat)
                .longitude(lng)
                .locationName(locationName)
                .photos(photosJson)
                .source("jssdk")
                .build();
        clockRecordRepository.save(record);
        if (type == ClockRecord.ClockType.IN) {
            s.transit(ScheduleStatus.IN_SERVICE);
            s.setActualStart(record.getClockTime());
        } else {
            s.transit(ScheduleStatus.FINISHED);
            s.setActualEnd(record.getClockTime());
            if (s.getActualStart() != null) {
                s.setActualDuration((int) Duration.between(s.getActualStart(), s.getActualEnd()).toMinutes());
            }
            // 完成打卡后推送小程序订阅消息，引导客户签名 + 评价
            notificationService.sendServiceFinishNotice(s);
        }
        scheduleRepository.save(s);
        return record;
    }

    /** 改派：已排班/已通知/已接单 -> 已改派 -> 重新排班 */
    @Transactional
    public void reassign(Long scheduleId, Long newStaffId, Long operatorId) {
        ServiceSchedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BizException("工单不存在"));
        if (!Set.of(ScheduleStatus.SCHEDULED, ScheduleStatus.NOTIFIED, ScheduleStatus.ACCEPTED)
                .contains(s.getStatus())) {
            throw new BizException("当前状态不可改派");
        }
        s.transit(ScheduleStatus.REASSIGNED);
        s.setReassignedFrom(s.getStaffId());
        scheduleRepository.save(s);
        // 重新走排班流程（通知新员工）
        s.transit(ScheduleStatus.SCHEDULED);
        s.setStaffId(newStaffId);
        scheduleRepository.save(s);
        notificationService.sendReassignNotice(s, s.getReassignedFrom());
    }

    /** 客户电子签名：已完成 -> 已签名 */
    @Transactional
    public void sign(Long scheduleId, Long customerId, String signImage, String clientIp) {
        ServiceSchedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BizException("工单不存在"));
        if (!s.getCustomerId().equals(customerId)) {
            throw new BizException("无权操作该工单");
        }
        signatureRepository.save(ServiceSignature.builder()
                .scheduleId(scheduleId)
                .tenantId(s.getTenantId())
                .customerId(customerId)
                .signImage(signImage)
                .signedAt(LocalDateTime.now())
                .clientIp(clientIp)
                .build());
        s.transit(ScheduleStatus.SIGNED);
        scheduleRepository.save(s);
    }

    /** 客户评价：已签名 -> 已评价（终态，工单关闭） */
    @Transactional
    public void evaluate(Long scheduleId, Long customerId, int score, String tagsJson, String comment) {
        ServiceSchedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BizException("工单不存在"));
        if (!s.getCustomerId().equals(customerId)) {
            throw new BizException("无权操作该工单");
        }
        if (score < 1 || score > 5) {
            throw new BizException("评分须为 1-5 星");
        }
        evaluationRepository.save(ServiceEvaluation.builder()
                .scheduleId(scheduleId)
                .tenantId(s.getTenantId())
                .customerId(customerId)
                .staffId(s.getStaffId())
                .score(score)
                .tags(tagsJson)
                .comment(comment)
                .build());
        s.transit(ScheduleStatus.EVALUATED);
        scheduleRepository.save(s);
    }

    private ServiceSchedule getAndCheckStaff(Long scheduleId, Long staffId) {
        ServiceSchedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BizException("工单不存在"));
        if (!s.getStaffId().equals(staffId)) {
            throw new BizException("无权操作该工单");
        }
        return s;
    }

    /** 取消：已排班/已通知/已接单 -> 已取消，同步释放计划日期 */
    @Transactional
    public void cancel(Long scheduleId, String reason, Long operatorId) {
        ServiceSchedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BizException("工单不存在"));
        if (!Set.of(ScheduleStatus.SCHEDULED, ScheduleStatus.NOTIFIED, ScheduleStatus.ACCEPTED)
                .contains(s.getStatus())) {
            throw new BizException("当前状态不可取消");
        }
        s.transit(ScheduleStatus.CANCELLED);
        s.setCancelReason(reason);
        scheduleRepository.save(s);
        if (s.getPlanItemId() != null) {
            planItemRepository.findById(s.getPlanItemId()).ifPresent(pi -> {
                pi.setStatus("pending");
                planItemRepository.save(pi);
            });
        }
    }

    /** 店长端：查询排班（按日期或按员工） */
    public List<ServiceSchedule> query(Long tenantId, LocalDate date, Long staffId) {
        if (staffId != null) {
            return scheduleRepository.findByStaffIdAndServiceDateGreaterThanEqual(
                    staffId, date != null ? date : LocalDate.now());
        }
        return scheduleRepository.findByTenantIdAndServiceDate(tenantId,
                date != null ? date : LocalDate.now());
    }

    /** 员工端：我的排班 */
    public List<ServiceSchedule> mySchedules(Long staffId) {
        return scheduleRepository.findByStaffIdAndServiceDateGreaterThanEqual(staffId, LocalDate.now());
    }

    /** 打卡记录（按时间正序） */
    public List<ClockRecord> clockRecords(Long scheduleId) {
        return clockRecordRepository.findByScheduleIdOrderByIdAsc(scheduleId);
    }
}
