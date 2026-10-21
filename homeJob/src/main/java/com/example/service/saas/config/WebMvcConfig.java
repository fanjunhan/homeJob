package com.example.service.saas.config;

import com.example.service.saas.common.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Web 层配置：注册认证拦截器（登录与企微回调接口不拦截） */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /** 免认证路径：登录接口需先拿到 token 才能访问其余接口 */
    private static final String[] WHITE_LIST = {
            "/api/v1/miniapp/login",
            "/api/v1/auth/wecom/login",
            "/api/wecom/callback/**",
            // 放行框架端点与接口文档
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(WHITE_LIST);
    }
}
