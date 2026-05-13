package com.xiaoyu.yinran.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoyu.yinran.dto.AnnouncementRequest;
import com.xiaoyu.yinran.entity.Announcement;
import com.xiaoyu.yinran.mapper.AnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final AnnouncementMapper announcementMapper;

    public List<Announcement> listAll() {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .orderByAsc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getId));
    }

    public List<Announcement> active() {
        LocalDateTime now = LocalDateTime.now();
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getEnabled, true)
                .and(w -> w.isNull(Announcement::getStartsAt).or().le(Announcement::getStartsAt, now))
                .and(w -> w.isNull(Announcement::getEndsAt).or().ge(Announcement::getEndsAt, now))
                .orderByAsc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getId));
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
        return announcement;
    }

    public void delete(Long id) {
        announcementMapper.deleteById(id);
    }
}

