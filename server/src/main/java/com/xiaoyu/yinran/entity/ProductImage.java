package com.xiaoyu.yinran.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_images")
public class ProductImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String mediaType;
    private String imageUrl;
    private String objectKey;
    private String posterUrl;
    private Integer width;
    private Integer height;
    private Integer sortOrder;
    private Boolean isCover;
    private Boolean showInDetail;
    private Integer detailSortOrder;
    private LocalDateTime createdAt;
}
