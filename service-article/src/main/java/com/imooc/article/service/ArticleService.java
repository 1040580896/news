package com.imooc.article.service;

import com.imooc.pojo.Category;
import com.imooc.pojo.bo.NewArticleBO;
import com.imooc.utils.PagedGridResult;

import java.util.Date;
import java.util.List;

public interface ArticleService {

    /**
     * 发布文章
     */
    void createArticle(NewArticleBO newArticleBO, Category category);

    /**
     * 更改文章的状态
     */
    void updateArticleStatus(String articleId, Integer pendingStatus);


    /**
     * 用户中心 - 查询我的文章列表
     */
    PagedGridResult queryMyArticleList(String userId, String keyword, Integer status, Date startDate,
                                       Date endDate,
                                       Integer page,
                                       Integer pageSize);

    /**
     * 更新定时发布为即时发布
     */
    void updateAppointToPublish();

    /**
     * 管理员查询用户的所有文章列表
     *
     * @param status
     * @param page
     * @param pageSize
     * @return
     */
    PagedGridResult queryAllArticleListAdmin(Integer status, Integer page, Integer pageSize);

    /**
     * 删除文章
     */
    void deleteArticle(String userId, String articleId);

    /**
     * 撤回文章
     */
    void withdrawArticle(String userId, String articleId);

    /**
     * html 存储到对应的文章，进行关联保存
     *
     * @param articleId
     * @param articleMongoId
     */
    void updateArticleToGridFS(String articleId, String articleMongoId);

    /**
     * mq 收到消息，开始处理
     *
     * @param articleId
     */
    void updateArticleToPublish(String articleId);
}
