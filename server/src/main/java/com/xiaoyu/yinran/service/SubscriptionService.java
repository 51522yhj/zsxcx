package com.xiaoyu.yinran.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyu.yinran.config.AppProperties;
import com.xiaoyu.yinran.entity.UserSubscription;
import com.xiaoyu.yinran.mapper.UserSubscriptionMapper;
import com.xiaoyu.yinran.vo.SubscriptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private static final String SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final AppProperties appProperties;
    private final UserSubscriptionMapper userSubscriptionMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public SubscriptionVO bindByCode(String code) {
        String openid = exchangeOpenid(code);
        UserSubscription subscription = findOrCreate(openid);
        return new SubscriptionVO(subscription.getOpenid(), Boolean.TRUE.equals(subscription.getNewProductEnabled()));
    }

    public SubscriptionVO updateNewProduct(String openid, Boolean enabled) {
        if (!StringUtils.hasText(openid)) {
            throw new IllegalArgumentException("openid 不能为空");
        }
        UserSubscription subscription = findOrCreate(openid);
        subscription.setNewProductEnabled(Boolean.TRUE.equals(enabled));
        userSubscriptionMapper.updateById(subscription);
        return new SubscriptionVO(subscription.getOpenid(), Boolean.TRUE.equals(subscription.getNewProductEnabled()));
    }

    public SubscriptionVO status(String openid) {
        UserSubscription subscription = findOrCreate(openid);
        return new SubscriptionVO(subscription.getOpenid(), Boolean.TRUE.equals(subscription.getNewProductEnabled()));
    }

    private UserSubscription findOrCreate(String openid) {
        if (!StringUtils.hasText(openid)) {
            openid = "local-" + UUID.randomUUID();
        }
        UserSubscription subscription = userSubscriptionMapper.selectOne(new LambdaQueryWrapper<UserSubscription>()
                .eq(UserSubscription::getOpenid, openid)
                .last("LIMIT 1"));
        if (subscription == null) {
            subscription = new UserSubscription();
            subscription.setOpenid(openid);
            subscription.setNewProductEnabled(false);
            userSubscriptionMapper.insert(subscription);
        }
        return subscription;
    }

    private String exchangeOpenid(String code) {
        if (!StringUtils.hasText(appProperties.getWechatAppId())
                || !StringUtils.hasText(appProperties.getWechatAppSecret())
                || !StringUtils.hasText(code)) {
            return "local-" + (StringUtils.hasText(code) ? code : UUID.randomUUID());
        }
        String url = UriComponentsBuilder.fromHttpUrl(SESSION_URL)
                .queryParam("appid", appProperties.getWechatAppId())
                .queryParam("secret", appProperties.getWechatAppSecret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            String openid = json.path("openid").asText("");
            if (!StringUtils.hasText(openid)) {
                throw new IllegalStateException(json.path("errmsg").asText("微信登录换取 openid 失败"));
            }
            return openid;
        } catch (Exception ex) {
            throw new IllegalStateException("微信登录换取 openid 失败：" + ex.getMessage(), ex);
        }
    }
}
