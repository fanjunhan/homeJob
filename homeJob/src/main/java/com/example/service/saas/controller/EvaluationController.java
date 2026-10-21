package com.example.service.saas.controller;

import com.example.service.saas.common.ApiResponse;
import com.example.service.saas.common.TenantContext;
import com.example.service.saas.entity.ServiceEvaluation;
import com.example.service.saas.service.EvaluationQueryService;
import com.example.service.saas.service.EvalItemService;
import com.example.service.saas.service.ScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** 服务评价：提交（客户）、报表查询与评价目录（店长端） */
@RestController
@RequiredArgsConstructor
public class EvaluationController {

    private final ScheduleService scheduleService;
    private final EvaluationQueryService evaluationQueryService;
    private final EvalItemService evalItemService;

    /**
     * 提交评价（员工端代客提交场景）。
     * 小程序端请使用 POST /api/v1/miniapp/schedules/{id}/evaluation。
     */
    @PostMapping("/api/v1/schedules/{id}/evaluation")
    public ApiResponse<Void> evaluate(@PathVariable Long id, @Valid @RequestBody EvalForm form) {
        scheduleService.evaluate(id, TenantContext.customerId(), form.score(),
                form.tags() == null ? null : String.join(",", form.tags()), form.comment());
        return ApiResponse.ok();
    }

    /** 评价报表：按员工/门店 + 月份 */
    @GetMapping("/api/v1/evaluations")
    public ApiResponse<List<ServiceEvaluation>> list(@RequestParam(required = false) Long staffId,
                                                     @RequestParam(required = false) YearMonth month) {
        return ApiResponse.ok(evaluationQueryService.list(TenantContext.tenantId(), staffId, month));
    }

    /** 评价统计：平均分、星级分布 */
    @GetMapping("/api/v1/evaluations/stats")
    public ApiResponse<Map<String, Object>> stats(@RequestParam(required = false) Long staffId,
                                                  @RequestParam(required = false) YearMonth month) {
        return ApiResponse.ok(evaluationQueryService.stats(TenantContext.tenantId(), staffId, month));
    }

    /**
     * 评价服务项目目录：生活照护类 20 项 + 医疗护理类 16 项（长护险目录），
     * 小程序评价页从此接口拉取，目录调整无需前端发版。
     */
    @GetMapping("/api/v1/eval-items")
    public ApiResponse<List<EvalItemService.Group>> evalItems() {
        return ApiResponse.ok(evalItemService.groups());
    }

    public record EvalForm(
            @NotNull(message = "评分不能为空") @Min(value = 1, message = "评分最低 1 星")
            @Max(value = 5, message = "评分最高 5 星") Integer score,
            List<String> tags,
            String comment) {
    }
}
