package com.imooc.user.service;

import com.imooc.enums.Sex;
import com.imooc.pojo.vo.FansCountsVO;
import com.imooc.pojo.vo.RegionRatioVO;
import com.imooc.utils.PagedGridResult;

import java.io.IOException;
import java.util.List;

/**
 * @Author xiaokaixin
 * @Date 2022/7/13 15:02
 * @Version 1.0
 */
public interface MyFanService {


    /**
     * 查询当前用户是否关注作家
     */
    public boolean isMeFollowThisWriter(String writerId, String fanId);

    /**
     * 用户关注作家，成为粉丝
     * @param writerId
     * @param fanId
     */
    void follow(String writerId, String fanId);

    /**
     *  取消关注，作家损失粉丝
     * @param writerId
     * @param fanId
     */
    void unfollow(String writerId, String fanId);

    /**
     * 查询我的所有粉丝列表
     * @param writerId
     * @param page
     * @param pageSize
     * @return
     */
    /**
     * 查询我的粉丝数
     */
    public PagedGridResult queryMyFansList(String writerId,
                                           Integer page,
                                           Integer pageSize);

    // todo ES
    /**
     * 查询我的粉丝数
     */
    public PagedGridResult queryMyFansESList(String writerId,
                                           Integer page,
                                           Integer pageSize) throws IOException;

    /**
     * 查询粉丝数
     */
    public Integer queryFansCounts(String writerId, Sex sex);
    public FansCountsVO queryFansESCounts(String writerId) throws IOException;

    /**
     * 查询粉丝数
     */
    public List<RegionRatioVO> queryRegionRatioCounts(String writerId);
    public List<RegionRatioVO> queryRegionRatioESCounts(String writerId) throws IOException;

    /**
     * 粉丝信息被动更新
     * @param relationId
     * @param fanId
     */
    void forceUpdateFanInfo(String relationId, String fanId) throws IOException;
}
