package com.xiaoyu.yinran.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubscriptionVO {
    private String openid;
    private Boolean newProductEnabled;
}
