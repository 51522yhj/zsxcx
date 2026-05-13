package com.xiaoyu.yinran.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String jwtSecret;
    private long jwtExpireHours;
    private String uploadRoot;
    private String publicFileBaseUrl;
    private String defaultAdminUsername;
    private String defaultAdminPassword;
}

