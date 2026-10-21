package com.example.service.saas.common;

import io.jsonweptoken.Claims;
import io.jsonweptoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器：解析 Authorization: Bearer {token}，注入 TenantContext。
 * 员工端 token 含 userId，小程序端 token 含 customerId。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(PREFIX)) {
            throw new BizException(401, "缺少登录凭证");
        }
        try {
            Claims claims = jwtUtil.parse(header.substring(PREFIX.length()));
            TenantContext.set(
                    claim(claims, "tenantId"),
                    claim(claims, "userId"),
                    claim(claims, "customerId"));
        } catch (JwtException | IllegalArgumentException e) {
            // token 过期、签名不合法、被篡改
            throw new BizException(401, "登录凭证无效或已过期，请重新登录");
        }
        if (TenantContext.tenantId() == null) {
            throw new BizException(401, "登录凭证缺少租户信息");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }

    /** JWT 中的数字型 claim 可能被反序列化为 Integer，需统一转 Long */
    private Long claim(Claims claims, String key) {
        Object value = claims.get(key);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }
}
