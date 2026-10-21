package com.example.service.saas.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * 企业微信回调消息加解密工具。
 *
 * 规范要点（企业微信官方「消息加解密」）：
 * 1. 签名：sha1(sort(token, timestamp, nonce, encrypt))，参数字典序拼接后取 SHA1；
 * 2. 加密：AES-256-CBC，密钥 = Base64Decode(EncodingAESKey + "=")，IV 取密钥前 16 字节，PKCS5/PKCS7 填充；
 * 3. 明文结构：16 字节随机数 + 4 字节网络字节序正文长度 + 正文 + 接收方 corpid；
 * 4. 回调 URL 验证（GET）：校验签名后，将 echostr 解密并原样返回明文。
 *
 * 说明：也可用 WxJava 内置的 WXBizMsgCrypt 替换本实现，算法等价。
 */
public final class WeComCryptoUtil {

    /** 明文头部长度：16 字节随机数 + 4 字节网络序正文长度 */
    private static final int HEADER_LENGTH = 20;

    private WeComCryptoUtil() {
    }

    /** 验签：sha1(sort(token, timestamp, nonce, encrypt)) */
    public static boolean checkSignature(String token, String timestamp, String nonce,
                                         String encrypt, String msgSignature) {
        if (msgSignature == null) {
            return false;
        }
        return signature(token, timestamp, nonce, encrypt).equalsIgnoreCase(msgSignature);
    }

    public static String signature(String token, String timestamp, String nonce, String encrypt) {
        String[] arr = new String[]{token, timestamp, nonce, encrypt};
        Arrays.sort(arr);
        return sha1Hex(String.join("", arr));
    }

    /**
     * 解密回调消息体
     *
     * @param base64Encrypted 加密串（Base64）
     * @param encodingAesKey  43 位 EncodingAESKey（<b>不带结尾 '='</b>）
     * @return 解密后的 XML 明文
     */
    public static String decrypt(String base64Encrypted, String encodingAesKey) {
        byte[] aesKey = decodeAesKey(encodingAesKey);
        try {
            byte[] encrypted = Base64.getDecoder().decode(base64Encrypted.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(aesKey, "AES"),
                    new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16)));
            byte[] plain = cipher.doFinal(encrypted);
            // 结构：16 字节随机数 + 4 字节网络序长度 + 正文 + receiveid
            if (plain.length < HEADER_LENGTH + 1) {
                throw new IllegalStateException("企微回调明文长度异常: " + plain.length);
            }
            int msgLen = readLength(plain);
            if (msgLen <= 0 || msgLen > plain.length - HEADER_LENGTH) {
                // 长度字段来自外部输入，必须校验，否则数组越界 / 解析出垃圾数据
                throw new IllegalStateException("企微回调正文长度非法: " + msgLen);
            }
            return new String(plain, HEADER_LENGTH, msgLen, StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("企微回调消息解密失败", e);
        }
    }

    /**
     * 解码 EncodingAESKey。
     * 企微后台生成的 EncodingAESKey 为 43 位、不带结尾 '='，补一个 '=' 后 Base64 解码得 32 字节 AES-256 密钥。
     * 位数写错（如多带一个 '='、或直接粘贴了含 '=' 的串）都会导致解码后不是 32 字节，
     * JCE 抛出的 "Invalid AES key length" 难以定位，这里提前给出可读的错误。
     */
    private static byte[] decodeAesKey(String encodingAesKey) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode((encodingAesKey + "=").getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "EncodingAESKey 格式非法：应为 43 位且不带结尾 '='，当前长度 " + length(encodingAesKey), e);
        }
        if (key.length != 32) {
            throw new IllegalStateException(
                    "EncodingAESKey 位数错误：43 位解码后应为 32 字节，实际 " + key.length + " 字节");
        }
        return key;
    }

    private static int length(String s) {
        return s == null ? 0 : s.length();
    }

    /** 头部 = 16 字节随机数 + 4 字节网络序长度 */
    private static int readLength(byte[] plain) {
        return ((plain[16] & 0xff) << 24) | ((plain[17] & 0xff) << 16)
                | ((plain[18] & 0xff) << 8) | (plain[19] & 0xff);
    }

    private static String sha1Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-1 计算失败", e);
        }
    }
}
