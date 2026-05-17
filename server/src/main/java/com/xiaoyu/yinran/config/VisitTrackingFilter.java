package com.xiaoyu.yinran.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class VisitTrackingFilter extends OncePerRequestFilter {
    private final JdbcTemplate jdbcTemplate;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/public/")
                || !"miniprogram".equalsIgnoreCase(request.getHeader("X-Client-Platform"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            recordVisit(request, response.getStatus(), System.currentTimeMillis() - startedAt);
        }
    }

    private void recordVisit(HttpServletRequest request, int status, long costMs) {
        try {
            String visitorId = visitorId(request);
            Integer existed = jdbcTemplate.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM visit_events WHERE visitor_id = ? LIMIT 1)",
                    Integer.class,
                    visitorId
            );
            boolean isNew = existed == null || existed == 0;
            jdbcTemplate.update("""
                            INSERT INTO visit_events
                            (visitor_id, source, path, method, client_ip, user_agent, referer, platform, status_code, cost_ms, is_new, visited_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    visitorId,
                    resolveSource(request),
                    requestPath(request),
                    request.getMethod(),
                    clientIp(request),
                    limit(request.getHeader("User-Agent"), 500),
                    limit(request.getHeader("Referer"), 500),
                    resolvePlatform(request),
                    status,
                    costMs,
                    isNew ? 1 : 0,
                    LocalDateTime.now()
            );
        } catch (Exception ignored) {
            // Monitoring must never affect user-facing APIs.
        }
    }

    private String resolveSource(HttpServletRequest request) {
        String explicitSource = firstText(request.getHeader("X-Client-Source"));
        if (StringUtils.hasText(explicitSource)) {
            return limit(explicitSource, 180);
        }
        return firstText(limit(request.getHeader("Referer"), 180), "unknown");
    }

    private String resolvePlatform(HttpServletRequest request) {
        String platform = firstText(request.getHeader("X-Client-Platform")).toLowerCase();
        if ("miniprogram".equals(platform)) {
            return platform;
        }
        return "unknown";
    }

    private String visitorId(HttpServletRequest request) {
        String provided = firstText(request.getHeader("X-Visitor-Id"), request.getHeader("X-WX-OPENID"));
        if (StringUtils.hasText(provided)) {
            return limit(provided, 128);
        }
        return sha256(firstText(clientIp(request), "") + "|" + firstText(request.getHeader("User-Agent"), ""));
    }

    private String requestPath(HttpServletRequest request) {
        String query = request.getQueryString();
        return request.getRequestURI() + (StringUtils.hasText(query) ? "?" + query : "");
    }

    private String clientIp(HttpServletRequest request) {
        return firstText(
                request.getHeader("X-Forwarded-For"),
                request.getHeader("X-Real-IP"),
                request.getRemoteAddr()
        ).split(",")[0].trim();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return String.valueOf(value.hashCode());
        }
    }
}
