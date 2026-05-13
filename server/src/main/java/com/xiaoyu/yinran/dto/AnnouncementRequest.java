package com.xiaoyu.yinran.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementRequest {
    @NotBlank(message = "请输入公告标题")
    private String title;
    @NotBlank(message = "请输入滚动公告内容")
    private String tickerText;
    @NotBlank(message = "请输入公告正文")
    private String content;
    private String imageUrl;
    private Boolean enabled = true;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Integer sortOrder = 0;
}

