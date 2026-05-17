package com.xiaoyu.yinran.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductRequest {
    @NotBlank(message = "请输入商品名称")
    private String name;
    private Long categoryId;
    private String summary;
    private String description;
    private String coverUrl;
    private String status = "DRAFT";
    private Integer sortOrder = 0;
    private Boolean carouselAutoplayEnabled = true;
    private Integer carouselIntervalSeconds = 3;
    private List<Long> tagIds = new ArrayList<>();
    private List<ImageRequest> images = new ArrayList<>();
}
