package com.example.service.saas.controller;

import cn.binaryflyer.wxjava.core.common.exception.WxErrorException;
import cn.binaryflyer.wxjava.miniapp.api.WxMaService;
import cn.binaryflyer.wxjava.miniapp.bean.WxMaJscode2SessionResult;
import com.example.service.saas.common.ApiResponse;
import com.example.service.saas.common.BizException;
import com.example.service.saas.common.JwtUtil;
import com.example.service.saas.common.TenantContext;
import com.example.service.saas.entity.Customer;
import com.example.service.saas.entity.ServiceSchedule;
import com.example.service.saas.repository.ScheduleRepository;
import com.example.service.saas.service.CustomerService;
import com.example.service.saas.service.ScheduleService;
import com.example.service.saas.storage.OssStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信小程序端接口：登录、服务记录、服务详情、电子签名上传、提交评价。
 *
 * 认证方式：登录成功后签发 JWT，后续请求带 Authorization: Bearer {token}，
 * 由认证过滤器解析并注入 TenantContext（小程序场景填 customerId）。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/miniapp")
@RequiredArgsConstructor
public class MiniProgramController {

    private final WxMaService wxMaService;
    private final CustomerService customerService;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;
    private final OssStorageService ossStorageService;
    private final JwtUtil jwtUtil;

    /** 微信登录 + 手机号绑定 + 签发 JWT */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginForm form) {
        // 1. wx.login 的 code 换取 openid
        String openid;
        try {
            WxMaJscode2SessionResult session = wxMaService.getUserService().getSessionInfo(form.code());
            openid = session.getOpenid();
        } catch (WxErrorException e) {
            throw new BizException(401, "微信登录失败，请重试");
        }
        // 2. 手机号授权（新版 getPhoneNumber 返回 code，由后端换取；用户拒绝时降级）
        String phone = null;
        if (StringUtils.hasText(form.phoneCode())) {
            try {
                phone = wxMaService.getUserService().getNewPhoneNoInfo(form.phoneCode()).getPhoneNumber();
            } catch (WxErrorException e) {
                log.warn("手机号授权失败，降级为 openid 登录: {}", e.getError().getErrorCode());
            }
        }
        // 3. 按手机号匹配店长维护的客户档案，并绑定 openid（订阅消息依赖）
        Customer customer = customerService.findByPhone(phone)
                .orElseThrow(() -> new BizException("未找到您的服务档案，请联系门店店长"));
        customerService.bindOpenid(customer, openid);
        // 4. 签发 JWT
        String token = jwtUtil.createForCustomer(customer.getTenantId(), customer.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("customerId", customer.getId());
        data.put("customerName", customer.getName());
        return ApiResponse.ok(data);
    }

    /** 我的服务记录（按日期倒序） */
    @GetMapping("/my-schedules")
    public ApiResponse<List<ServiceSchedule>> mySchedules() {
        Long customerId = TenantContext.customerId();
        return ApiResponse.ok(scheduleRepository.findByCustomerIdOrderByServiceDateDesc(customerId));
    }

    /** 服务详情（仅限本人） */
    @GetMapping("/schedules/{id}")
    public ApiResponse<ServiceSchedule> detail(@PathVariable Long id) {
        ServiceSchedule s = scheduleRepository.findById(id)
                .orElseThrow(() -> new BizException("服务记录不存在"));
        if (!s.getCustomerId().equals(TenantContext.customerId())) {
            throw new BizException(403, "无权查看该服务记录");
        }
        return ApiResponse.ok(s);
    }

    /** 电子签名上传：小程序传 base64，后端落 OSS 后登记签名 */
    @PostMapping("/schedules/{id}/signature")
    public ApiResponse<String> uploadSignature(@PathVariable Long id,
                                               @Valid @RequestBody SignatureUploadForm form,
                                               HttpServletRequest request) {
        ServiceSchedule s = scheduleRepository.findById(id)
                .orElseThrow(() -> new BizException("服务记录不存在"));
        if (!s.getCustomerId().equals(TenantContext.customerId())) {
            throw new BizException(403, "无权操作该服务记录");
        }
        String url = ossStorageService.uploadBase64("sign/" + id, form.image());
        scheduleService.sign(id, TenantContext.customerId(), url, request.getRemoteAddr());
        return ApiResponse.ok(url);
    }

    /** 提交评价：星级 + 服务项目勾选（最多 5 项）+ 文字 */
    @PostMapping("/schedules/{id}/evaluation")
    public ApiResponse<Void> evaluate(@PathVariable Long id, @Valid @RequestBody EvalForm form) {
        ServiceSchedule s = scheduleRepository.findById(id)
                .orElseThrow(() -> new BizException("服务记录不存在"));
        if (!s.getCustomerId().equals(TenantContext.customerId())) {
            throw new BizException(403, "无权操作该服务记录");
        }
        scheduleService.evaluate(id, TenantContext.customerId(), form.score(),
                form.tags() == null ? null : String.join(",", form.tags()), form.comment());
        return ApiResponse.ok();
    }

    public record LoginForm(
            @NotBlank(message = "code 不能为空") String code,
            String phoneCode) {
    }

    public record SignatureUploadForm(
            @NotBlank(message = "签名图片不能为空") String image) {
    }

    public record EvalForm(
            @NotNull(message = "评分不能为空") Integer score,
            List<String> tags,
            String comment) {
    }
}
