package com.xiaoyu.yinran.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("product_tags")
public class ProductTag {
    @TableId
    private Long productId;
    private Long tagId;
}
