package com.example.service.saas.config;

import cn.binaryflyer.wxjava.cp.api.WxCpService;
import cn.binaryflyer.wxjava.cp.api.impl.WxCpServiceImpl;
import cn.binaryflyer.wxjava.cp.config.WxCpConfig;
import cn.binaryflyer.wxjava.miniapp.api.WxMaService;
import cn.binaryflyer.wxjava.miniapp.api.impl.WxMaServiceImpl;
import cn.binaryflyer.wxjava.miniapp.config.WxMaConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WxJava 客户端装配。
 * 多租户说明：SaaS 场景下 corpId/secret 来自 tenant 表，请改为按租户动态构建
 * WxCpService（如 WxCpMultiClients），此处为单 corp 演示配置。
 */
@Configuration
public class WxJavaConfiguration {

    @Bean
    public WxCpService wxCpService(IntegrationProperties props) {
        WxCpService service = new WxCpServiceImpl();
        WxCpConfig config = new WxCpConfig();
        config.setCorpId(props.getWecom().getCorpId());
        config.setCorpSecret(props.getWecom().getCorpSecret());
        config.setAgentId(props.getWecom().getAgentId());
        config.setToken(props.getWecom().getToken());
        config.setAesKey(props.getWecom().getAesKey());
        service.setWxCpConfig(config);
        return service;
    }

    @Bean
    public WxMaService wxMaService(IntegrationProperties props) {
        WxMaService service = new WxMaServiceImpl();
        WxMaConfig config = new WxMaConfig();
        config.setAppid(props.getMiniapp().getAppId());
        config.setSecret(props.getMiniapp().getAppSecret());
        config.setToken(props.getMiniapp().getToken());
        config.setAesKey(props.getMiniapp().getAesKey());
        service.setWxMaConfig(config);
        return service;
    }
}
