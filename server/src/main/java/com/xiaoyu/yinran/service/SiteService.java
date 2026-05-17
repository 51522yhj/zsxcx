package com.xiaoyu.yinran.service;

import com.xiaoyu.yinran.config.AppProperties;
import com.xiaoyu.yinran.dto.SiteSettingsRequest;
import com.xiaoyu.yinran.entity.SiteSettings;
import com.xiaoyu.yinran.mapper.SiteSettingsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SiteService {
    private final SiteSettingsMapper siteSettingsMapper;
    private final UploadService uploadService;
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
            settings.setNewProductNoticeEnabled(false);
            settings.setNewProductNoticeTitle("新品上架");
            settings.setNewProductNoticeRemark("点击查看新品详情");
            siteSettingsMapper.insert(settings);
        }
        fillDefaults(settings);
        settings.setLogoUrl(uploadService.resolveFileUrl(settings.getLogoUrl()));
        return settings;
    }

    public SiteSettings update(SiteSettingsRequest request) {
        SiteSettings settings = getSettings();
        BeanUtils.copyProperties(request, settings);
        settings.setId(1L);
        fillDefaults(settings);
        siteSettingsMapper.updateById(settings);
        return getSettings();
    }

    private void fillDefaults(SiteSettings settings) {
        boolean filledTemplateFromConfig = false;
        if (!StringUtils.hasText(settings.getNewProductTemplateId())
                && StringUtils.hasText(appProperties.getNewProductTemplateId())) {
            settings.setNewProductTemplateId(appProperties.getNewProductTemplateId());
            filledTemplateFromConfig = true;
        }
        if (!StringUtils.hasText(settings.getSiteName())) {
            settings.setSiteName("小于印染");
        }
        if (settings.getCustomerServiceEnabled() == null) {
            settings.setCustomerServiceEnabled(true);
        }
        if (!StringUtils.hasText(settings.getCustomerServiceText())) {
            settings.setCustomerServiceText("咨询客服");
        }
        if (!StringUtils.hasText(settings.getHomeSectionTitle())) {
            settings.setHomeSectionTitle("精选面料");
        }
        if (settings.getNewProductNoticeEnabled() == null
                || (filledTemplateFromConfig && !Boolean.TRUE.equals(settings.getNewProductNoticeEnabled()))) {
            settings.setNewProductNoticeEnabled(StringUtils.hasText(settings.getNewProductTemplateId()));
        }
        if (!StringUtils.hasText(settings.getNewProductNoticeTitle())) {
            settings.setNewProductNoticeTitle("新品上架");
        }
        if (!StringUtils.hasText(settings.getNewProductNoticeRemark())) {
            settings.setNewProductNoticeRemark("点击查看新品详情");
        }
    }

}
