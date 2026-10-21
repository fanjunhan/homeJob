package com.example.service.saas.service;

import com.example.service.saas.common.BizException;
import com.example.service.saas.entity.Customer;
import com.example.service.saas.entity.MonthlyPlan;
import com.example.service.saas.entity.PlanItem;
import com.example.service.saas.repository.CustomerRepository;
import com.example.service.saas.repository.MonthlyPlanRepository;
import com.example.service.saas.repository.PlanItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 月度服务计划（店长端）：为每个客户制定当月上门服务日期。
 * 保存策略为「全量覆盖」：已有排班/已完成的日期保留，其余待排班日期重建。
 */
@Service
@RequiredArgsConstructor
public class MonthlyPlanService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MonthlyPlanRepository planRepository;
    private final PlanItemRepository planItemRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public MonthlyPlan savePlan(Long tenantId, Long customerId, String planMonth,
                                List<LocalDate> dates, Long operatorId) {
        Customer customer = customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new BizException("客户不存在"));
        YearMonth ym;
        try {
            ym = YearMonth.parse(planMonth, MONTH_FMT);
        } catch (Exception e) {
            throw new BizException("月份格式应为 yyyy-MM");
        }
        if (dates == null || dates.isEmpty()) {
            throw new BizException("至少需要一个服务日期");
        }
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        dates.forEach(d -> {
            if (d.isBefore(monthStart) || d.isAfter(monthEnd)) {
                throw new BizException("服务日期须在 " + planMonth + " 月内");
            }
        });

        MonthlyPlan plan = planRepository.findByCustomerIdAndPlanMonth(customerId, planMonth)
                .orElseGet(() -> MonthlyPlan.builder()
                        .tenantId(tenantId)
                        .customerId(customerId)
                        .planMonth(planMonth)
                        .status("active")
                        .createdBy(operatorId)
                        .build());
        plan = planRepository.save(plan);

        List<PlanItem> exists = planItemRepository.findByPlanId(plan.getId());
        List<LocalDate> keepDates = new ArrayList<>();
        // 已排班 / 已完成的明细保留，不允许被覆盖删除
        for (PlanItem item : exists) {
            if ("scheduled".equals(item.getStatus()) || "done".equals(item.getStatus())) {
                keepDates.add(item.getServiceDate());
            } else {
                planItemRepository.delete(item);
            }
        }
        List<LocalDate> finalDates = new ArrayList<>(keepDates);
        for (LocalDate date : dates) {
            if (!finalDates.contains(date)) {
                finalDates.add(date);
            }
        }
        for (LocalDate date : finalDates) {
            planItemRepository.save(PlanItem.builder()
                    .planId(plan.getId())
                    .tenantId(tenantId)
                    .customerId(customerId)
                    .serviceDate(date)
                    .status("pending")
                    .build());
        }
        return plan;
    }

    /** 客户维度：某月计划（含每个日期状态） */
    public PlanVO getByCustomer(Long tenantId, Long customerId, String planMonth) {
        MonthlyPlan plan = planRepository.findByCustomerIdAndPlanMonth(customerId, planMonth)
                .orElse(null);
        List<PlanItem> items = plan == null ? List.of()
                : planItemRepository.findByPlanId(plan.getId()).stream()
                .sorted((a, b) -> a.getServiceDate().compareTo(b.getServiceDate()))
                .toList();
        return new PlanVO(plan, items);
    }

    /** 跨客户日历：某月全部计划日期（店长计划页） */
    public List<PlanItem> monthItems(Long tenantId, String planMonth) {
        YearMonth ym = YearMonth.parse(planMonth, MONTH_FMT);
        return planItemRepository.findByTenantIdAndServiceDateBetween(
                tenantId, ym.atDay(1), ym.atEndOfMonth());
    }

    public Optional<Customer> getCustomer(Long tenantId, Long customerId) {
        return customerRepository.findByIdAndTenantId(customerId, tenantId);
    }

    public record PlanVO(MonthlyPlan plan, List<PlanItem> items) {
    }
}
