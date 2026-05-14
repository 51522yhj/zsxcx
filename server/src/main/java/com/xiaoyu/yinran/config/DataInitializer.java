package com.xiaoyu.yinran.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoyu.yinran.entity.Admin;
import com.xiaoyu.yinran.entity.Announcement;
import com.xiaoyu.yinran.entity.Category;
import com.xiaoyu.yinran.entity.Tag;
import com.xiaoyu.yinran.mapper.AdminMapper;
import com.xiaoyu.yinran.mapper.AnnouncementMapper;
import com.xiaoyu.yinran.mapper.CategoryMapper;
import com.xiaoyu.yinran.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final AppProperties appProperties;
    private final AdminMapper adminMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final AnnouncementMapper announcementMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        ensureSiteSettingsColumns();

        if (adminMapper.selectCount(null) == 0) {
            Admin admin = new Admin();
            admin.setUsername(appProperties.getDefaultAdminUsername());
            admin.setPasswordHash(passwordEncoder.encode(appProperties.getDefaultAdminPassword()));
            admin.setDisplayName("小于印染管理员");
            admin.setEnabled(true);
            adminMapper.insert(admin);
        }

        seedCategory("上衣", 10);
        seedCategory("裤子", 20);
        seedCategory("面料", 30);
        seedTag("棉麻", 10);
        seedTag("印花", 20);
        seedTag("直弹", 30);

        if (announcementMapper.selectCount(null) == 0) {
            Announcement announcement = new Announcement();
            announcement.setTitle("公告");
            announcement.setTickerText("欢迎来到小于印染，点击查看联系方式与最新通知");
            announcement.setContent("欢迎来到小于印染。可通过商品详情页咨询客服，或复制电话、微信号进一步沟通。");
            announcement.setEnabled(true);
            announcement.setSortOrder(1);
            announcementMapper.insert(announcement);
        }
    }

    private void ensureSiteSettingsColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE site_settings ADD COLUMN logo_url VARCHAR(500) NULL AFTER contact_wechat");
        } catch (Exception ignored) {
            // The column already exists in upgraded databases.
        }
    }

    private void seedCategory(String name, int sort) {
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<Category>().eq(Category::getName, name));
        if (count == 0) {
            Category category = new Category();
            category.setName(name);
            category.setSortOrder(sort);
            category.setEnabled(true);
            categoryMapper.insert(category);
        }
    }

    private void seedTag(String name, int sort) {
        Long count = tagMapper.selectCount(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name));
        if (count == 0) {
            Tag tag = new Tag();
            tag.setName(name);
            tag.setSortOrder(sort);
            tag.setEnabled(true);
            tagMapper.insert(tag);
        }
    }
}
