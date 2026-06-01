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
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private static final String PUBLISHED = "PUBLISHED";
    private static final String MEDIA_IMAGE = "IMAGE";
    private static final String MEDIA_VIDEO = "VIDEO";

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductTagMapper productTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final UploadService uploadService;
    private final NewProductNotificationService newProductNotificationService;

    public PageResult<ProductVO> page(long page, long size, String keyword, Long categoryId, Long tagId, String status, boolean publicOnly) {
        return page(page, size, keyword, categoryId, tagId, status, publicOnly, null);
    }

    public PageResult<ProductVO> page(long page, long size, String keyword, Long categoryId, Long tagId, String status, boolean publicOnly, String sort) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (publicOnly) {
            wrapper.eq(Product::getStatus, PUBLISHED);
        } else if (StringUtils.hasText(status)) {
            wrapper.eq(Product::getStatus, status);
        }
        if (categoryId != null) {
            wrapper.in(Product::getCategoryId, collectCategoryIds(categoryId));
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

        applySort(wrapper, sort);
        Page<Product> result = productMapper.selectPage(Page.of(page, size), wrapper);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), toVOList(result.getRecords()));
    }

    private Set<Long> collectCategoryIds(Long categoryId) {
        List<Category> categories = categoryMapper.selectList(null);
        Map<Long, List<Long>> childrenByParent = categories.stream()
                .filter(category -> category.getParentId() != null)
                .collect(Collectors.groupingBy(
                        Category::getParentId,
                        LinkedHashMap::new,
                        Collectors.mapping(Category::getId, Collectors.toList())
                ));
        Set<Long> categoryIds = new LinkedHashSet<>();
        Deque<Long> pending = new ArrayDeque<>();
        pending.add(categoryId);
        while (!pending.isEmpty()) {
            Long currentId = pending.removeFirst();
            if (categoryIds.add(currentId)) {
                pending.addAll(childrenByParent.getOrDefault(currentId, Collections.emptyList()));
            }
        }
        return categoryIds;
    }

    private void applySort(LambdaQueryWrapper<Product> wrapper, String sort) {
        String normalized = StringUtils.hasText(sort) ? sort.trim().toLowerCase() : "";
        if ("latest".equals(normalized)) {
            wrapper.orderByDesc(Product::getUpdatedAt).orderByDesc(Product::getId);
            return;
        }
        if ("oldest".equals(normalized)) {
            wrapper.orderByAsc(Product::getUpdatedAt).orderByDesc(Product::getId);
            return;
        }
        wrapper.orderByDesc(Product::getSortOrder).orderByDesc(Product::getId);
    }

    public ProductVO detail(Long id, boolean publicOnly) {
        Product product = productMapper.selectById(id);
        if (product == null || (publicOnly && !PUBLISHED.equals(product.getStatus()))) {
            throw new IllegalArgumentException("商品不存在或已下架");
        }
        return toVO(product);
    }

    public ProductVO save(Long id, ProductRequest request) {
        Product product = id == null ? new Product() : productMapper.selectById(id);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        String previousStatus = product.getStatus();
        BeanUtils.copyProperties(request, product, "images", "tagIds");
        product.setContactPhone(null);
        product.setContactWechat(null);
        product.setSearchText(buildSearchText(request));
        product.setCoverUrl(resolveCoverUrl(request.getImages(), product.getCoverUrl()));
        if (!StringUtils.hasText(product.getStatus())) {
            product.setStatus("DRAFT");
        }
        if (product.getSortOrder() == null) {
            product.setSortOrder(0);
        }
        if (product.getCarouselAutoplayEnabled() == null) {
            product.setCarouselAutoplayEnabled(true);
        }
        if (product.getCarouselIntervalSeconds() == null || product.getCarouselIntervalSeconds() < 1) {
            product.setCarouselIntervalSeconds(3);
        }
        if (product.getCarouselIntervalSeconds() > 20) {
            product.setCarouselIntervalSeconds(20);
        }
        if (id == null) {
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
        }
        replaceImages(product.getId(), request.getImages());
        replaceTags(product.getId(), request.getTagIds());
        ProductVO vo = detail(product.getId(), false);
        notifyIfNewlyPublished(previousStatus, vo);
        return vo;
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
        String previousStatus = product.getStatus();
        product.setStatus(status);
        productMapper.updateById(product);
        ProductVO vo = detail(id, false);
        if (PUBLISHED.equals(status)) {
            newProductNotificationService.notifyNewProduct(vo);
        } else {
            notifyIfNewlyPublished(previousStatus, vo);
        }
        return vo;
    }

    private void replaceImages(Long productId, List<ImageRequest> images) {
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, productId));
        if (images == null) {
            return;
        }
        int index = 0;
        int detailIndex = 0;
        for (ImageRequest request : images) {
            String mediaType = normalizeMediaType(request.getMediaType());
            if (!StringUtils.hasText(request.getImageUrl())) {
                continue;
            }
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setMediaType(mediaType);
            image.setImageUrl(request.getImageUrl());
            image.setObjectKey(request.getObjectKey());
            image.setPosterUrl(request.getPosterUrl());
            image.setWidth(request.getWidth());
            image.setHeight(request.getHeight());
            image.setSortOrder(request.getSortOrder() == null ? index : request.getSortOrder());
            image.setIsCover(Boolean.TRUE.equals(request.getIsCover()));
            boolean isDetailImage = MEDIA_IMAGE.equals(mediaType) && !Boolean.FALSE.equals(request.getShowInDetail());
            image.setShowInDetail(isDetailImage);
            image.setDetailSortOrder(request.getDetailSortOrder() == null ? detailIndex : request.getDetailSortOrder());
            productImageMapper.insert(image);
            if (isDetailImage) {
                detailIndex++;
            }
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
        images.forEach(this::normalizeMediaForOutput);
        vo.setImages(images);
        ProductImage cover = images.stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsCover()))
                .findFirst()
                .orElseGet(() -> images.stream()
                        .filter(image -> MEDIA_IMAGE.equals(normalizeMediaType(image.getMediaType())))
                        .findFirst()
                        .orElse(images.isEmpty() ? null : images.get(0)));
        vo.setCoverUrl(cover == null ? resolveUploadUrl(vo.getCoverUrl()) : mediaCoverUrl(cover));
        List<ProductTag> productTags = productTagMapper.selectList(new LambdaQueryWrapper<ProductTag>()
                .eq(ProductTag::getProductId, product.getId()));
        if (!productTags.isEmpty()) {
            List<Long> tagIds = productTags.stream().map(ProductTag::getTagId).collect(Collectors.toList());
            Map<Long, Tag> tagMap = tagMapper.selectBatchIds(tagIds).stream()
                    .collect(Collectors.toMap(Tag::getId, tag -> tag, (a, b) -> a, LinkedHashMap::new));
            vo.setTags(tagIds.stream().map(tagMap::get).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        vo.getImages().sort(Comparator.comparing(ProductImage::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ProductImage::getId, Comparator.nullsLast(Long::compareTo)));
        return vo;
    }

    private void normalizeMediaForOutput(ProductImage media) {
        media.setMediaType(normalizeMediaType(media.getMediaType()));
        media.setImageUrl(resolveUploadUrl(StringUtils.hasText(media.getObjectKey()) ? media.getObjectKey() : media.getImageUrl()));
        media.setPosterUrl(resolveUploadUrl(media.getPosterUrl()));
        if (media.getShowInDetail() == null) {
            media.setShowInDetail(MEDIA_IMAGE.equals(media.getMediaType()));
        }
        if (media.getDetailSortOrder() == null) {
            media.setDetailSortOrder(media.getSortOrder());
        }
    }

    private String resolveCoverUrl(List<ImageRequest> images, String fallback) {
        if (images != null) {
            ImageRequest explicitCover = images.stream().filter(image -> Boolean.TRUE.equals(image.getIsCover())).findFirst().orElse(null);
            ImageRequest firstImage = images.stream().filter(image -> MEDIA_IMAGE.equals(normalizeMediaType(image.getMediaType()))).findFirst().orElse(null);
            ImageRequest firstMedia = images.stream().findFirst().orElse(null);
            ImageRequest selected = explicitCover != null ? explicitCover : firstImage != null ? firstImage : firstMedia;
            if (selected != null) {
                if (MEDIA_VIDEO.equals(normalizeMediaType(selected.getMediaType()))) {
                    return StringUtils.hasText(selected.getPosterUrl()) ? selected.getPosterUrl() : "";
                }
                if (StringUtils.hasText(selected.getImageUrl())) {
                    return selected.getImageUrl();
                }
            }
        }
        return fallback;
    }

    private String mediaCoverUrl(ProductImage media) {
        if (MEDIA_VIDEO.equals(normalizeMediaType(media.getMediaType())) && StringUtils.hasText(media.getPosterUrl())) {
            return media.getPosterUrl();
        }
        if (MEDIA_VIDEO.equals(normalizeMediaType(media.getMediaType()))) {
            return "";
        }
        return media.getImageUrl();
    }

    private String normalizeMediaType(String mediaType) {
        return MEDIA_VIDEO.equalsIgnoreCase(mediaType) ? MEDIA_VIDEO : MEDIA_IMAGE;
    }

    private void notifyIfNewlyPublished(String previousStatus, ProductVO product) {
        if (PUBLISHED.equals(product.getStatus()) && !PUBLISHED.equals(previousStatus)) {
            newProductNotificationService.notifyNewProduct(product);
        }
    }

    private String resolveUploadUrl(String url) {
        return uploadService.resolveFileUrl(url);
    }
}
