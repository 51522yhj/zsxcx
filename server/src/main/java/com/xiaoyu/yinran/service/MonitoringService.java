package com.xiaoyu.yinran.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MonitoringService {
    private final JdbcTemplate jdbcTemplate;
    private static final String MINI_PROGRAM_PLATFORM = "miniprogram";

    public Map<String, Object> overview(int days) {
        ensureSchema();
        int rangeDays = Math.min(Math.max(days, 7), 90);
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime trendStart = today.minusDays(rangeDays - 1L).atStartOfDay();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("platform", MINI_PROGRAM_PLATFORM);
        data.put("today", summary(todayStart));
        data.put("week", summary(weekStart));
        data.put("month", summary(monthStart));
        data.put("trend", trend(trendStart));
        data.put("sources", sources(trendStart));
        data.put("paths", paths(trendStart));
        data.put("recent", recent());
        return data;
    }

    private Map<String, Object> summary(LocalDateTime start) {
        return jdbcTemplate.queryForMap("""
                SELECT
                  COUNT(*) AS visits,
                  COUNT(DISTINCT visitor_id) AS users,
                  COALESCE(SUM(CASE WHEN UPPER(CAST(is_new AS CHAR)) IN ('1', 'Y', 'TRUE') THEN 1 ELSE 0 END), 0) AS newUsers,
                  COALESCE(ROUND(AVG(cost_ms)), 0) AS avgCostMs
                FROM visit_events
                WHERE visited_at >= ?
                  AND platform = ?
                """, start, MINI_PROGRAM_PLATFORM);
    }

    private List<Map<String, Object>> trend(LocalDateTime start) {
        return jdbcTemplate.queryForList("""
                SELECT
                  DATE_FORMAT(visited_at, '%Y-%m-%d') AS label,
                  COUNT(*) AS visits,
                  COUNT(DISTINCT visitor_id) AS users,
                  COALESCE(SUM(CASE WHEN UPPER(CAST(is_new AS CHAR)) IN ('1', 'Y', 'TRUE') THEN 1 ELSE 0 END), 0) AS newUsers
                FROM visit_events
                WHERE visited_at >= ?
                  AND platform = ?
                GROUP BY DATE_FORMAT(visited_at, '%Y-%m-%d')
                ORDER BY DATE_FORMAT(visited_at, '%Y-%m-%d')
                """, start, MINI_PROGRAM_PLATFORM);
    }

    private List<Map<String, Object>> sources(LocalDateTime start) {
        return jdbcTemplate.queryForList("""
                SELECT
                  COALESCE(NULLIF(source, ''), 'unknown') AS source,
                  COUNT(*) AS visits,
                  COUNT(DISTINCT visitor_id) AS users,
                  COALESCE(SUM(CASE WHEN UPPER(CAST(is_new AS CHAR)) IN ('1', 'Y', 'TRUE') THEN 1 ELSE 0 END), 0) AS newUsers
                FROM visit_events
                WHERE visited_at >= ?
                  AND platform = ?
                GROUP BY COALESCE(NULLIF(source, ''), 'unknown')
                ORDER BY COUNT(*) DESC
                LIMIT 10
                """, start, MINI_PROGRAM_PLATFORM);
    }

    private List<Map<String, Object>> paths(LocalDateTime start) {
        return jdbcTemplate.queryForList("""
                SELECT
                  path,
                  COUNT(*) AS visits,
                  COUNT(DISTINCT visitor_id) AS users,
                  COALESCE(ROUND(AVG(cost_ms)), 0) AS avgCostMs
                FROM visit_events
                WHERE visited_at >= ?
                  AND platform = ?
                GROUP BY path
                ORDER BY COUNT(*) DESC
                LIMIT 10
                """, start, MINI_PROGRAM_PLATFORM);
    }

    private List<Map<String, Object>> recent() {
        return jdbcTemplate.queryForList("""
                SELECT
                  DATE_FORMAT(visited_at, '%Y-%m-%d %H:%i:%s') AS visitedAt,
                  source,
                  path,
                  client_ip AS clientIp,
                  platform,
                  status_code AS statusCode,
                  cost_ms AS costMs,
                  CASE WHEN UPPER(CAST(is_new AS CHAR)) IN ('1', 'Y', 'TRUE') THEN 1 ELSE 0 END AS isNew
                FROM visit_events
                WHERE 1 = 1
                  AND platform = ?
                ORDER BY visited_at DESC
                LIMIT 20
                """, MINI_PROGRAM_PLATFORM);
    }

    private void ensureSchema() {
        jdbcTemplate.execute("""
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
}
