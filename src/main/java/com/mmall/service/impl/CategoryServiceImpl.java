package com.mmall.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CategoryMapper;
import com.mmall.pojo.Category;
import com.mmall.service.ICategoryService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service("iCategoryService")
public class CategoryServiceImpl implements ICategoryService {

    private Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);

    @Autowired
    private CategoryMapper categoryMapper;

    // 添加商品分类
    public ServerResponse addCategory(String categoryName, Integer parentId) {
        if (parentId == null || StringUtils.isBlank(categoryName)) {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        Category category = new Category();
        category.setParentId(parentId);
        category.setName(categoryName);
        category.setStatus(true);

        int rowCount = categoryMapper.insert(category);
        if (rowCount > 0) {
            return ServerResponse.createBySuccessMessage("添加分类成功");
        }
        return ServerResponse.createByErrorMessage("添加分类失败");
    }

    // 更新商品分类名字
    public ServerResponse updateCategoryName(Integer categoryId, String categoryName) {
        if (categoryId == null || StringUtils.isBlank(categoryName)) {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        Category category = new Category();
        category.setId(categoryId);
        category.setName(categoryName);

        int rowCount = categoryMapper.updateByPrimaryKeySelective(category);
        if (rowCount > 0) {
            return ServerResponse.createBySuccessMessage("更新品类名字成功");
        }
        return ServerResponse.createByErrorMessage("更新品类名字失败");
    }

    // 根据传入parentId(如果没传入默认0)查询当前parentId下的商品
    // 比如1下面有（10，20），10下面有（100，200，300），则查询1的时候， 只会返回（10，20）
    public ServerResponse<List<Category>> getCategoryByParentId(Integer parent_id) {
        List<Category> categoryList = categoryMapper.selectCategoryByParentId(parent_id);
        if (CollectionUtils.isEmpty(categoryList)) {
            logger.info("未找到当前分类下的商品");
        }
        return ServerResponse.createBySuccess(categoryList);
    }

    // 根据传入的parent_id(如果没传入默认0)查询当前parentId下的商品，如果该商品还有子节点，也要查出来
    // 比如1下面有（10，20），10下面有（100，200，300），则查询1的时候， 会返回（10，20，100，200）
    public ServerResponse getCategoryAndChildrenByParentId(Integer parentId) {
        Set<Category> categorySet = Sets.newHashSet();
        findChildrenCategory(categorySet, parentId);
        List<Integer> categoryList = Lists.newArrayList();
        if (parentId != null) {
            for (Category categoryItem : categorySet) {
                categoryList.add(categoryItem.getId());
            }
        }
        return ServerResponse.createBySuccess(categoryList);
    }

    // 递归算法
    private Set<Category> findChildrenCategory(Set<Category> categorySet, Integer parentId) {
        Category category = categoryMapper.selectByPrimaryKey(parentId);
        if (category != null) {
            categorySet.add(category);
        }
        // 查找子节点，递归算法要有一个退出条件
        List<Category> categoryList = categoryMapper.selectCategoryByParentId(parentId);
        for (Category categoryItem : categoryList) {
            findChildrenCategory(categorySet, categoryItem.getId());
        }
        return categorySet;
    }
}
