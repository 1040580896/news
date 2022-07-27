package com.imooc.admin.service;

import com.imooc.pojo.bo.NewAdminBO;
import com.imooc.pojo.AdminUser;
import com.imooc.utils.PagedGridResult;

/**
 * @Author xiaokaixin
 * @Date 2022/7/4 21:22
 * @Version 1.0
 */
public interface AdminUserService {
    /**
     * 获得管理员的用户信息
     * @param username
     * @return
     */
    public AdminUser queryAdminByUsername(String username);

    /**
     * 添加管理员
     * @param newAdminBO
     */
    void createAdminUser(NewAdminBO newAdminBO);

    /**
     * 分页查询admin列表
     */
    public PagedGridResult queryAdminList(Integer page, Integer pageSize);
}
