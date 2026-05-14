package com.xiaoyu.yinran.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoyu.yinran.config.AppProperties;
import com.xiaoyu.yinran.dto.AnnouncementRequest;
import com.xiaoyu.yinran.entity.Announcement;
import com.xiaoyu.yinran.mapper.AnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final AnnouncementMapper announcementMapper;
    private final AppProperties appProperties;

    public List<Announcement> listAll() {
        List<Announcement> announcements = announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .orderByAsc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getId));
        announcements.forEach(this::resolveImage);
        return announcements;
    }

    public List<Announcement> active() {
        LocalDateTime now = LocalDateTime.now();
        List<Announcement> announcements = announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getEnabled, true)
                .and(w -> w.isNull(Announcement::getStartsAt).or().le(Announcement::getStartsAt, now))
                .and(w -> w.isNull(Announcement::getEndsAt).or().ge(Announcement::getEndsAt, now))
                .orderByAsc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getId));
        announcements.forEach(this::resolveImage);
        return announcements;
    }

    public Announcement save(Long id, AnnouncementRequest request) {
        Announcement announcement = id == null ? new Announcement() : announcementMapper.selectById(id);
        if (announcement == null) {
            throw new IllegalArgumentException("公告不存在");
        }
        BeanUtils.copyProperties(request, announcement);
        if (id == null) {
            announcementMapper.insert(announcement);
        } else {
            announcementMapper.updateById(announcement);
        }
        resolveImage(announcement);
        return announcement;
    }

    public void delete(Long id) {
        announcementMapper.deleteById(id);
    }

    private void resolveImage(Announcement announcement) {
        announcement.setImageUrl(resolveUploadUrl(announcement.getImageUrl()));
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
