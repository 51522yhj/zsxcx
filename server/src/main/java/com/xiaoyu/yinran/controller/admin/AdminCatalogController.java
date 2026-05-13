package com.xiaoyu.yinran.controller.admin;

import com.xiaoyu.yinran.common.ApiResponse;
import com.xiaoyu.yinran.dto.CategoryRequest;
import com.xiaoyu.yinran.dto.TagRequest;
import com.xiaoyu.yinran.entity.Category;
import com.xiaoyu.yinran.entity.Tag;
import com.xiaoyu.yinran.service.CatalogService;
import com.xiaoyu.yinran.vo.CategoryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCatalogController {
    private final CatalogService catalogService;

    @GetMapping("/categories")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.ok(catalogService.listCategories(false));
    }

    @GetMapping("/categories/tree")
    public ApiResponse<List<CategoryVO>> categoryTree() {
        return ApiResponse.ok(catalogService.categoryTree(false));
    }

    @PostMapping("/categories")
    public ApiResponse<Category> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(catalogService.saveCategory(null, request));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(catalogService.saveCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return ApiResponse.ok();
    }

    @GetMapping("/tags")
    public ApiResponse<List<Tag>> tags() {
        return ApiResponse.ok(catalogService.listTags(false));
    }

    @PostMapping("/tags")
    public ApiResponse<Tag> createTag(@Valid @RequestBody TagRequest request) {
        return ApiResponse.ok(catalogService.saveTag(null, request));
    }

    @PutMapping("/tags/{id}")
    public ApiResponse<Tag> updateTag(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
        return ApiResponse.ok(catalogService.saveTag(id, request));
    }

    @DeleteMapping("/tags/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable Long id) {
        catalogService.deleteTag(id);
        return ApiResponse.ok();
    }
}

