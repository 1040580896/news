package com.imooc.admin.mapper;

import com.imooc.my.mapper.MyMapper;
import com.imooc.pojo.Category;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryMapper extends MyMapper<Category> {
    /**
     * 用户端删除分类列表
     * @param cid
     * @return
     */
    Integer deleteCats(Integer cid);
}