package com.xiaoyu.yinran.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceLoggingFilter extends OncePerRequestFilter {
    private static final String TRACE_ID = "traceId";
    private static final int MAX_BODY_LENGTH = 8000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = firstText(request.getHeader("X-Trace-Id"), UUID.randomUUID().toString().replace("-", ""));
        MDC.put(TRACE_ID, traceId);
        response.setHeader("X-Trace-Id", traceId);

        long startedAt = System.currentTimeMillis();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long costMs = System.currentTimeMillis() - startedAt;
            logRequestResponse(wrappedRequest, wrappedResponse, traceId, costMs);
            wrappedResponse.copyBodyToResponse();
            MDC.remove(TRACE_ID);
        }
    }

    private void logRequestResponse(ContentCachingRequestWrapper request,
                                    ContentCachingResponseWrapper response,
                                    String traceId,
                                    long costMs) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String requestUri = uri + (StringUtils.hasText(query) ? "?" + query : "");
        String clientIp = clientIp(request);
        String requestBody = requestBody(request);
        String responseBody = responseBody(response);

        log.info("HTTP request-response traceId={} method={} uri={} status={} costMs={} clientIp={} request={} response={}",
                traceId,
                method,
                requestUri,
                response.getStatus(),
                costMs,
                clientIp,
                requestBody,
                responseBody);
    }

    private String requestBody(HttpServletRequest request) {
        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("params", request.getParameterMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue()), (a, b) -> a, LinkedHashMap::new)));
            body.put("files", multipartRequest.getFileMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> fileInfo(e.getValue()), (a, b) -> a, LinkedHashMap::new)));
            return limit(mask(body.toString()));
        }

        ContentCachingRequestWrapper wrapper = request instanceof ContentCachingRequestWrapper caching
                ? caching
                : null;
        if (wrapper == null || wrapper.getContentAsByteArray().length == 0) {
            return "{}";
        }
        String contentType = request.getContentType();
        if (!isTextContent(contentType)) {
            return "[binary contentType=" + contentType + "]";
        }
        String body = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        return limit(mask(body));
    }

    private Map<String, Object> fileInfo(MultipartFile file) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("originalFilename", file.getOriginalFilename());
        info.put("contentType", file.getContentType());
        info.put("size", file.getSize());
        return info;
    }

    private String responseBody(ContentCachingResponseWrapper response) {
        byte[] bytes = response.getContentAsByteArray();
        if (bytes.length == 0) {
            return "";
        }
        String contentType = response.getContentType();
        if (!isTextContent(contentType)) {
            return "[binary contentType=" + contentType + ", size=" + bytes.length + "]";
        }
        return limit(mask(new String(bytes, StandardCharsets.UTF_8)));
    }

    private boolean isTextContent(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return true;
        }
        String lower = contentType.toLowerCase();
        return lower.contains(MediaType.APPLICATION_JSON_VALUE)
                || lower.contains(MediaType.TEXT_PLAIN_VALUE)
                || lower.contains(MediaType.TEXT_HTML_VALUE)
                || lower.contains(MediaType.APPLICATION_XML_VALUE)
                || lower.contains("javascript")
                || lower.contains("x-www-form-urlencoded");
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

    private String limit(String value) {
        if (value == null || value.length() <= MAX_BODY_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_BODY_LENGTH) + "...[truncated]";
    }

    private String mask(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replaceAll("(?i)(\"?(?:password|token|authorization|jwtSecret|secret|secretKey|secretId)\"?\\s*[:=]\\s*)\"?[^\"]+\"?", "$1\"***\"")
                .replaceAll("(?i)(Bearer\\s+)[A-Za-z0-9._\\-]+", "$1***");
    }
}
