package com.xiaoyu.yinran.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoyu.yinran.dto.LoginRequest;
import com.xiaoyu.yinran.entity.Admin;
import com.xiaoyu.yinran.mapper.AdminMapper;
import com.xiaoyu.yinran.security.JwtService;
import com.xiaoyu.yinran.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginVO login(LoginRequest request) {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, request.getUsername())
                .last("LIMIT 1"));
        if (admin == null || !Boolean.TRUE.equals(admin.getEnabled())) {
            throw new IllegalArgumentException("账号不存在或已停用");
        }
        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        admin.setLastLoginAt(LocalDateTime.now());
        adminMapper.updateById(admin);
        return new LoginVO(jwtService.generateToken(admin.getId(), admin.getUsername()), admin.getUsername(), admin.getDisplayName());
    }
}

