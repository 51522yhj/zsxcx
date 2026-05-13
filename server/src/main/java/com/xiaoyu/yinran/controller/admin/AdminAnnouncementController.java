package com.xiaoyu.yinran.controller.admin;

import com.xiaoyu.yinran.common.ApiResponse;
import com.xiaoyu.yinran.dto.AnnouncementRequest;
import com.xiaoyu.yinran.entity.Announcement;
import com.xiaoyu.yinran.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {
    private final AnnouncementService announcementService;

    @GetMapping
    public ApiResponse<List<Announcement>> list() {
        return ApiResponse.ok(announcementService.listAll());
    }

    @PostMapping
    public ApiResponse<Announcement> create(@Valid @RequestBody AnnouncementRequest request) {
        return ApiResponse.ok(announcementService.save(null, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Announcement> update(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest request) {
        return ApiResponse.ok(announcementService.save(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return ApiResponse.ok();
    }
}

