package com.example.service.saas.service;

import com.example.service.saas.entity.ServiceEvaluation;
import com.example.service.saas.repository.ServiceEvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 评价查询与报表（店长端） */
@Service
@RequiredArgsConstructor
public class EvaluationQueryService {

    private final ServiceEvaluationRepository evaluationRepository;

    public List<ServiceEvaluation> list(Long tenantId, Long staffId, YearMonth month) {
        LocalDateTime[] range = monthRange(month);
        if (staffId != null) {
            return evaluationRepository.findByTenantIdAndStaffIdAndCreatedAtBetween(
                    tenantId, staffId, range[0], range[1]);
        }
        return evaluationRepository.findByTenantIdAndCreatedAtBetween(
                tenantId, range[0], range[1]);
    }

    /** 汇总：评价数、平均分、各星级数量 */
    public Map<String, Object> stats(Long tenantId, Long staffId, YearMonth month) {
        List<ServiceEvaluation> list = list(tenantId, staffId, month);
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        list.forEach(e -> distribution.merge(e.getScore(), 1L, Long::sum));
        double avg = list.stream().mapToInt(ServiceEvaluation::getScore).average().orElse(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", list.size());
        result.put("averageScore", Math.round(avg * 10) / 10.0);
        result.put("distribution", distribution);
        return result;
    }

    private LocalDateTime[] monthRange(YearMonth month) {
        if (month == null) {
            month = YearMonth.now();
        }
        ZoneId zone = ZoneId.systemDefault();
        return new LocalDateTime[]{
                month.atDay(1).atStartOfDay(),
                month.atEndOfMonth().atTime(23, 59, 59)
        };
    }
}
