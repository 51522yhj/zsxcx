package com.xiaoyu.yinran.controller.publicapi;

import com.xiaoyu.yinran.common.ApiResponse;
import com.xiaoyu.yinran.common.PageResult;
import com.xiaoyu.yinran.entity.Announcement;
import com.xiaoyu.yinran.entity.SiteSettings;
import com.xiaoyu.yinran.entity.Tag;
import com.xiaoyu.yinran.service.AnnouncementService;
import com.xiaoyu.yinran.service.CatalogService;
import com.xiaoyu.yinran.service.ProductService;
import com.xiaoyu.yinran.service.SiteService;
import com.xiaoyu.yinran.vo.CategoryVO;
import com.xiaoyu.yinran.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {
    private final SiteService siteService;
    private final CatalogService catalogService;
    private final AnnouncementService announcementService;
    private final ProductService productService;

    @GetMapping("/settings")
    public ApiResponse<SiteSettings> settings() {
        return ApiResponse.ok(siteService.getSettings());
    }

    @GetMapping("/categories/tree")
    public ApiResponse<List<CategoryVO>> categories() {
        return ApiResponse.ok(catalogService.categoryTree(true));
    }

    @GetMapping("/tags")
    public ApiResponse<List<Tag>> tags() {
        return ApiResponse.ok(catalogService.listTags(true));
    }

    @GetMapping("/announcements/active")
    public ApiResponse<List<Announcement>> announcements() {
        return ApiResponse.ok(announcementService.active());
    }

    @GetMapping("/products")
    public ApiResponse<PageResult<ProductVO>> products(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "20") long size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Long categoryId,
                                                       @RequestParam(required = false) Long tagId) {
        return ApiResponse.ok(productService.page(page, size, keyword, categoryId, tagId, null, true));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<ProductVO> product(@PathVariable Long id) {
        return ApiResponse.ok(productService.detail(id, true));
    }
}

