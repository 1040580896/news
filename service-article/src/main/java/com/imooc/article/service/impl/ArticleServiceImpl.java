package com.imooc.article.service.impl;

import com.github.pagehelper.PageHelper;
import com.imooc.api.config.RabbitMQConfig;
import com.imooc.api.config.RabbitMQDelayConfig;
import com.imooc.api.service.BaseService;
import com.imooc.article.mapper.ArticleMapper;
import com.imooc.article.mapper.ArticleMapperCustom;
import com.imooc.article.service.ArticleService;
import com.imooc.enums.ArticleAppointType;
import com.imooc.enums.ArticleReviewLevel;
import com.imooc.enums.ArticleReviewStatus;
import com.imooc.enums.YesOrNo;
import com.imooc.exception.GraceException;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.pojo.Article;
import com.imooc.pojo.Category;
import com.imooc.pojo.bo.NewArticleBO;
import com.imooc.pojo.eo.ArticleEO;
import com.imooc.utils.DateUtil;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.PagedGridResult;
import com.imooc.utils.extend.AliTextReviewUtils;
import com.mongodb.client.gridfs.GridFSBucket;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.n3r.idworker.Sid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class ArticleServiceImpl extends BaseService implements ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);
    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleMapperCustom articleMapperCustom;

    @Autowired
    AliTextReviewUtils aliTextReviewUtils;

    @Autowired
    private GridFSBucket gridFSBucket;

    @Autowired
    private Sid sid;

    @Autowired
    public RestTemplate restTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;


    /**
     * ????????????
     * @param newArticleBO
     * @param category
     */
    @Transactional
    @Override
    public void createArticle(NewArticleBO newArticleBO, Category category) {

        // ??????????????????
        String articleId = sid.nextShort();

        Article article = new Article();
        BeanUtils.copyProperties(newArticleBO, article);

        article.setId(articleId);
        article.setCategoryId(category.getId());
        article.setArticleStatus(ArticleReviewStatus.REVIEWING.type);
        article.setCommentCounts(0);
        article.setReadCounts(0);

        article.setIsDelete(YesOrNo.NO.type);
        article.setCreateTime(new Date());
        article.setUpdateTime(new Date());

        if (article.getIsAppoint() == ArticleAppointType.TIMING.type) {
            article.setPublishTime(newArticleBO.getPublishTime());
        } else if (article.getIsAppoint() == ArticleAppointType.IMMEDIATELY.type) {
            article.setPublishTime(new Date());
        }

        int res = articleMapper.insert(article);
        if (res != 1) {
            GraceException.display(ResponseStatusEnum.ARTICLE_CREATE_ERROR);
        }

        // ?????????????????????mq????????????????????????????????????????????????????????????????????????????????????
        if (article.getIsAppoint() == ArticleAppointType.TIMING.type) {

            Date endDate = newArticleBO.getPublishTime();
            Date startDate = new Date();

           int delayTimes = (int)(endDate.getTime() - startDate.getTime());

            System.out.println(DateUtil.timeBetween(startDate, endDate));

            // FIXME: ???????????????????????????10s
            // int delayTimes = 10 * 1000;

            MessagePostProcessor messagePostProcessor = new MessagePostProcessor() {
                @Override
                public Message postProcessMessage(Message message) throws AmqpException {
                    // ?????????????????????
                    message.getMessageProperties()
                            .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    // ????????????????????????????????????ms??????
                    message.getMessageProperties()
                            .setDelay(delayTimes);
                    return message;
                }
            };

            rabbitTemplate.convertAndSend(
                    RabbitMQDelayConfig.EXCHANGE_DELAY,
                    "publish.delay.display",
                    articleId,
                    messagePostProcessor);

            System.out.println("????????????-?????????????????????" + new Date());
        }


        /**
         * FIXME: ?????????????????????????????????????????????????????????????????????
         */
        // ??????????????????AI??????????????????????????????????????????????????????
       // String reviewTextResult = aliTextReviewUtils.reviewTextContent(newArticleBO.getContent());

        // ????????????????????????
        String reviewTextResult = ArticleReviewLevel.REVIEW.type;

        if (reviewTextResult
                .equalsIgnoreCase(ArticleReviewLevel.PASS.type)) {
            // ???????????????????????????????????????????????????
            this.updateArticleStatus(articleId, ArticleReviewStatus.SUCCESS.type);
        } else if (reviewTextResult
                .equalsIgnoreCase(ArticleReviewLevel.REVIEW.type)) {
            // ?????????????????????????????????????????????????????????
            this.updateArticleStatus(articleId, ArticleReviewStatus.WAITING_MANUAL.type);
        } else if (reviewTextResult
                .equalsIgnoreCase(ArticleReviewLevel.BLOCK.type)) {
            // ??????????????????????????????????????????????????????
            this.updateArticleStatus(articleId, ArticleReviewStatus.FAILED.type);
        }

    }

    /**
     * ??????????????????
     * @param articleId
     * @param pendingStatus
     */
    @Override
    public void updateArticleStatus(String articleId, Integer pendingStatus) {

        int res = articleMapper.updateArticleStatus(articleId, pendingStatus);
        if (res != 1) {
            GraceException.display(ResponseStatusEnum.ARTICLE_REVIEW_ERROR);
        }

        // ??????????????????????????????article???????????????????????????????????????es???
        if (pendingStatus == ArticleReviewStatus.SUCCESS.type) {
            Article result = articleMapper.selectByPrimaryKey(articleId);
            // ?????????????????????????????????????????????????????????????????????es???
            if (result.getIsAppoint() == ArticleAppointType.IMMEDIATELY.type) {
                ArticleEO articleEO = new ArticleEO();
                BeanUtils.copyProperties(result, articleEO);
                try {
                    // 1?????????Request
                    IndexRequest request = new IndexRequest("articles").id(articleEO.getId().toString());
                    // 2.??????????????????DSL????????????????????????JSON?????????
                    request.source(JsonUtils.objectToJson(articleEO), XContentType.JSON).type();
                    // 3.????????????
                    restHighLevelClient.index(request, RequestOptions.DEFAULT);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            // todo????????????????????????????????????????????????es????????????????????????????????????????????????
        }
    }

    /**
     * ??????????????????
     * @param userId
     * @param keyword
     * @param status
     * @param startDate
     * @param endDate
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PagedGridResult queryMyArticleList(String userId, String keyword, Integer status, Date startDate, Date endDate, Integer page, Integer pageSize) {
        Example example = new Example(Article.class);
        example.orderBy("createTime").desc();
        Example.Criteria criteria = example.createCriteria();

        criteria.andEqualTo("publishUserId", userId);

        if (StringUtils.isNotBlank(keyword)) {
            criteria.andLike("title", "%" + keyword + "%");
        }

        if (ArticleReviewStatus.isArticleStatusValid(status)) {
            criteria.andEqualTo("articleStatus", status);
        }

        if (status != null && status == 12) {
            criteria.andEqualTo("articleStatus", ArticleReviewStatus.REVIEWING.type)
                    .orEqualTo("articleStatus", ArticleReviewStatus.WAITING_MANUAL.type);
        }

        criteria.andEqualTo("isDelete", YesOrNo.NO.type);

        if (startDate != null) {
            criteria.andGreaterThanOrEqualTo("publishTime", startDate);
        }
        if (endDate != null) {
            criteria.andLessThanOrEqualTo("publishTime", endDate);
        }

        PageHelper.startPage(page, pageSize);
        List<Article> list = articleMapper.selectByExample(example);
        return setterPagedGrid(list, page);
    }

    @Transactional
    @Override
    public void updateAppointToPublish() {
        articleMapperCustom.updateAppointToPublish();
    }

    @Override
    public PagedGridResult queryAllArticleListAdmin(Integer status, Integer page, Integer pageSize) {
        Example articleExample = new Example(Article.class);
        articleExample.orderBy("createTime").desc();

        Example.Criteria criteria = articleExample.createCriteria();
        if (ArticleReviewStatus.isArticleStatusValid(status)) {
            criteria.andEqualTo("articleStatus", status);
        }

        // ????????????????????????????????????????????????????????????????????????
        if (status != null && status == 12) {
            criteria.andEqualTo("articleStatus", ArticleReviewStatus.REVIEWING.type)
                    .orEqualTo("articleStatus", ArticleReviewStatus.WAITING_MANUAL.type);
        }

        //isDelete ?????????0
        criteria.andEqualTo("isDelete", YesOrNo.NO.type);

        /**
         * page: ?????????
         * pageSize: ??????????????????
         */
        PageHelper.startPage(page, pageSize);
        List<Article> list = articleMapper.selectByExample(articleExample);
        return setterPagedGrid(list, page);
    }

    /**
     * ????????????xs
     * @param userId
     * @param articleId
     */
    @Transactional
    @Override
    public void deleteArticle(String userId, String articleId) {
        // 1. ???????????????mongoFileId
        Article pending = articleMapper.selectByPrimaryKey(articleId);
        Example articleExample = makeExampleCriteria(userId, articleId);

        pending.setIsDelete(YesOrNo.YES.type);
        int result = articleMapper.updateByExampleSelective(pending, articleExample);
        if (result != 1) {
            GraceException.display(ResponseStatusEnum.ARTICLE_DELETE_ERROR);
        }

        String articleMongoId = pending.getMongoFileId();

        if(StringUtils.isBlank(articleMongoId)){
            return;
        }

        // 2. ??????GridFS????????????
        gridFSBucket.delete(new ObjectId(articleMongoId));

        // 3. ??????????????????HTML??????
//        doDeleteArticleHTML(articleId);
        doDeleteArticleHTMLByMQ(articleId);

        //4. ??????ES???????????? TODO ES
        DeleteRequest request = new DeleteRequest("articles", articleId);

        try {
            restHighLevelClient.delete(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("ES?????????????????????????????????");
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????????????????????????????html
     */
    private void deleteHTML(String articleId) {
        // 1. ???????????????mongoFileId
        Article pending = articleMapper.selectByPrimaryKey(articleId);
        String articleMongoId = pending.getMongoFileId();
        if(StringUtils.isBlank(articleMongoId)){
            return;
        }

        // 2. ??????GridFS????????????
        gridFSBucket.delete(new ObjectId(articleMongoId));

        // 3. ??????????????????HTML??????
//        doDeleteArticleHTML(articleId);
        doDeleteArticleHTMLByMQ(articleId);
    }


    private void doDeleteArticleHTML(String articleId) {
        String url = "http://html.imoocnews.com:8002/article/html/delete?articleId=" + articleId;
        ResponseEntity<Integer> responseEntity = restTemplate.getForEntity(url, Integer.class);
        int status = responseEntity.getBody();
        if (status != HttpStatus.OK.value()) {
            GraceException.display(ResponseStatusEnum.SYSTEM_OPERATION_ERROR);
        }
    }


    private void doDeleteArticleHTMLByMQ(String articleId) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_ARTICLE,
                "article.html.download.do", articleId);
    }

    /**
     * ????????????
     * @param userId
     * @param articleId
     */
    @Transactional
    @Override
    public void withdrawArticle(String userId, String articleId) {
        Example articleExample = makeExampleCriteria(userId, articleId);

        Article pending = new Article();
        pending.setIsDelete(YesOrNo.YES.type);

        int result = articleMapper.updateByExampleSelective(pending, articleExample);
        if (result != 1) {
            GraceException.display(ResponseStatusEnum.ARTICLE_DELETE_ERROR);
        }

        deleteHTML(articleId);

        //4. ??????ES???????????? TODO ES
        DeleteRequest request = new DeleteRequest("articles", articleId);

        try {
            restHighLevelClient.delete(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("ES?????????????????????????????????");
            e.printStackTrace();
        }
    }


    /**
     * article?????????????????????mongo????????????????????????
     * @param articleId
     * @param articleMongoId
     */
    @Transactional
    @Override
    public void updateArticleToGridFS(String articleId, String articleMongoId) {
        Article pendingArticle = new Article();
        pendingArticle.setId(articleId);
        pendingArticle.setMongoFileId(articleMongoId);
        articleMapper.updateByPrimaryKeySelective(pendingArticle);
    }

    /**
     * mq ??????????????????????????? ???????????????????????????es???
     * @param articleId
     */

    @Transactional
    @Override
    public void updateArticleToPublish(String articleId) {
        // ????????????
        Article article = new Article();
        article.setId(articleId);
        article.setIsAppoint(ArticleAppointType.IMMEDIATELY.type);

        // ?????????ES???
        Article articleEO = articleMapper.selectByPrimaryKey(articleId);
        IndexRequest request = new IndexRequest("articles").id(articleEO.getId().toString());
        // 2.??????????????????DSL????????????????????????JSON?????????
        request.source(JsonUtils.objectToJson(articleEO), XContentType.JSON).type();
        // 3.????????????
        try {
            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.info("?????????ES?????????", e);
            e.printStackTrace();
        }
        articleMapper.updateByPrimaryKeySelective(article);
    }


    /**
     * ????????????
     * @param userId
     * @param articleId
     * @return
     */
    private Example makeExampleCriteria(String userId, String articleId) {
        Example articleExample = new Example(Article.class);
        Example.Criteria criteria = articleExample.createCriteria();
        criteria.andEqualTo("publishUserId", userId);
        criteria.andEqualTo("id", articleId);
        return articleExample;
    }


}
