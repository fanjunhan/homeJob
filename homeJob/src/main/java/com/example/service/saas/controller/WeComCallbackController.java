package com.example.service.saas.controller;

import com.example.service.saas.common.BizException;
import com.example.service.saas.config.IntegrationProperties;
import com.example.service.saas.service.ContactSyncService;
import com.example.service.saas.util.WeComCryptoUtil;
import com.example.service.saas.util.WeComXmlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 企业微信回调服务。
 *
 * 规范（官方「接收消息 / 回调配置」）：
 * 1. GET 验证：参数 msg_signature、timestamp、nonce、echostr；
 *    校验签名 → 解密 echostr → 1 秒内原样返回明文（不加引号、不带 BOM、不带换行）。
 * 2. POST 接收：校验签名 → 解密 Encrypt → 处理业务；5 秒内未响应会重试 3 次，
 *    事件类消息建议用 FromUserName + CreateTime 排重；不需要被动回复时返回 "success"。
 */
@Slf4j
@RestController
@RequestMapping("/api/wecom/callback")
@RequiredArgsConstructor
public class WeComCallbackController {

    private final IntegrationProperties props;
    private final ContactSyncService contactSyncService;

    /** 回调 URL 有效性验证 */
    @GetMapping(produces = "text/plain;charset=UTF-8")
    public String verifyUrl(@RequestParam("msg_signature") String msgSignature,
                            @RequestParam("timestamp") String timestamp,
                            @RequestParam("nonce") String nonce,
                            @RequestParam("echostr") String echostr) {
        String token = props.getWecom().getToken();
        String aesKey = props.getWecom().getAesKey();
        if (!WeComCryptoUtil.checkSignature(token, timestamp, nonce, echostr, msgSignature)) {
            throw new BizException(403, "回调签名校验失败");
        }
        // 原样返回明文，不得加引号 / BOM / 换行
        return WeComCryptoUtil.decrypt(echostr, aesKey);
    }

    /** 接收事件与业务数据 */
    @PostMapping(produces = "text/plain;charset=UTF-8")
    public String receive(@RequestParam("msg_signature") String msgSignature,
                          @RequestParam("timestamp") String timestamp,
                          @RequestParam("nonce") String nonce,
                          @RequestBody String body) {
        String token = props.getWecom().getToken();
        String aesKey = props.getWecom().getAesKey();
        String encrypt = WeComXmlUtil.extractEncrypt(body);
        if (!WeComCryptoUtil.checkSignature(token, timestamp, nonce, encrypt, msgSignature)) {
            throw new BizException(403, "回调签名校验失败");
        }
        String plain = WeComCryptoUtil.decrypt(encrypt, aesKey);
        // 立即应答，业务异步处理，避免企微 5 秒超时重试
        contactSyncService.handleCallbackAsync(plain);
        return "success";
    }
}
