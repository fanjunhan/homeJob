package com.example.service.saas.repository;

import com.example.service.saas.entity.ServiceEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ServiceEvaluationRepository extends JpaRepository<ServiceEvaluation, Long> {

    /** 评价报表：按员工 + 月份 */
    List<ServiceEvaluation> findByTenantIdAndStaffIdAndCreatedAtBetween(Long tenantId, Long staffId,
                                                                       LocalDateTime begin, LocalDateTime end);

    /** 评价报表：门店整体 + 月份 */
    List<ServiceEvaluation> findByTenantIdAndCreatedAtBetween(Long tenantId, LocalDateTime begin, LocalDateTime end);
}
