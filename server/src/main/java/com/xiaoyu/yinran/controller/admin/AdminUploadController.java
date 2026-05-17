package com.xiaoyu.yinran.controller.admin;

import com.xiaoyu.yinran.common.ApiResponse;
import com.xiaoyu.yinran.dto.DirectUploadRequest;
import com.xiaoyu.yinran.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
public class AdminUploadController {
    private final UploadService uploadService;

    @PostMapping("/image")
    public ApiResponse<Map<String, String>> image(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(uploadService.uploadImage(file));
    }

    @PostMapping("/video")
    public ApiResponse<Map<String, String>> video(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(uploadService.uploadVideo(file));
    }

    @PostMapping("/direct-url")
    public ApiResponse<Map<String, String>> directUrl(@RequestBody DirectUploadRequest request) {
        return ApiResponse.ok(uploadService.createDirectUploadUrl(request));
    }
}
