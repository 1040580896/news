package com.imooc.admin.service.impl;

import com.imooc.admin.repository.FriendLinkRepository;
import com.imooc.admin.service.FriendLinkService;
import com.imooc.enums.YesOrNo;
import com.imooc.pojo.mo.FriendLinkMO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FriendLinkServiceImpl implements FriendLinkService {

    @Autowired
    private FriendLinkRepository friendLinkRepository;

    @Override
    public void saveOrUpdateFriendLink(FriendLinkMO friendLinkMO) {
        friendLinkRepository.save(friendLinkMO);
    }

    @Override
    public List<FriendLinkMO> queryAllFriendLinkList() {
        return friendLinkRepository.findAll();
    }

    /**
     * 删除友情链接
     * @param linkId
     */
    @Override
    public void delete(String linkId) {
        friendLinkRepository.deleteById(linkId);
    }

    /**
     * 首页查询友情链接
     * @return
     */
    @Override
    public List<FriendLinkMO> queryPortalAllFriendLinkList() {
        return friendLinkRepository.getAllByIsDelete(YesOrNo.NO.type);
    }
}
