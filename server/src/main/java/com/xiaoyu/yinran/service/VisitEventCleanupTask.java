package com.xiaoyu.yinran.service;

import com.xiaoyu.yinran.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class VisitEventCleanupTask {
    private final JdbcTemplate jdbcTemplate;
    private final AppProperties appProperties;

    @Scheduled(cron = "0 20 3 * * ?", zone = "Asia/Shanghai")
    public void cleanup() {
        int retentionDays = Math.max(appProperties.getVisitEventRetentionDays(), 7);
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
            int deleted = jdbcTemplate.update("""
                    DELETE FROM visit_events
                    WHERE visited_at < ?
                    """, cutoff);
            log.info("Visit event cleanup completed. retentionDays={}, deleted={}", retentionDays, deleted);
        } catch (Exception ex) {
            log.warn("Visit event cleanup skipped. reason={}", ex.getMessage());
        }
    }
}
