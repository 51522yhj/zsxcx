package com.xiaoyu.yinran.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoyu.yinran.dto.CategoryRequest;
import com.xiaoyu.yinran.dto.TagRequest;
import com.xiaoyu.yinran.entity.Category;
import com.xiaoyu.yinran.entity.Tag;
import com.xiaoyu.yinran.mapper.CategoryMapper;
import com.xiaoyu.yinran.mapper.TagMapper;
import com.xiaoyu.yinran.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public List<Category> listCategories(Boolean onlyEnabled) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder)
                .orderByDesc(Category::getId);
        if (Boolean.TRUE.equals(onlyEnabled)) {
            wrapper.eq(Category::getEnabled, true);
        }
        return categoryMapper.selectList(wrapper);
    }

    public List<CategoryVO> categoryTree(Boolean onlyEnabled) {
        List<Category> categories = listCategories(onlyEnabled);
        Map<Long, CategoryVO> map = new LinkedHashMap<>();
        for (Category category : categories) {
            CategoryVO vo = new CategoryVO();
            BeanUtils.copyProperties(category, vo);
            map.put(vo.getId(), vo);
        }
        List<CategoryVO> roots = new ArrayList<>();
        for (CategoryVO vo : map.values()) {
            if (vo.getParentId() != null && map.containsKey(vo.getParentId())) {
                map.get(vo.getParentId()).getChildren().add(vo);
            } else {
                roots.add(vo);
            }
        }
        roots.sort(Comparator.comparing(CategoryVO::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
        return roots;
    }

    public Category saveCategory(Long id, CategoryRequest request) {
        Category category = id == null ? new Category() : categoryMapper.selectById(id);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        BeanUtils.copyProperties(request, category);
        if (id == null) {
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        return category;
    }

    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }

    public List<Tag> listTags(Boolean onlyEnabled) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<Tag>()
                .orderByAsc(Tag::getSortOrder)
                .orderByDesc(Tag::getId);
        if (Boolean.TRUE.equals(onlyEnabled)) {
            wrapper.eq(Tag::getEnabled, true);
        }
        return tagMapper.selectList(wrapper);
    }

    public Tag saveTag(Long id, TagRequest request) {
        Tag tag = id == null ? new Tag() : tagMapper.selectById(id);
        if (tag == null) {
            throw new IllegalArgumentException("标签不存在");
        }
        BeanUtils.copyProperties(request, tag);
        if (id == null) {
            tagMapper.insert(tag);
        } else {
            tagMapper.updateById(tag);
        }
        return tag;
    }

    public void deleteTag(Long id) {
        tagMapper.deleteById(id);
    }
}

