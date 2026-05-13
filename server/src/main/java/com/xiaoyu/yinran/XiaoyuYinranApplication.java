package com.xiaoyu.yinran;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.xiaoyu.yinran.mapper")
public class XiaoyuYinranApplication {
    public static void main(String[] args) {
        SpringApplication.run(XiaoyuYinranApplication.class, args);
    }
}

