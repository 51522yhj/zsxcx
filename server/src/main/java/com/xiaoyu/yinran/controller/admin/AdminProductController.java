package com.xiaoyu.yinran.controller.admin;

import com.xiaoyu.yinran.common.ApiResponse;
import com.xiaoyu.yinran.common.PageResult;
import com.xiaoyu.yinran.dto.ProductRequest;
import com.xiaoyu.yinran.service.ProductService;
import com.xiaoyu.yinran.vo.ProductVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {
    private final ProductService productService;

    @GetMapping
    public ApiResponse<PageResult<ProductVO>> page(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Long categoryId,
                                                   @RequestParam(required = false) Long tagId,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String sort) {
        return ApiResponse.ok(productService.page(page, size, keyword, categoryId, tagId, status, false, sort));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.detail(id, false));
    }

    @PostMapping
    public ApiResponse<ProductVO> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.save(null, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductVO> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.save(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ProductVO> status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(productService.updateStatus(id, body.getOrDefault("status", "DRAFT")));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.ok();
    }
}
