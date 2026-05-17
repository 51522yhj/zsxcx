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
        ensureRuntimeSchema();

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
        seedTag("布料", 10);
        seedTag("棉麻", 20);
        seedTag("印花", 30);
        seedTag("直弹", 40);

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

    private void ensureRuntimeSchema() {
        executeIgnoreError("ALTER TABLE site_settings ADD COLUMN logo_url VARCHAR(500) NULL AFTER contact_wechat");
        executeIgnoreError("ALTER TABLE site_settings ADD COLUMN new_product_notice_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER home_section_title");
        executeIgnoreError("ALTER TABLE site_settings ADD COLUMN new_product_template_id VARCHAR(120) NULL AFTER new_product_notice_enabled");
        executeIgnoreError("ALTER TABLE site_settings ADD COLUMN new_product_notice_title VARCHAR(80) NOT NULL DEFAULT '新品上架' AFTER new_product_template_id");
        executeIgnoreError("ALTER TABLE site_settings ADD COLUMN new_product_notice_remark VARCHAR(160) NOT NULL DEFAULT '点击查看新品详情' AFTER new_product_notice_title");

        executeIgnoreError("ALTER TABLE products ADD COLUMN carousel_autoplay_enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER sort_order");
        executeIgnoreError("ALTER TABLE products ADD COLUMN carousel_interval_seconds INT NOT NULL DEFAULT 3 AFTER carousel_autoplay_enabled");
        executeIgnoreError("UPDATE products SET carousel_autoplay_enabled = 1 WHERE carousel_autoplay_enabled IS NULL");
        executeIgnoreError("UPDATE products SET carousel_interval_seconds = 3 WHERE carousel_interval_seconds IS NULL OR carousel_interval_seconds < 1");

        executeIgnoreError("ALTER TABLE product_images ADD COLUMN media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE' AFTER product_id");
        executeIgnoreError("ALTER TABLE product_images ADD COLUMN poster_url VARCHAR(500) NULL AFTER object_key");
        executeIgnoreError("ALTER TABLE product_images ADD COLUMN show_in_detail TINYINT(1) NOT NULL DEFAULT 1 AFTER is_cover");
        executeIgnoreError("ALTER TABLE product_images ADD COLUMN detail_sort_order INT NOT NULL DEFAULT 0 AFTER show_in_detail");
        executeIgnoreError("UPDATE product_images SET media_type = 'IMAGE' WHERE media_type IS NULL OR media_type = ''");
        executeIgnoreError("UPDATE product_images SET show_in_detail = 1 WHERE show_in_detail IS NULL");
        executeIgnoreError("UPDATE product_images SET detail_sort_order = sort_order WHERE detail_sort_order IS NULL OR detail_sort_order = 0");

        executeIgnoreError("""
                CREATE TABLE IF NOT EXISTS user_subscriptions (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  openid VARCHAR(120) NOT NULL UNIQUE,
                  new_product_enabled TINYINT(1) NOT NULL DEFAULT 0,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  INDEX idx_new_product_enabled(new_product_enabled)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        executeIgnoreError("""
                CREATE TABLE IF NOT EXISTS visit_events (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  visitor_id VARCHAR(128) NOT NULL,
                  source VARCHAR(180) NOT NULL DEFAULT 'unknown',
                  path VARCHAR(500) NOT NULL,
                  method VARCHAR(12) NOT NULL,
                  client_ip VARCHAR(80) NULL,
                  user_agent VARCHAR(500) NULL,
                  referer VARCHAR(500) NULL,
                  platform VARCHAR(40) NULL,
                  status_code INT NULL,
                  cost_ms BIGINT NULL,
                  is_new TINYINT(1) NOT NULL DEFAULT 0,
                  visited_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  INDEX idx_visited_at(visited_at),
                  INDEX idx_visitor(visitor_id),
                  INDEX idx_source_time(source, visited_at),
                  INDEX idx_path_time(path, visited_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void executeIgnoreError(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Existing production databases are upgraded column-by-column.
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
