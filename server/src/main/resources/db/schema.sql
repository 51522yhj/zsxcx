CREATE TABLE IF NOT EXISTS admins (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(120) NOT NULL,
  display_name VARCHAR(64) NOT NULL DEFAULT '管理员',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  last_login_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NULL,
  name VARCHAR(80) NOT NULL,
  icon_url VARCHAR(500) NULL,
  cover_url VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_parent_sort(parent_id, sort_order),
  INDEX idx_enabled(enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(60) NOT NULL UNIQUE,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  category_id BIGINT NULL,
  summary VARCHAR(255) NULL,
  description TEXT NULL,
  cover_url VARCHAR(500) NULL,
  contact_phone VARCHAR(40) NULL,
  contact_wechat VARCHAR(80) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  sort_order INT NOT NULL DEFAULT 0,
  carousel_autoplay_enabled TINYINT(1) NOT NULL DEFAULT 1,
  carousel_interval_seconds INT NOT NULL DEFAULT 3,
  search_text TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_category_status(category_id, status),
  INDEX idx_status_sort(status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_images (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
  image_url VARCHAR(500) NOT NULL,
  object_key VARCHAR(500) NULL,
  poster_url VARCHAR(500) NULL,
  width INT NULL,
  height INT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  is_cover TINYINT(1) NOT NULL DEFAULT 0,
  show_in_detail TINYINT(1) NOT NULL DEFAULT 1,
  detail_sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_product_sort(product_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_tags (
  product_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  PRIMARY KEY(product_id, tag_id),
  INDEX idx_tag(tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS announcements (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  ticker_text VARCHAR(180) NOT NULL,
  content TEXT NOT NULL,
  image_url VARCHAR(500) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  starts_at DATETIME NULL,
  ends_at DATETIME NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_enabled_time(enabled, starts_at, ends_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS site_settings (
  id BIGINT PRIMARY KEY,
  site_name VARCHAR(80) NOT NULL DEFAULT '小于印染',
  contact_phone VARCHAR(40) NULL,
  contact_wechat VARCHAR(80) NULL,
  logo_url VARCHAR(500) NULL,
  customer_service_enabled TINYINT(1) NOT NULL DEFAULT 1,
  customer_service_text VARCHAR(40) NOT NULL DEFAULT '咨询客服',
  home_section_title VARCHAR(80) NOT NULL DEFAULT '精选面料',
  new_product_notice_enabled TINYINT(1) NOT NULL DEFAULT 0,
  new_product_template_id VARCHAR(120) NULL,
  new_product_notice_title VARCHAR(80) NOT NULL DEFAULT '新品上架',
  new_product_notice_remark VARCHAR(160) NOT NULL DEFAULT '点击查看新品详情',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_subscriptions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  openid VARCHAR(120) NOT NULL UNIQUE,
  new_product_enabled TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_new_product_enabled(new_product_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO site_settings (id, site_name, customer_service_enabled, customer_service_text, home_section_title)
VALUES (1, '小于印染', 1, '咨询客服', '精选面料')
ON DUPLICATE KEY UPDATE id = id;
