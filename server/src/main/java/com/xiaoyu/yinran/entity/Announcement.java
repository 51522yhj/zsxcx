package com.xiaoyu.yinran.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("announcements")
public class Announcement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String tickerText;
    private String content;
    private String imageUrl;
    private Boolean enabled;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
