package com.example.service.saas.controller;

import com.example.service.saas.common.ApiResponse;
import com.example.service.saas.common.BizException;
import com.example.service.saas.common.TenantContext;
import com.example.service.saas.entity.ClockRecord;
import com.example.service.saas.entity.ServiceSchedule;
import com.example.service.saas.service.ScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 排班调度与打卡（店长端 + 员工端）。
 * 员工身份说明：骨架用 TenantContext.userId() 作为 staffId（JWT subject 即员工用户ID）。
 */
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /** 排班查询：按日期（店长）或按员工（员工端我的排班） */
    @GetMapping
    public ApiResponse<List<ServiceSchedule>> query(@RequestParam(required = false) LocalDate date,
                                                    @RequestParam(required = false) Long staffId) {
        if (staffId != null) {
            return ApiResponse.ok(scheduleService.mySchedules(staffId));
        }
        return ApiResponse.ok(scheduleService.query(TenantContext.tenantId(), date, null));
    }

    /** 创建排班：服务日前一天完成，成功后自动推送企微通知 */
    @PostMapping
    public ApiResponse<ServiceSchedule> create(@Valid @RequestBody ScheduleForm form) {
        ServiceSchedule schedule = scheduleService.createSchedule(
                TenantContext.tenantId(),
                form.customerId(),
                form.staffId(),
                form.serviceDate(),
                form.plannedStart(),
                form.durationMin(),
                TenantContext.userId());
        return ApiResponse.ok(schedule);
    }

    /** 员工接单 */
    @PostMapping("/{id}/accept")
    public ApiResponse<Void> accept(@PathVariable Long id) {
        scheduleService.accept(id, TenantContext.userId());
        return ApiResponse.ok();
    }

    /** 改派 */
    @PostMapping("/{id}/reassign")
    public ApiResponse<Void> reassign(@PathVariable Long id, @Valid @RequestBody ReassignForm form) {
        scheduleService.reassign(id, form.staffId(), TenantContext.userId());
        return ApiResponse.ok();
    }

    /** 取消 */
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id, @Valid @RequestBody CancelForm form) {
        scheduleService.cancel(id, form.reason(), TenantContext.userId());
        return ApiResponse.ok();
    }

    /** 打卡：in=上门打卡 out=完成打卡（企微 JSSDK 定位） */
    @PostMapping("/{id}/clock")
    public ApiResponse<ClockRecord> clock(@PathVariable Long id, @Valid @RequestBody ClockForm form) {
        ClockRecord.ClockType type;
        try {
            type = ClockRecord.ClockType.valueOf(form.type());
        } catch (IllegalArgumentException e) {
            throw new BizException("打卡类型须为 in 或 out");
        }
        ClockRecord record = scheduleService.clock(id, TenantContext.userId(), type,
                form.latitude(), form.longitude(), form.locationName(),
                form.photos() == null ? null : String.join(",", form.photos()));
        return ApiResponse.ok(record);
    }

    /** 打卡记录 */
    @GetMapping("/{id}/clock-records")
    public ApiResponse<List<ClockRecord>> clockRecords(@PathVariable Long id) {
        return ApiResponse.ok(scheduleService.clockRecords(id));
    }

    /** 客户电子签名（员工端代客户提交时使用；小程序端走 /miniapp 接口） */
    @PostMapping("/{id}/signature")
    public ApiResponse<Void> signature(@PathVariable Long id, @Valid @RequestBody SignatureForm form) {
        scheduleService.sign(id, TenantContext.customerId(), form.signImage(), null);
        return ApiResponse.ok();
    }

    public record ScheduleForm(
            @NotNull(message = "客户不能为空") Long customerId,
            @NotNull(message = "员工不能为空") Long staffId,
            @NotNull(message = "服务日期不能为空") LocalDate serviceDate,
            @NotNull(message = "上门时间不能为空") LocalTime plannedStart,
            @NotNull(message = "服务时长不能为空") @Positive(message = "服务时长须大于 0") Integer durationMin,
            Long planItemId) {
    }

    public record ReassignForm(
            @NotNull(message = "新员工不能为空") Long staffId) {
    }

    public record CancelForm(
            @NotBlank(message = "取消原因不能为空") String reason) {
    }

    public record ClockForm(
            @NotBlank(message = "打卡类型不能为空") String type,
            Double latitude,
            Double longitude,
            String locationName,
            List<String> photos) {
    }

    public record SignatureForm(
            @NotBlank(message = "签名图片不能为空") String signImage) {
    }
}
