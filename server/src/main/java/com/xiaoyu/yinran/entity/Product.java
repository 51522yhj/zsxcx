package com.xiaoyu.yinran.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("products")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long categoryId;
    private String summary;
    private String description;
    private String coverUrl;
    private String contactPhone;
    private String contactWechat;
    private String status;
    private Integer sortOrder;
    private Boolean carouselAutoplayEnabled;
    private Integer carouselIntervalSeconds;
    private String searchText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
