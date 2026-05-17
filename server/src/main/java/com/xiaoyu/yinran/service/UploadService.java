package com.xiaoyu.yinran.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.xiaoyu.yinran.config.AppProperties;
import com.xiaoyu.yinran.dto.DirectUploadRequest;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {
    private static final Set<String> IMAGE_ALLOWED = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> VIDEO_ALLOWED = Set.of("mp4");
    private static final long MAX_UPLOAD_SIZE = 100L * 1024L * 1024L;
    private static final String COS_AUTH_URL = "http://api.weixin.qq.com/_/cos/getauth";

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, String> uploadImage(MultipartFile file) throws IOException {
        return upload(
                file,
                "image",
                "products",
                IMAGE_ALLOWED,
                "Please choose an image file",
                "Only jpg, jpeg, png, webp and gif images are supported"
        );
    }

    public Map<String, String> uploadVideo(MultipartFile file) throws IOException {
        return upload(
                file,
                "video",
                "videos",
                VIDEO_ALLOWED,
                "Please choose a video file",
                "Only mp4 videos are supported"
        );
    }

    public Map<String, String> createDirectUploadUrl(DirectUploadRequest request) {
        if (!"cos".equalsIgnoreCase(appProperties.getStorageMode())) {
            throw new IllegalStateException("当前存储模式不支持直传");
        }
        if (request == null) {
            throw new IllegalArgumentException("上传参数不能为空");
        }
        if (request.getSize() != null && request.getSize() > MAX_UPLOAD_SIZE) {
            throw new IllegalArgumentException("文件最大不得超过100MB");
        }
        String mediaType = "VIDEO".equalsIgnoreCase(request.getMediaType()) ? "VIDEO" : "IMAGE";
        Set<String> allowed = "VIDEO".equals(mediaType) ? VIDEO_ALLOWED : IMAGE_ALLOWED;
        String folder = "VIDEO".equals(mediaType) ? "videos" : "products";
        String filename = StringUtils.cleanPath(StringUtils.hasText(request.getFilename()) ? request.getFilename() : mediaType.toLowerCase());
        String ext = getExt(filename);
        if (!allowed.contains(ext)) {
            throw new IllegalArgumentException("VIDEO".equals(mediaType)
                    ? "Only mp4 videos are supported"
                    : "Only jpg, jpeg, png, webp and gif images are supported");
        }

        String datePath = LocalDate.now().toString().replace("-", "/");
        String objectKey = folder + "/" + datePath + "/" + UUID.randomUUID() + "." + ext;
        String contentType = StringUtils.hasText(request.getContentType()) ? request.getContentType() : "application/octet-stream";
        String uploadUrl = signedCosUrl(buildCosKey(objectKey), HttpMethodName.PUT, contentType, 900);
        String url = resolveFileUrl(objectKey);
        return Map.of("uploadUrl", uploadUrl, "url", url, "objectKey", objectKey);
    }

    private Map<String, String> upload(MultipartFile file, String fallbackName, String folder, Set<String> allowed,
                                       String emptyMessage, String typeMessage) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(emptyMessage);
        }
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            throw new IllegalArgumentException("文件最大不得超过100MB");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? fallbackName : file.getOriginalFilename());
        String ext = getExt(original);
        if (!allowed.contains(ext)) {
            throw new IllegalArgumentException(typeMessage);
        }

        String datePath = LocalDate.now().toString().replace("-", "/");
        String fileName = UUID.randomUUID() + "." + ext;
        String objectKey = folder + "/" + datePath + "/" + fileName;

        if ("cos".equalsIgnoreCase(appProperties.getStorageMode())) {
            uploadToCos(file, objectKey);
        } else {
            uploadToLocal(file, objectKey);
        }

        String url = resolveFileUrl(objectKey);
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
        String bucket = required(appProperties.getCosBucket(), "COS_BUCKET is required");
        String region = required(appProperties.getCosRegion(), "COS_REGION is required");
        String cosKey = buildCosKey(objectKey);
        COSCredentials credentials = cosCredentials();

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

    private COSCredentials cosCredentials() throws IOException {
        if (StringUtils.hasText(appProperties.getCosSecretId()) && StringUtils.hasText(appProperties.getCosSecretKey())) {
            return new BasicCOSCredentials(appProperties.getCosSecretId(), appProperties.getCosSecretKey());
        }
        CosAuth auth = getCosAuth();
        return new BasicSessionCredentials(auth.tmpSecretId(), auth.tmpSecretKey(), auth.token());
    }

    public String buildCosKey(String objectKey) {
        String prefix = appProperties.getCosKeyPrefix();
        if (!StringUtils.hasText(prefix)) {
            return objectKey;
        }
        prefix = trimSlash(prefix);
        return prefix + "/" + objectKey;
    }

    public String resolveFileUrl(String urlOrObjectKey) {
        if (!StringUtils.hasText(urlOrObjectKey)) {
            return urlOrObjectKey;
        }
        String objectKey = normalizeObjectKey(urlOrObjectKey);
        if ("cos".equalsIgnoreCase(appProperties.getStorageMode()) && !isHttpUrl(objectKey)) {
            return signedCosUrl(buildCosKey(objectKey));
        }
        return publicUrl(objectKey);
    }

    private String publicUrl(String objectKey) {
        if (isHttpUrl(objectKey)) {
            return objectKey;
        }
        String baseUrl = trimRightSlash(appProperties.getPublicFileBaseUrl());
        return baseUrl + "/" + objectKey;
    }

    private String signedCosUrl(String cosKey) {
        return signedCosUrl(cosKey, HttpMethodName.GET, null, appProperties.getCosSignedUrlSeconds());
    }

    private String signedCosUrl(String cosKey, HttpMethodName method, String contentType, long signedSeconds) {
        String bucket = required(appProperties.getCosBucket(), "COS_BUCKET is required");
        String region = required(appProperties.getCosRegion(), "COS_REGION is required");
        long seconds = Math.max(signedSeconds, 60);
        Date expiration = Date.from(Instant.now().plusSeconds(seconds));
        COSClient client = new COSClient(cosCredentialsUnchecked(), new ClientConfig(new Region(region)));
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, cosKey, method);
            request.setExpiration(expiration);
            if (StringUtils.hasText(contentType)) {
                request.setContentType(contentType);
            }
            return client.generatePresignedUrl(request).toString();
        } finally {
            client.shutdown();
        }
    }

    private COSCredentials cosCredentialsUnchecked() {
        try {
            return cosCredentials();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare COS credentials", e);
        }
    }

    private String normalizeObjectKey(String urlOrObjectKey) {
        String value = urlOrObjectKey.trim();
        String base = appProperties.getPublicFileBaseUrl();
        if (StringUtils.hasText(base)) {
            base = trimRightSlash(base);
            if (value.startsWith(base + "/")) {
                value = value.substring(base.length() + 1);
            }
        }
        int queryIndex = value.indexOf('?');
        if (queryIndex >= 0) {
            value = value.substring(0, queryIndex);
        }
        int uploadsIndex = value.indexOf("/uploads/");
        if (uploadsIndex >= 0) {
            value = value.substring(uploadsIndex + "/uploads/".length());
        } else if (value.startsWith("uploads/")) {
            value = value.substring("uploads/".length());
        } else if (value.startsWith("/uploads/")) {
            value = value.substring("/uploads/".length());
        } else if (value.startsWith("/")) {
            value = value.substring(1);
        } else if (isHttpUrl(value)) {
            return value;
        }
        String prefix = trimSlash(appProperties.getCosKeyPrefix());
        if (StringUtils.hasText(prefix) && value.startsWith(prefix + "/")) {
            value = value.substring(prefix.length() + 1);
        }
        return value;
    }

    private boolean isHttpUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private CosAuth getCosAuth() throws IOException {
        ResponseEntity<String> response = restTemplate.getForEntity(COS_AUTH_URL, String.class);
        String body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(body)) {
            throw new IllegalStateException("Failed to get temporary COS credentials. Configure COS_SECRET_ID and COS_SECRET_KEY.");
        }
        JsonNode json = objectMapper.readTree(body);
        String tmpSecretId = text(json, "TmpSecretId");
        String tmpSecretKey = text(json, "TmpSecretKey");
        String token = text(json, "Token");
        if (!StringUtils.hasText(tmpSecretId) || !StringUtils.hasText(tmpSecretKey) || !StringUtils.hasText(token)) {
            throw new IllegalStateException("Invalid temporary COS credentials. Configure COS_SECRET_ID and COS_SECRET_KEY.");
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
