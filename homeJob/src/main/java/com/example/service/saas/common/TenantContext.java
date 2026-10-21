package com.example.service.saas.common;

/**
 * 租户与当前登录人上下文（ThreadLocal）。
 * 由认证拦截器 / 网关在请求入口根据 JWT 注入，请求结束后必须 clear()。
 * 员工端填 userId（店长或员工），小程序端填 customerId。
 */
public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> CUSTOMER_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId, Long userId, Long customerId) {
        TENANT_ID.set(tenantId);
        USER_ID.set(userId);
        CUSTOMER_ID.set(customerId);
    }

    public static Long tenantId() {
        Long id = TENANT_ID.get();
        if (id == null) {
            throw new BizException(401, "缺少租户上下文，请先登录");
        }
        return id;
    }

    /** 员工端当前登录人（店长/员工）ID */
    public static Long userId() {
        Long id = USER_ID.get();
        if (id == null) {
            throw new BizException(401, "缺少用户上下文，请先登录");
        }
        return id;
    }

    /** 小程序端当前客户 ID */
    public static Long customerId() {
        Long id = CUSTOMER_ID.get();
        if (id == null) {
            throw new BizException(401, "缺少客户上下文，请先登录");
        }
        return id;
    }

    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
        CUSTOMER_ID.remove();
    }
}
