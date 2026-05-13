package com.xiaoyu.yinran.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryVO {
    private Long id;
    private Long parentId;
    private String name;
    private String iconUrl;
    private String coverUrl;
    private Integer sortOrder;
    private Boolean enabled;
    private List<CategoryVO> children = new ArrayList<>();
}

