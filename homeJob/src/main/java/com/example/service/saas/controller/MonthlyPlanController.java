package com.example.service.saas.controller;

import com.example.service.saas.common.ApiResponse;
import com.example.service.saas.common.TenantContext;
import com.example.service.saas.entity.PlanItem;
import com.example.service.saas.service.MonthlyPlanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** 月度服务计划（店长端）：制定客户每月上门服务日期 */
@RestController
@RequestMapping("/api/v1/monthly-plans")
@RequiredArgsConstructor
public class MonthlyPlanController {

    private final MonthlyPlanService planService;

    /** 跨客户日历：某月全部计划日期（计划页日历数据源） */
    @GetMapping
    public ApiResponse<List<PlanItem>> monthItems(@RequestParam @NotBlank String month) {
        return ApiResponse.ok(planService.monthItems(TenantContext.tenantId(), month));
    }

    /** 客户维度：某月计划（含每个日期的排班状态） */
    @GetMapping("/customer/{customerId}")
    public ApiResponse<MonthlyPlanService.PlanVO> byCustomer(@PathVariable Long customerId,
                                                             @RequestParam @NotBlank String month) {
        return ApiResponse.ok(planService.getByCustomer(TenantContext.tenantId(), customerId, month));
    }

    /** 制定/更新月度计划（全量覆盖当月待排班日期） */
    @PostMapping
    public ApiResponse<Void> save(@Valid @RequestBody PlanForm form) {
        planService.savePlan(TenantContext.tenantId(), form.customerId(), form.planMonth(),
                form.dates(), TenantContext.userId());
        return ApiResponse.ok();
    }

    public record PlanForm(
            @NotNull(message = "客户不能为空") Long customerId,
            @NotBlank(message = "月份不能为空")
            @Pattern(regexp = "\\d{4}-\\d{2}", message = "月份格式应为 yyyy-MM") String planMonth,
            @NotEmpty(message = "至少需要一个服务日期") List<LocalDate> dates) {
    }
}
