package com.example.service.saas.repository;

import com.example.service.saas.entity.PlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlanItemRepository extends JpaRepository<PlanItem, Long> {

    Optional<PlanItem> findByCustomerIdAndServiceDate(Long customerId, LocalDate serviceDate);

    List<PlanItem> findByPlanId(Long planId);

    /** 跨客户日历：某租户当月全部计划日期 */
    List<PlanItem> findByTenantIdAndServiceDateBetween(Long tenantId, LocalDate start, LocalDate end);
}
