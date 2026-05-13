package com.xiaoyu.yinran.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TagRequest {
    @NotBlank(message = "请输入标签名称")
    private String name;
    private Integer sortOrder = 0;
    private Boolean enabled = true;
}

