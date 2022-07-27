package com.imooc.admin.repository;

import com.imooc.pojo.mo.FriendLinkMO;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FriendLinkRepository extends MongoRepository<FriendLinkMO, String> {

    /**
     * 首页查询友情链接
     * @param isDelete
     * @return
     */
    public List<FriendLinkMO> getAllByIsDelete(Integer isDelete);

}