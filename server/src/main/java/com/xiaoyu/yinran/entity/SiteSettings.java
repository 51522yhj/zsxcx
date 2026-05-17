package com.xiaoyu.yinran.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("site_settings")
public class SiteSettings {
    @TableId
    private Long id;
    private String siteName;
    private String contactPhone;
    private String contactWechat;
    private String logoUrl;
    private Boolean customerServiceEnabled;
    private String customerServiceText;
    private String homeSectionTitle;
    private Boolean newProductNoticeEnabled;
    private String newProductTemplateId;
    private String newProductNoticeTitle;
    private String newProductNoticeRemark;
    private LocalDateTime updatedAt;
}
