package com.xiaoyu.yinran.controller.admin;

import com.xiaoyu.yinran.common.ApiResponse;
import com.xiaoyu.yinran.dto.LoginRequest;
import com.xiaoyu.yinran.service.AuthService;
import com.xiaoyu.yinran.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}

