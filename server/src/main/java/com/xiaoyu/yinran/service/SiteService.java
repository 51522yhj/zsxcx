package com.xiaoyu.yinran.service;

import com.xiaoyu.yinran.dto.SiteSettingsRequest;
import com.xiaoyu.yinran.entity.SiteSettings;
import com.xiaoyu.yinran.mapper.SiteSettingsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteService {
    private final SiteSettingsMapper siteSettingsMapper;

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
        return settings;
    }

    public SiteSettings update(SiteSettingsRequest request) {
        SiteSettings settings = getSettings();
        BeanUtils.copyProperties(request, settings);
        settings.setId(1L);
        siteSettingsMapper.updateById(settings);
        return getSettings();
    }
}

