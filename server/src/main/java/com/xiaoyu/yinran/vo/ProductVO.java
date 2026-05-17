package com.xiaoyu.yinran.vo;

import com.xiaoyu.yinran.entity.ProductImage;
import com.xiaoyu.yinran.entity.Tag;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductVO {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String summary;
    private String description;
    private String coverUrl;
    private String status;
    private Integer sortOrder;
    private Boolean carouselAutoplayEnabled;
    private Integer carouselIntervalSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Tag> tags = new ArrayList<>();
    private List<ProductImage> images = new ArrayList<>();
}
