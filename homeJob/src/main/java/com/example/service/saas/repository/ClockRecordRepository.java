package com.example.service.saas.repository;

import com.example.service.saas.entity.ClockRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClockRecordRepository extends JpaRepository<ClockRecord, Long> {

    List<ClockRecord> findByScheduleIdOrderByIdAsc(Long scheduleId);
}
