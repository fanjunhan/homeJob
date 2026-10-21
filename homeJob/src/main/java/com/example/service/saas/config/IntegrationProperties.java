package com.example.service.saas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 企业微信 / 微信小程序集成配置。
 * application.yml 示例：
 * integration:
 *   wecom:
 *     corp-id: wwxxxxxxxx
 *     corp-secret: xxxx
 *     agent-id: 1000002
 *     suite-id: xxxx            # 服务商第三方应用模式
 *     token: xxxx
 *     aes-key: xxxx
 *     schedule-url: https://xx.wwcan.cn/schedule/detail
 *   miniapp:
 *     app-id: wxXXXX
 *     app-secret: xxxx
 *     token: xxxx# 小程序消息回调（客服消息）token
 *     aes-key: xxxx                # EncodingAESKey，43 位
 *     template-id-service-finish: xxxx
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "integration")
public class IntegrationProperties {

    private final Wecom wecom = new Wecom();

    private final Miniapp miniapp = new Miniapp();

    @Data
    public static class Wecom {
        /** 企业 corpid */
        private String corpId;
        /** 自建应用 secret */
        private String corpSecret;
        /** 自建应用 agentId */
        private Integer agentId;
        /** 服务商第三方应用 suite_id（服务商模式） */
        private String suiteId;
        /** 服务商模式 suite_secret */
        private String suiteSecret;
        /** 回调配置 */
        private String token;
        private String aesKey;
        /** 应用消息跳转的排班详情页地址 */
        private String scheduleUrl;
    }

    @Data
    public static class Miniapp {
        private String appId;
        private String appSecret;
        /** 小程序侧回调 token（客服消息/业务回调） */
        private String token;
        /** 小程序侧 EncodingAESKey（43 位），用于解密回调与客服消息 */
        private String aesKey;
        /** 订阅消息模板 ID（服务完成提醒） */
        private String templateIdServiceFinish;
    }
}
