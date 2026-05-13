package com.xiaoyu.yinran.controller;

import com.xiaoyu.yinran.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/healthz")
    public ApiResponse<Map<String, String>> healthz() {
        return ApiResponse.ok(Map.of("status", "UP"));
    }
}

