package com.xiaoyu.yinran.controller.admin;

import com.xiaoyu.yinran.common.ApiResponse;
import com.xiaoyu.yinran.dto.SiteSettingsRequest;
import com.xiaoyu.yinran.entity.SiteSettings;
import com.xiaoyu.yinran.service.SiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {
    private final SiteService siteService;

    @GetMapping
    public ApiResponse<SiteSettings> get() {
        return ApiResponse.ok(siteService.getSettings());
    }

    @PutMapping
    public ApiResponse<SiteSettings> update(@Valid @RequestBody SiteSettingsRequest request) {
        return ApiResponse.ok(siteService.update(request));
    }
}

