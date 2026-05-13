package com.xiaoyu.yinran.service;

import com.xiaoyu.yinran.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final AppProperties appProperties;

    public Map<String, String> uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择图片");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot + 1).toLowerCase();
        }
        if (!ALLOWED.contains(ext)) {
            throw new IllegalArgumentException("仅支持 jpg、png、webp、gif 图片");
        }
        String datePath = LocalDate.now().toString().replace("-", "/");
        String fileName = UUID.randomUUID() + "." + ext;
        Path relative = Path.of("products", datePath, fileName);
        Path target = Path.of(appProperties.getUploadRoot()).resolve(relative).toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        file.transferTo(target);

        String objectKey = relative.toString().replace("\\", "/");
        String base = appProperties.getPublicFileBaseUrl();
        String url = trimRightSlash(base) + "/" + objectKey;
        return Map.of("url", url, "objectKey", objectKey);
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
}

