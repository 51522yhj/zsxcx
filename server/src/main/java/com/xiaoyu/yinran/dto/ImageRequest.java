package com.xiaoyu.yinran.dto;

import lombok.Data;

@Data
public class ImageRequest {
    private String mediaType = "IMAGE";
    private String imageUrl;
    private String objectKey;
    private String posterUrl;
    private Integer width;
    private Integer height;
    private Integer sortOrder = 0;
    private Boolean isCover = false;
    private Boolean showInDetail = true;
    private Integer detailSortOrder = 0;
}
