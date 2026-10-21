package com.example.service.saas.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 企微回调加解密单元测试。
 *
 * <p>用与 {@code WeComCryptoUtil} 对称的方式构造加密数据（同样的 AES 参数与明文结构），
 * 验证「能正确解密」与「篡改后能识别」两件事。
 * 加密侧实现放在测试里，避免为生产代码引入 encrypt 方法而扩大攻击面。
 */
class WeComCryptoUtilTest {

    /**
     * EncodingAESKey 必须是 43 位（<b>不带结尾 '='</b>），补 '=' 后 Base64 解码为 32 字节 AES-256 密钥。
     * 企微后台生成的就是这个格式；多写或少写一个字符都会导致解码后不是 32 字节，
     * 从而抛出 "Invalid AES key length"，是接入时的常见坑。
     */
    private static final String AES_KEY = "jWmYm7qr5nMoAUwZRjGtBxmz3KA1tkAj3ykkR6q2B2C";
    private static final String TOKEN = "test-callback-token";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    @DisplayName("测试用密钥本身合法：43 位，补 '=' 解码为 32 字节")
    void aesKeyFixtureShouldBeValid() {
        assertEquals(43, AES_KEY.length());
        assertEquals(32, Base64.getDecoder()
                .decode((AES_KEY + "=").getBytes(StandardCharsets.UTF_8)).length);
    }

    /** 按企微明文结构加密：16 随机字节 + 4 字节网络序长度 + 正文 + receiveid */
    private static String encrypt(String plainXml) {
        return encrypt(plainXml.getBytes(StandardCharsets.UTF_8));
    }

    private static String encrypt(byte[] body) {
        try {
            byte[] aesKey = Base64.getDecoder().decode((AES_KEY + "=").getBytes(StandardCharsets.UTF_8));
            byte[] plain = new byte[20 + body.length + 8];
            RANDOM.nextBytes(Arrays.copyOfRange(plain, 0, 16));
            int len = body.length;
            plain[16] = (byte) ((len >> 24) & 0xff);
            plain[17] = (byte) ((len >> 16) & 0xff);
            plain[18] = (byte) ((len >> 8) & 0xff);
            plain[19] = (byte) (len & 0xff);
            System.arraycopy(body, 0, plain, 20, body.length);
            // 尾部 receiveid（corpid），本测试填 8 字节占位
            Arrays.fill(plain, 20 + body.length, plain.length, (byte) 'x');

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(aesKey, "AES"),
                    new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16)));
            return Base64.getEncoder().encodeToString(cipher.doFinal(plain));
        } catch (Exception e) {
            throw new IllegalStateException("测试数据构造失败", e);
        }
    }

    /** 构造 msgLen 被伪造为指定值的密文（模拟异常/恶意报文） */
    private static String encryptWithForgedLength(int forgedLen) {
        byte[] plain = new byte[28];
        RANDOM.nextBytes(Arrays.copyOfRange(plain, 0, 16));
        plain[16] = (byte) ((forgedLen >> 24) & 0xff);
        plain[17] = (byte) ((forgedLen >> 16) & 0xff);
        plain[18] = (byte) ((forgedLen >> 8) & 0xff);
        plain[19] = (byte) (forgedLen & 0xff);
        return encryptRaw(plain);
    }

    private static String encryptRaw(byte[] plain) {
        try {
            byte[] aesKey = Base64.getDecoder().decode((AES_KEY + "=").getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(aesKey, "AES"),
                    new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16)));
            return Base64.getEncoder().encodeToString(cipher.doFinal(plain));
        } catch (Exception e) {
            throw new IllegalStateException("测试数据构造失败", e);
        }
    }

    @Test
    @DisplayName("能正确解密企微明文结构，且忽略尾部 receiveid")
    void decryptShouldReturnPlainXml() {
        String xml = "<xml><ToUserName>corpid</ToUserName><FromUserName>sys</FromUserName>"
                + "<MsgType>event</MsgType><Event>change_contact</Event>"
                + "<ChangeType>create_user</ChangeType><UserName>zhangsan</UserName>"
                + "<Name>张三</Name></xml>";
        assertEquals(xml, WeComCryptoUtil.decrypt(encrypt(xml), AES_KEY));
    }

    @Test
    @DisplayName("中文正文（UTF-8 多字节）不应被截断")
    void decryptShouldHandleChineseContent() {
        String xml = "<xml><Content>长护险上门服务：协助进食、协助排泄、生命体征测量</Content></xml>";
        assertEquals(xml, WeComCryptoUtil.decrypt(encrypt(xml), AES_KEY));
    }

    @Test
    @DisplayName("验签：参数按字典序拼接后取 SHA1")
    void signatureShouldBeStableAndOrderIndependent() {
        String sig1 = WeComCryptoUtil.signature(TOKEN, "1700000000", "nonce", "encrypt-body");
        String sig2 = WeComCryptoUtil.signature("nonce", TOKEN, "encrypt-body", "1700000000");
        assertEquals(sig1, sig2, "参数顺序不应影响签名");
        assertEquals(40, sig1.length(), "SHA1 十六进制应为 40 位");
    }

    @Test
    @DisplayName("验签：token 或密文被篡改应判定失败")
    void checkSignatureShouldRejectTamperedParams() {
        String encrypt = encrypt("<xml><Event>change_contact</Event></xml>");
        String timestamp = "1700000000";
        String nonce = "abc123";
        String correct = WeComCryptoUtil.signature(TOKEN, timestamp, nonce, encrypt);

        assertTrue(WeComCryptoUtil.checkSignature(TOKEN, timestamp, nonce, encrypt, correct));
        // 错误 token
        assertFalse(WeComCryptoUtil.checkSignature("wrong-token", timestamp, nonce, encrypt, correct));
        // 密文被替换
        assertFalse(WeComCryptoUtil.checkSignature(TOKEN, timestamp, nonce,
                encrypt + "AAAA", correct));
        // timestamp 被替换
        assertFalse(WeComCryptoUtil.checkSignature(TOKEN, "1700000001", nonce, encrypt, correct));
        // 签名为空
        assertFalse(WeComCryptoUtil.checkSignature(TOKEN, timestamp, nonce, encrypt, null));
    }

    @Test
    @DisplayName("换密钥后签名应不同——避免签名可跨租户复用")
    void signatureShouldDifferByKey() {
        String encrypt = "encrypt-body";
        assertNotEquals(
                WeComCryptoUtil.signature(TOKEN, "1700000000", "n", encrypt),
                WeComCryptoUtil.signature(TOKEN + "x", "1700000000", "n", encrypt));
    }

    @Test
    @DisplayName("密文非法时抛业务异常，不返回半截数据")
    void decryptShouldThrowOnMalformedInput() {
        assertThrows(IllegalStateException.class,
                () -> WeComCryptoUtil.decrypt("not-base64-!!!", AES_KEY));
        assertThrows(IllegalStateException.class,
                () -> WeComCryptoUtil.decrypt("aGVsbG8gd29ybGQ=", "short-key"));
        // 长度字段是外部输入：伪造超大 msgLen 时必须被长度校验拦住，
        // 而不是抛 ArrayIndexOutOfBoundsException 或解析出垃圾 XML
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> WeComCryptoUtil.decrypt(encryptWithForgedLength(0x7f000000), AES_KEY));
        assertTrue(ex.getMessage().contains("长度"), "应为长度校验失败，实际: " + ex.getMessage());
    }

    @Test
    @DisplayName("长度字段伪造为 0 或负数同样被拒")
    void decryptShouldRejectInvalidLengths() {
        assertThrows(IllegalStateException.class,
                () -> WeComCryptoUtil.decrypt(encryptWithForgedLength(0), AES_KEY));
        assertThrows(IllegalStateException.class,
                () -> WeComCryptoUtil.decrypt(encryptWithForgedLength(-1), AES_KEY));
    }

    @Test
    @DisplayName("EncryptingAesKey 位数不对时提示清晰")
    void decryptShouldGiveClearHintOnBadKeyLength() {
        // 45 位（多一位）→ 解码后 33 字节，非 AES 合法长度
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> WeComCryptoUtil.decrypt(encrypt("<xml/>"), AES_KEY + "AA"));
        assertTrue(ex.getMessage().contains("EncodingAESKey"),
                "应提示 EncodingAESKey 位数问题，实际: " + ex.getMessage());
    }
}