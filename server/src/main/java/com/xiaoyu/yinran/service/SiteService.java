package com.xiaoyu.yinran.service;

import com.xiaoyu.yinran.dto.SiteSettingsRequest;
import com.xiaoyu.yinran.entity.SiteSettings;
import com.xiaoyu.yinran.mapper.SiteSettingsMapper;
import com.xiaoyu.yinran.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SiteService {
    private final SiteSettingsMapper siteSettingsMapper;
    private final AppProperties appProperties;

    public SiteSettings getSettings() {
        SiteSettings settings = siteSettingsMapper.selectById(1L);
        if (settings == null) {
            settings = new SiteSettings();
            settings.setId(1L);
            settings.setSiteName("小于印染");
            settings.setCustomerServiceEnabled(true);
            settings.setCustomerServiceText("咨询客服");
            settings.setHomeSectionTitle("精选面料");
            siteSettingsMapper.insert(settings);
        }
        settings.setLogoUrl(resolveUploadUrl(settings.getLogoUrl()));
        return settings;
    }

    public SiteSettings update(SiteSettingsRequest request) {
        SiteSettings settings = getSettings();
        BeanUtils.copyProperties(request, settings);
        settings.setId(1L);
        siteSettingsMapper.updateById(settings);
        return getSettings();
    }

    private String resolveUploadUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        String base = appProperties.getPublicFileBaseUrl();
        if (!StringUtils.hasText(base)) {
            return url;
        }
        int uploadsIndex = url.indexOf("/uploads/");
        if (uploadsIndex >= 0) {
            url = url.substring(uploadsIndex + "/uploads/".length());
        } else if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        } else if (url.startsWith("/uploads/")) {
            url = url.substring("/uploads/".length());
        } else if (url.startsWith("uploads/")) {
            url = url.substring("uploads/".length());
        } else if (url.startsWith("/")) {
            url = url.substring(1);
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + url;
    }
}
