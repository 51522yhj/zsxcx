package com.xiaoyu.yinran.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.xiaoyu.yinran.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final String COS_AUTH_URL = "http://api.weixin.qq.com/_/cos/getauth";

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, String> uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择图片");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        String ext = getExt(original);
        if (!ALLOWED.contains(ext)) {
            throw new IllegalArgumentException("仅支持 jpg、png、webp、gif 图片");
        }

        String datePath = LocalDate.now().toString().replace("-", "/");
        String fileName = UUID.randomUUID() + "." + ext;
        String objectKey = "products/" + datePath + "/" + fileName;

        if ("cos".equalsIgnoreCase(appProperties.getStorageMode())) {
            uploadToCos(file, objectKey);
        } else {
            uploadToLocal(file, objectKey);
        }

        String url = trimRightSlash(appProperties.getPublicFileBaseUrl()) + "/" + objectKey;
        return Map.of("url", url, "objectKey", objectKey);
    }

    private String getExt(String original) {
        int dot = original.lastIndexOf('.');
        if (dot < 0 || dot == original.length() - 1) {
            return "";
        }
        return original.substring(dot + 1).toLowerCase();
    }

    private void uploadToLocal(MultipartFile file, String objectKey) throws IOException {
        Path target = Path.of(appProperties.getUploadRoot()).resolve(objectKey).toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        file.transferTo(target);
    }

    private void uploadToCos(MultipartFile file, String objectKey) throws IOException {
        String bucket = required(appProperties.getCosBucket(), "COS_BUCKET 未配置");
        String region = required(appProperties.getCosRegion(), "COS_REGION 未配置");
        String cosKey = buildCosKey(objectKey);
        CosAuth auth = getCosAuth();

        COSCredentials credentials = new BasicSessionCredentials(auth.tmpSecretId(), auth.tmpSecretKey(), auth.token());
        COSClient client = new COSClient(credentials, new ClientConfig(new Region(region)));
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            if (StringUtils.hasText(file.getContentType())) {
                metadata.setContentType(file.getContentType());
            }
            client.putObject(new PutObjectRequest(bucket, cosKey, inputStream, metadata));
        } finally {
            client.shutdown();
        }
    }

    private String buildCosKey(String objectKey) {
        String prefix = appProperties.getCosKeyPrefix();
        if (!StringUtils.hasText(prefix)) {
            return objectKey;
        }
        prefix = trimSlash(prefix);
        return prefix + "/" + objectKey;
    }

    private CosAuth getCosAuth() throws IOException {
        ResponseEntity<String> response = restTemplate.getForEntity(COS_AUTH_URL, String.class);
        String body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(body)) {
            throw new IllegalStateException("获取对象存储临时密钥失败");
        }
        JsonNode json = objectMapper.readTree(body);
        String tmpSecretId = text(json, "TmpSecretId");
        String tmpSecretKey = text(json, "TmpSecretKey");
        String token = text(json, "Token");
        if (!StringUtils.hasText(tmpSecretId) || !StringUtils.hasText(tmpSecretKey) || !StringUtils.hasText(token)) {
            throw new IllegalStateException("对象存储临时密钥返回异常，请检查云调用是否配置 /_/cos/getauth");
        }
        return new CosAuth(tmpSecretId, tmpSecretKey, token);
    }

    private String text(JsonNode json, String field) {
        JsonNode value = json.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private String required(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private String trimRightSlash(String value) {
        if (value == null || value.isBlank()) {
            return "/uploads";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String trimSlash(String value) {
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private record CosAuth(String tmpSecretId, String tmpSecretKey, String token) {
    }
}
