package com.example.service.saas.repository;

import com.example.service.saas.entity.ScheduleStatus;
import com.example.service.saas.entity.ServiceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<ServiceSchedule, Long> {

    /** 员工当日未完工工单数（排班负载校验） */
    long countByStaffIdAndServiceDateAndStatusIn(Long staffId, LocalDate serviceDate,
                                                 Collection<ScheduleStatus> statuses);

    Optional<ServiceSchedule> findByIdAndTenantId(Long id, Long tenantId);

    /** 店长端：按日期查看当日排班 */
    List<ServiceSchedule> findByTenantIdAndServiceDate(Long tenantId, LocalDate serviceDate);

    /** 员工端：我的排班（从某日起） */
    List<ServiceSchedule> findByStaffIdAndServiceDateGreaterThanEqual(Long staffId, LocalDate date);

    /** 小程序端：客户的服务记录 */
    List<ServiceSchedule> findByCustomerIdOrderByServiceDateDesc(Long customerId);
}
