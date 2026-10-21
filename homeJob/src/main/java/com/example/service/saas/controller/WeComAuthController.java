package com.example.service.saas.controller;

import cn.binaryflyer.wxjava.cp.api.WxCpService;
import com.example.service.saas.common.ApiResponse;
import com.example.service.saas.common.BizException;
import com.example.service.saas.common.JwtUtil;
import com.example.service.saas.entity.SysUser;
import com.example.service.saas.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 员工端（店长/员工）登录：企业微信网页授权 OAuth2 静默授权。
 *
 * <p>流程：
 * <ol>
 *   <li>企微工作台应用主页携带 code 打开 redirect_uri；
 *      未授权时企微自动跳转到企业微信登录页，回调时带 code。</li>
 *   <li>本接口用 code 调 {@code user/getuserinfo} 换取企业内 userid。</li>
 *   <li>按 sys_user.wecom_userid 反查系统用户 → 签发 JWT（含 tenantId + userId）。</li>
 * </ol>
 *
 * <p>前端拿到 token 后放全局请求头 Authorization: Bearer {token}，
 * 后续 /api/v1/** 请求由 AuthInterceptor 解析并注入 TenantContext。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/wecom")
@RequiredArgsConstructor
public class WeComAuthController {

    private final WxCpService wxCpService;
    private final SysUserRepository userRepository;
    private final JwtUtil jwtUtil;

    /** 企微授权回调：用 code 换 userid 并签发 token */
    @GetMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestParam("code") String code) {
        String wecomUserid;
        try {
            wecomUserid = wxCpService.getUserService().getUserInfo(code).getUserid();
        } catch (Exception e) {
            log.error("企微网页授权 code 换取 userid 失败", e);
            throw new BizException(401, "企微授权失败，请重新进入应用");
        }
        SysUser user = userRepository.findByWecomUserid(wecomUserid)
                .orElseThrow(() -> new BizException(403, "您不在服务人员名单中，请联系门店店长开通"));
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(403, "账号已停用，请联系门店店长");
        }
        String token = jwtUtil.createForUser(user.getTenantId(), user.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("name", user.getName());
        data.put("role", user.getRole());
        data.put("storeId", user.getStoreId());
        return ApiResponse.ok(data);
    }
}