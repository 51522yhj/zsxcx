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
    private Boolean newProductNoticeEnabled = false;
    private String newProductTemplateId;
    private String newProductNoticeTitle = "新品上架";
    private String newProductNoticeRemark = "点击查看新品详情";
}
