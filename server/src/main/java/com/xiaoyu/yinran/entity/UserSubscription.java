package com.xiaoyu.yinran.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_subscriptions")
public class UserSubscription {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private Boolean newProductEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
