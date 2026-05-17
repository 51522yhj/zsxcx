package com.xiaoyu.yinran.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyu.yinran.config.AppProperties;
import com.xiaoyu.yinran.entity.SiteSettings;
import com.xiaoyu.yinran.entity.UserSubscription;
import com.xiaoyu.yinran.mapper.UserSubscriptionMapper;
import com.xiaoyu.yinran.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewProductNotificationService {
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";

    private final AppProperties appProperties;
    private final SiteService siteService;
    private final UserSubscriptionMapper userSubscriptionMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void notifyNewProduct(ProductVO product) {
        SiteSettings settings = siteService.getSettings();
        String templateId = firstText(settings.getNewProductTemplateId(), appProperties.getNewProductTemplateId());
        if (!Boolean.TRUE.equals(settings.getNewProductNoticeEnabled()) || !StringUtils.hasText(templateId)) {
            log.info("Skip new product notice, switch/template not configured. productId={}", product.getId());
            return;
        }
        if (!StringUtils.hasText(appProperties.getWechatAppId()) || !StringUtils.hasText(appProperties.getWechatAppSecret())) {
            log.info("Skip new product notice, WECHAT_APP_ID/WECHAT_APP_SECRET not configured. productId={}", product.getId());
            return;
        }

        List<UserSubscription> users = userSubscriptionMapper.selectList(new LambdaQueryWrapper<UserSubscription>()
                .eq(UserSubscription::getNewProductEnabled, true));
        if (users.isEmpty()) {
            log.info("Skip new product notice, no subscribed users. productId={}", product.getId());
            return;
        }

        log.info("Sending new product notice. productId={}, subscribedUsers={}", product.getId(), users.size());
        String accessToken = accessToken();
        for (UserSubscription user : users) {
            try {
                sendOne(accessToken, templateId, user.getOpenid(), settings, product);
            } catch (Exception ex) {
                log.warn("Send new product notice failed. openid={}, productId={}, reason={}",
                        user.getOpenid(), product.getId(), ex.getMessage());
            }
        }
    }

    private void sendOne(String accessToken, String templateId, String openid, SiteSettings settings, ProductVO product) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("touser", openid);
        body.put("template_id", templateId);
        body.put("page", "pages/detail/detail?id=" + product.getId());
        body.put("miniprogram_state", appProperties.getWechatMiniprogramState());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("thing1", Map.of("value", truncate(product.getName(), 20)));
        data.put("thing3", Map.of("value", truncate(firstText(settings.getNewProductNoticeRemark(), "点击查看新品详情"), 20)));
        body.put("data", data);

        String url = UriComponentsBuilder.fromHttpUrl(SEND_URL)
                .queryParam("access_token", accessToken)
                .toUriString();
        HttpResponse<String> response = postSubscribeMessage(url, body);
        String responseBody = response.body();
        log.info("New product notice response. openid={}, productId={}, httpStatus={}, body={}",
                openid, product.getId(), response.statusCode(), responseBody);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("微信接口HTTP状态异常: " + response.statusCode()
                    + ", body=" + firstText(responseBody, "empty"));
        }
        JsonNode json = objectMapper.readTree(responseBody);
        if (json.path("errcode").asInt(0) != 0) {
            throw new IllegalStateException(json.toString());
        }
        log.info("Send new product notice success. openid={}, productId={}, response={}", openid, product.getId(), json);
    }

    private HttpResponse<String> postSubscribeMessage(String url, Map<String, Object> body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .header("User-Agent", "xiaoyu-yinran-server/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String accessToken() {
        String url = UriComponentsBuilder.fromHttpUrl(TOKEN_URL)
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", appProperties.getWechatAppId())
                .queryParam("secret", appProperties.getWechatAppSecret())
                .toUriString();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            String token = json.path("access_token").asText("");
            if (!StringUtils.hasText(token)) {
                throw new IllegalStateException(json.path("errmsg").asText("获取 access_token 失败"));
            }
            return token;
        } catch (Exception ex) {
            throw new IllegalStateException("获取 access_token 失败: " + ex.getMessage(), ex);
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
