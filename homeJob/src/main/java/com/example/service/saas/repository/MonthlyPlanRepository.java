package com.example.service.saas.repository;

import com.example.service.saas.entity.MonthlyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthlyPlanRepository extends JpaRepository<MonthlyPlan, Long> {

    Optional<MonthlyPlan> findByCustomerIdAndPlanMonth(Long customerId, String planMonth);
}
