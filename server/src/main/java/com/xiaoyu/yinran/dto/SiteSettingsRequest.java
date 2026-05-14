package com.xiaoyu.yinran.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SiteSettingsRequest {
    @NotBlank(message = "请输入小程序名称")
    private String siteName;
    private String contactPhone;
    private String contactWechat;
    private String logoUrl;
    private Boolean customerServiceEnabled = true;
    private String customerServiceText = "咨询客服";
    private String homeSectionTitle = "精选面料";
}
