package com.xiaoyu.yinran.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {
    private Long parentId;
    @NotBlank(message = "请输入分类名称")
    private String name;
    private String iconUrl;
    private String coverUrl;
    private Integer sortOrder = 0;
    private Boolean enabled = true;
}

