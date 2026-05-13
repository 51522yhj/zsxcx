package com.xiaoyu.yinran.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoyu.yinran.common.PageResult;
import com.xiaoyu.yinran.dto.ImageRequest;
import com.xiaoyu.yinran.dto.ProductRequest;
import com.xiaoyu.yinran.entity.Category;
import com.xiaoyu.yinran.entity.Product;
import com.xiaoyu.yinran.entity.ProductImage;
import com.xiaoyu.yinran.entity.ProductTag;
import com.xiaoyu.yinran.entity.Tag;
import com.xiaoyu.yinran.mapper.CategoryMapper;
import com.xiaoyu.yinran.mapper.ProductImageMapper;
import com.xiaoyu.yinran.mapper.ProductMapper;
import com.xiaoyu.yinran.mapper.ProductTagMapper;
import com.xiaoyu.yinran.mapper.TagMapper;
import com.xiaoyu.yinran.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductTagMapper productTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final com.xiaoyu.yinran.config.AppProperties appProperties;

    public PageResult<ProductVO> page(long page, long size, String keyword, Long categoryId, Long tagId, String status, boolean publicOnly) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .orderByDesc(Product::getSortOrder)
                .orderByDesc(Product::getId);
        if (publicOnly) {
            wrapper.eq(Product::getStatus, "PUBLISHED");
        } else if (StringUtils.hasText(status)) {
            wrapper.eq(Product::getStatus, status);
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getSearchText, keyword.trim());
        }
        if (tagId != null) {
            List<Long> productIds = productTagMapper.selectList(new LambdaQueryWrapper<ProductTag>()
                            .eq(ProductTag::getTagId, tagId))
                    .stream()
                    .map(ProductTag::getProductId)
                    .distinct()
                    .collect(Collectors.toList());
            if (productIds.isEmpty()) {
                return new PageResult<>(0, page, size, Collections.emptyList());
            }
            wrapper.in(Product::getId, productIds);
        }

        Page<Product> result = productMapper.selectPage(Page.of(page, size), wrapper);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), toVOList(result.getRecords()));
    }

    public ProductVO detail(Long id, boolean publicOnly) {
        Product product = productMapper.selectById(id);
        if (product == null || (publicOnly && !"PUBLISHED".equals(product.getStatus()))) {
            throw new IllegalArgumentException("商品不存在或已下架");
        }
        return toVO(product);
    }

    public ProductVO save(Long id, ProductRequest request) {
        Product product = id == null ? new Product() : productMapper.selectById(id);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        BeanUtils.copyProperties(request, product, "images", "tagIds");
        product.setContactPhone(null);
        product.setContactWechat(null);
        product.setSearchText(buildSearchText(request));
        if (!StringUtils.hasText(product.getCoverUrl()) && request.getImages() != null && !request.getImages().isEmpty()) {
            product.setCoverUrl(request.getImages().get(0).getImageUrl());
        }
        if (!StringUtils.hasText(product.getStatus())) {
            product.setStatus("DRAFT");
        }
        if (product.getSortOrder() == null) {
            product.setSortOrder(0);
        }
        if (id == null) {
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
        }
        replaceImages(product.getId(), request.getImages());
        replaceTags(product.getId(), request.getTagIds());
        return detail(product.getId(), false);
    }

    public void delete(Long id) {
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, id));
        productTagMapper.delete(new LambdaQueryWrapper<ProductTag>().eq(ProductTag::getProductId, id));
        productMapper.deleteById(id);
    }

    public ProductVO updateStatus(Long id, String status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        product.setStatus(status);
        productMapper.updateById(product);
        return detail(id, false);
    }

    private void replaceImages(Long productId, List<ImageRequest> images) {
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, productId));
        if (images == null) {
            return;
        }
        int index = 0;
        for (ImageRequest request : images) {
            if (!StringUtils.hasText(request.getImageUrl())) {
                continue;
            }
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setImageUrl(request.getImageUrl());
            image.setObjectKey(request.getObjectKey());
            image.setWidth(request.getWidth());
            image.setHeight(request.getHeight());
            image.setSortOrder(request.getSortOrder() == null ? index : request.getSortOrder());
            image.setIsCover(Boolean.TRUE.equals(request.getIsCover()) || index == 0);
            productImageMapper.insert(image);
            index++;
        }
    }

    private void replaceTags(Long productId, List<Long> tagIds) {
        productTagMapper.delete(new LambdaQueryWrapper<ProductTag>().eq(ProductTag::getProductId, productId));
        if (tagIds == null) {
            return;
        }
        tagIds.stream().filter(Objects::nonNull).distinct().forEach(tagId -> {
            ProductTag productTag = new ProductTag();
            productTag.setProductId(productId);
            productTag.setTagId(tagId);
            productTagMapper.insert(productTag);
        });
    }

    private String buildSearchText(ProductRequest request) {
        List<String> parts = new ArrayList<>();
        parts.add(request.getName());
        parts.add(request.getSummary());
        parts.add(request.getDescription());
        if (request.getCategoryId() != null) {
            Category category = categoryMapper.selectById(request.getCategoryId());
            if (category != null) {
                parts.add(category.getName());
            }
        }
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tagMapper.selectBatchIds(request.getTagIds()).forEach(tag -> parts.add(tag.getName()));
        }
        return parts.stream().filter(StringUtils::hasText).collect(Collectors.joining(" "));
    }

    private List<ProductVO> toVOList(List<Product> products) {
        if (products.isEmpty()) {
            return Collections.emptyList();
        }
        return products.stream().map(this::toVO).collect(Collectors.toList());
    }

    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        vo.setCoverUrl(resolveImageUrl(vo.getCoverUrl()));
        if (product.getCategoryId() != null) {
            Category category = categoryMapper.selectById(product.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
        List<ProductImage> images = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, product.getId())
                .orderByAsc(ProductImage::getSortOrder)
                .orderByAsc(ProductImage::getId));
        images.forEach(image -> image.setImageUrl(resolveImageUrl(image.getImageUrl())));
        vo.setImages(images);
        List<ProductTag> productTags = productTagMapper.selectList(new LambdaQueryWrapper<ProductTag>()
                .eq(ProductTag::getProductId, product.getId()));
        if (!productTags.isEmpty()) {
            List<Long> tagIds = productTags.stream().map(ProductTag::getTagId).collect(Collectors.toList());
            Map<Long, Tag> tagMap = tagMapper.selectBatchIds(tagIds).stream()
                    .collect(Collectors.toMap(Tag::getId, tag -> tag, (a, b) -> a, LinkedHashMap::new));
            vo.setTags(tagIds.stream().map(tagMap::get).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        return vo;
    }

    private String resolveImageUrl(String url) {
        if (!StringUtils.hasText(url) || url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String base = appProperties.getPublicFileBaseUrl();
        if (!StringUtils.hasText(base)) {
            return url;
        }
        if (url.startsWith("/uploads/")) {
            url = url.substring("/uploads/".length());
        } else if (url.startsWith("uploads/")) {
            url = url.substring("uploads/".length());
        } else if (url.startsWith("/")) {
            url = url.substring(1);
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + url;
    }
}
