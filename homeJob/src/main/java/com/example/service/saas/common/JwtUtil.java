package com.example.service.saas.common;

import io.jsonweptoken.Claims;
import io.jsonweptoken.Jwts;
import io.jsonweptoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 签发与校验。
 *
 * <p>员工端（企微工作台）与小程序端共用同一套 token 结构，靠 claim 区分身份：
 * <ul>
 *   <li>企微员工：userId 有值，customerId 为 null</li>
 *   <li>小程序客户：customerId 有值，userId 为 null</li>
 * </ul>
 * 两者均携带 tenantId，由 {@link AuthInterceptor} 注入 {@link TenantContext} 实现数据隔离。
 *
 * <p>密钥由 application.yml 的 saas.jwt.secret 提供，生产环境务必改为
 * 环境变量注入（如 saas.jwt.secret: ${JWT_SECRET}），不要提交到代码库。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final Duration ttl;

    public JwtUtil(@Value("${saas.jwt.secret}") String secret,
                   @Value("${saas.jwt.ttl-days:7}") long ttlDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofDays(ttlDays);
    }

    /** 员工端 token */
    public String createForUser(Long tenantId, Long userId) {
        return create(tenantId, userId, null);
    }

    /** 小程序端 token */
    public String createForCustomer(Long tenantId, Long customerId) {
        return create(tenantId, null, customerId);
    }

    private String create(Long tenantId, Long userId, Long customerId) {
        return Jwts.builder()
                .claim("tenantId", tenantId)
                .claim("userId", userId)
                .claim("customerId", customerId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttl.toMillis()))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}