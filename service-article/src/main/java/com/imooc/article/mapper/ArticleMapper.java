package com.imooc.article.mapper;

import com.imooc.my.mapper.MyMapper;
import com.imooc.pojo.Article;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleMapper extends MyMapper<Article> {

    // 更新文章状态
    int updateArticleStatus(@Param("articleId") String articleId, @Param("pendingStatus") Integer pendingStatus);
}