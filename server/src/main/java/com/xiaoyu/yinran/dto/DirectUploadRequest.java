package com.xiaoyu.yinran.dto;

import lombok.Data;

@Data
public class DirectUploadRequest {
    private String mediaType = "IMAGE";
    private String filename;
    private String contentType;
    private Long size;
}
