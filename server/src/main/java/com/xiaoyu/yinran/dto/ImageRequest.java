package com.xiaoyu.yinran.dto;

import lombok.Data;

@Data
public class ImageRequest {
    private String imageUrl;
    private String objectKey;
    private Integer width;
    private Integer height;
    private Integer sortOrder = 0;
    private Boolean isCover = false;
}

