package com.example.service.saas.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * 对象存储服务：电子签名图片等服务凭证文件。
 * 小程序端直接传 base64（dataURL），由后端落 OSS，避免前端直传的密钥分发问题。
 * 高并发场景可改为「后端签预签名 URL + 前端 PUT 直传」。
 */
@Slf4j
@Component
public class OssStorageService {

    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.bucket}")
    private String bucket;

    @Value("${oss.access-key-id}")
    private String accessKeyId;

    @Value("${oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${oss.url-prefix:}")
    private String urlPrefix;

    /**
     * 上传 base64 图片，返回可访问 URL
     *
     * @param folder 目录，如 sign/1001
     * @param dataUrl data:image/png;base64,xxxx
     */
    public String uploadBase64(String folder, String dataUrl) {
        if (dataUrl == null || !dataUrl.contains(",")) {
            throw new IllegalArgumentException("非法的图片数据");
        }
        String meta = dataUrl.substring(0, dataUrl.indexOf(','));
        String base64 = dataUrl.substring(dataUrl.indexOf(',') + 1);
        String ext = meta.contains("jpeg") ? "jpg" : "png";
        byte[] bytes = Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));

        String day = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectKey = folder + "/" + day + "/" + UUID.randomUUID() + "." + ext;

        OSS client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            client.putObject(bucket, objectKey, new ByteArrayInputStream(bytes));
        } finally {
            client.shutdown();
        }
        String prefix = urlPrefix.isBlank()
                ? "https://" + bucket + "." + endpoint + "/"
                : urlPrefix;
        return prefix + objectKey;
    }
}
