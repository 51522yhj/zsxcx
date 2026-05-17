package com.xiaoyu.yinran.controller.publicapi;

import com.xiaoyu.yinran.common.ApiResponse;
import com.xiaoyu.yinran.dto.SubscriptionRequest;
import com.xiaoyu.yinran.dto.WechatCodeRequest;
import com.xiaoyu.yinran.service.SubscriptionService;
import com.xiaoyu.yinran.vo.SubscriptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/subscription")
@RequiredArgsConstructor
public class PublicSubscriptionController {
    private final SubscriptionService subscriptionService;

    @PostMapping("/openid")
    public ApiResponse<SubscriptionVO> openid(@RequestBody WechatCodeRequest request) {
        return ApiResponse.ok(subscriptionService.bindByCode(request.getCode()));
    }

    @PostMapping("/new-product")
    public ApiResponse<SubscriptionVO> newProduct(@RequestBody SubscriptionRequest request) {
        return ApiResponse.ok(subscriptionService.updateNewProduct(request.getOpenid(), request.getEnabled()));
    }

    @GetMapping("/status")
    public ApiResponse<SubscriptionVO> status(@RequestParam String openid) {
        return ApiResponse.ok(subscriptionService.status(openid));
    }
}
