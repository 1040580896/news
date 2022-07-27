package com.imooc.admin.service;

import com.imooc.pojo.Category;

import java.util.List;

public interface CategoryService {

    /**
     * 新增文章分类
     */
    public void createCategory(Category category);

    /**
     * 修改文章分类列表
     */
    public void modifyCategory(Category category);

    /**
     * 查询分类名是否已经存在
     */
    public boolean queryCatIsExist(String catName, String oldCatName);

    /**
     * 获得文章分类列表
     */
    public List<Category> queryCategoryList();

    /**
     * 用户端删除分类列表
     * @param cid
     * @return
     */
    Integer deleteCats(Integer cid);
}
