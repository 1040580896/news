package com.imooc.article.service;

import com.imooc.utils.PagedGridResult;

/**
 * @Author xiaokaixin
 * @Date 2022/7/14 09:03
 * @Version 1.0
 */
public interface CommentPortalService {


    /**
     * 发表评论
     */
    void createComment(String articleId, String fatherId, String content, String userId, String nickname, String face);

    /**
     * 查询文章的所有评论列表
     * @param articleId
     * @param page
     * @param pageSize
     * @return
     */
    PagedGridResult queryArticleComments(String articleId, Integer page, Integer pageSize);

    /**
     * 查询我的评论管理列表
     */
    public PagedGridResult queryWriterCommentsMng(String writerId, Integer page, Integer pageSize);

    /**
     * 删除评论
     */
    public void deleteComment(String writerId, String commentId);



}
