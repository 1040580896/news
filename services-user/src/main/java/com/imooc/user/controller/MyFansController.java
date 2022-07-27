package com.imooc.user.controller;

import com.imooc.api.BaseController;
import com.imooc.api.controller.user.HelloControllerApi;
import com.imooc.api.controller.user.MyFansControllerApi;
import com.imooc.enums.Sex;
import com.imooc.exception.GraceException;
import com.imooc.exception.MyCustomException;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.pojo.vo.FansCountsVO;
import com.imooc.user.service.MyFanService;
import com.imooc.utils.RedisOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class MyFansController extends BaseController implements MyFansControllerApi {

    final static Logger logger = LoggerFactory.getLogger(MyFansController.class);

    @Autowired
    private MyFanService myFanService;

    /**
     * 粉丝信息被动更新
     * @param relationId
     * @param fanId
     * @return
     */
    @Override
    public GraceJSONResult forceUpdateFanInfo(String relationId, String fanId) {
        try {
            myFanService.forceUpdateFanInfo(relationId, fanId);
        } catch (IOException e) {
            GraceException.display(ResponseStatusEnum.USERFANS_UPDATE_ERROR);
        }
        return GraceJSONResult.ok();
    }

    /**
     * 查询当前用户是否关注作家
     * @param writerId
     * @param fanId
     * @return
     */
    @Override
    public GraceJSONResult isMeFollowThisWriter(String writerId,
                                                String fanId) {
        boolean res = myFanService.isMeFollowThisWriter(writerId, fanId);
        return GraceJSONResult.ok(res);
    }

    /**
     * 用户关注作家，成为粉丝
     * @param writerId
     * @param fanId
     * @return
     */
    @Override
    public GraceJSONResult follow(String writerId, String fanId) {
        myFanService.follow(writerId, fanId);
        return GraceJSONResult.ok();
    }

    /**
     * 取消关注，作家损失粉丝
     * @param writerId
     * @param fanId
     * @return
     */
    @Override
    public GraceJSONResult unfollow(String writerId, String fanId) {
        myFanService.unfollow(writerId, fanId);
        return GraceJSONResult.ok();
    }

    /**
     * 查询我的所有粉丝列表
     * @param writerId
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public GraceJSONResult queryAll(String writerId,
                                    Integer page,
                                    Integer pageSize) throws IOException {

        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        // 可以改为ES中查询 todo
        // return GraceJSONResult.ok(myFanService.queryMyFansList(writerId,
        //         page,
        //         pageSize));

        return GraceJSONResult.ok(myFanService.queryMyFansESList(writerId,
                page,
                pageSize));
    }


    /**
     * 查询男女粉丝数量
     * @param writerId
     * @return
     */
    @Override
    public GraceJSONResult queryRatio(String writerId) throws IOException {

        // int manCounts = myFanService.queryFansCounts(writerId, Sex.man);
        // int womanCounts = myFanService.queryFansCounts(writerId, Sex.woman);
        //
        // FansCountsVO fansCountsVO = new FansCountsVO();
        // fansCountsVO.setManCounts(manCounts);
        // fansCountsVO.setWomanCounts(womanCounts);
        // todo
        FansCountsVO fansCountsVO = myFanService.queryFansESCounts(writerId);

        return GraceJSONResult.ok(fansCountsVO);
    }

    /**
     * 根据地域查询粉丝数量 todo
     * @param writerId
     * @return
     */
    @Override
    public GraceJSONResult queryRatioByRegion(String writerId) throws IOException {
        // return GraceJSONResult.ok(myFanService
        //         .queryRegionRatioCounts(writerId));
        // return GraceJSONResult.ok(myFanService.queryRegionRatioCounts(writerId));
        return GraceJSONResult.ok(myFanService
                .queryRegionRatioESCounts(writerId));
    }
}
