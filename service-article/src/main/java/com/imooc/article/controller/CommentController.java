package com.imooc.article.controller;

import com.imooc.api.BaseController;
import com.imooc.api.controller.article.CommentControllerApi;
import com.imooc.api.controller.user.HelloControllerApi;
import com.imooc.article.service.CommentPortalService;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.pojo.bo.CommentReplyBO;
import com.imooc.pojo.vo.AppUserVO;
import com.imooc.utils.PagedGridResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class CommentController extends BaseController implements CommentControllerApi {

    final static Logger logger = LoggerFactory.getLogger(CommentController.class);

    @Autowired
    private CommentPortalService commentPortalService;

    /**
     * 发表评论
     * @param commentReplyBO
     * @param
     * @return
     */
    @Override
    public GraceJSONResult createArticle(@Valid CommentReplyBO commentReplyBO) {

        // 0. 判断BindingResult是否保存错误的验证信息，如果有，则直接return
        // if (result.hasErrors()) {
        //     Map<String, String> errorMap = getErrors(result);
        //     return GraceJSONResult.errorMap(errorMap);
        // }

        // 1. 根据留言用户的id查询他的昵称，用于存入到数据表进行字段的冗余处理，从而避免多表关联查询的性能影响
        String userId = commentReplyBO.getCommentUserId();

        // 2. 发起restTemplate调用用户服务，获得用户侧昵称
        Set<String> idSet = new HashSet<>();
        idSet.add(userId);
        List<AppUserVO> userList = getBasicUserList(idSet);
        for (AppUserVO vo : userList) {
            System.out.println(vo);
        }
        AppUserVO appUserVO = userList.get(0);
        String nickname = appUserVO.getNickname();
        String face = appUserVO.getFace();

        // 3. 保存用户评论的信息到数据库
        commentPortalService.createComment(commentReplyBO.getArticleId(),
                commentReplyBO.getFatherId(),
                commentReplyBO.getContent(),
                userId,
                nickname,face);

        return GraceJSONResult.ok();
    }

    /**
     * 用户评论数查询
     * @param articleId
     * @return
     */
    @Override
    public GraceJSONResult commentCounts(String articleId) {

        Integer counts =
                getCountsFromRedis(REDIS_ARTICLE_COMMENT_COUNTS + ":" + articleId);

        return GraceJSONResult.ok(counts);
    }

    /**
     * 查询文章的所有评论列表
     * @param articleId
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public GraceJSONResult list(String articleId,
                                Integer page,
                                Integer pageSize) {

        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        PagedGridResult gridResult = commentPortalService.queryArticleComments(articleId, page, pageSize);
        return GraceJSONResult.ok(gridResult);
    }


    /**
     * 查询我的评论管理列表
     * @param writerId
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public GraceJSONResult mng(String writerId, Integer page, Integer pageSize) {

        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        PagedGridResult gridResult = commentPortalService.queryWriterCommentsMng(writerId, page, pageSize);
        return GraceJSONResult.ok(gridResult);
    }

    /**
     * 作者删除评论
     * @param writerId
     * @param commentId
     * @return
     */
    @Override
    public GraceJSONResult delete(String writerId, String commentId) {
        commentPortalService.deleteComment(writerId, commentId);
        return GraceJSONResult.ok();
    }

}
