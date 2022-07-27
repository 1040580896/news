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
     * 发布文章
     * @param newArticleBO
     * @param category
     */
    @Transactional
    @Override
    public void createArticle(NewArticleBO newArticleBO, Category category) {

        // 基本信息设置
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

        // 发送延迟消息到mq，计算定时发布时间和当前时间的时间差，则为往后延迟的时间
        if (article.getIsAppoint() == ArticleAppointType.TIMING.type) {

            Date endDate = newArticleBO.getPublishTime();
            Date startDate = new Date();

           int delayTimes = (int)(endDate.getTime() - startDate.getTime());

            System.out.println(DateUtil.timeBetween(startDate, endDate));

            // FIXME: 为了测试方便，写死10s
            // int delayTimes = 10 * 1000;

            MessagePostProcessor messagePostProcessor = new MessagePostProcessor() {
                @Override
                public Message postProcessMessage(Message message) throws AmqpException {
                    // 设置消息的持久
                    message.getMessageProperties()
                            .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    // 设置消息延迟的时间，单位ms毫秒
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

            System.out.println("延迟消息-定时发布文章：" + new Date());
        }


        /**
         * FIXME: 我们只检测正常的词汇，非正常词汇大家课后去检测
         */
        // 通过阿里智能AI实现对文章文本的自动检测（自动审核）
       // String reviewTextResult = aliTextReviewUtils.reviewTextContent(newArticleBO.getContent());

        // 收到改为人工审核
        String reviewTextResult = ArticleReviewLevel.REVIEW.type;

        if (reviewTextResult
                .equalsIgnoreCase(ArticleReviewLevel.PASS.type)) {
            // 修改当前的文章，状态标记为审核通过
            this.updateArticleStatus(articleId, ArticleReviewStatus.SUCCESS.type);
        } else if (reviewTextResult
                .equalsIgnoreCase(ArticleReviewLevel.REVIEW.type)) {
            // 修改当前的文章，状态标记为需要人工审核
            this.updateArticleStatus(articleId, ArticleReviewStatus.WAITING_MANUAL.type);
        } else if (reviewTextResult
                .equalsIgnoreCase(ArticleReviewLevel.BLOCK.type)) {
            // 修改当前的文章，状态标记为审核未通过
            this.updateArticleStatus(articleId, ArticleReviewStatus.FAILED.type);
        }

    }

    /**
     * 更新文章状态
     * @param articleId
     * @param pendingStatus
     */
    @Override
    public void updateArticleStatus(String articleId, Integer pendingStatus) {

        int res = articleMapper.updateArticleStatus(articleId, pendingStatus);
        if (res != 1) {
            GraceException.display(ResponseStatusEnum.ARTICLE_REVIEW_ERROR);
        }

        // 如果审核通过，则查询article，把相应的数据字段信息存入es中
        if (pendingStatus == ArticleReviewStatus.SUCCESS.type) {
            Article result = articleMapper.selectByPrimaryKey(articleId);
            // 如果是即时发布的文章，审核通过后则可以直接存入es中
            if (result.getIsAppoint() == ArticleAppointType.IMMEDIATELY.type) {
                ArticleEO articleEO = new ArticleEO();
                BeanUtils.copyProperties(result, articleEO);
                try {
                    // 1、准备Request
                    IndexRequest request = new IndexRequest("articles").id(articleEO.getId().toString());
                    // 2.准备请求参数DSL，其实就是文档的JSON字符串
                    request.source(JsonUtils.objectToJson(articleEO), XContentType.JSON).type();
                    // 3.发送请求
                    restHighLevelClient.index(request, RequestOptions.DEFAULT);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            // todo：如果是定时发布，此处不会存入到es中，需要在定时的延迟队列中去执行
        }
    }

    /**
     * 条件分页查询
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

        // 审核中是机审和人审核的两个状态，所以需要单独判断
        if (status != null && status == 12) {
            criteria.andEqualTo("articleStatus", ArticleReviewStatus.REVIEWING.type)
                    .orEqualTo("articleStatus", ArticleReviewStatus.WAITING_MANUAL.type);
        }

        //isDelete 必须是0
        criteria.andEqualTo("isDelete", YesOrNo.NO.type);

        /**
         * page: 第几页
         * pageSize: 每页显示条数
         */
        PageHelper.startPage(page, pageSize);
        List<Article> list = articleMapper.selectByExample(articleExample);
        return setterPagedGrid(list, page);
    }

    /**
     * 删除文章xs
     * @param userId
     * @param articleId
     */
    @Transactional
    @Override
    public void deleteArticle(String userId, String articleId) {
        // 1. 查询文章的mongoFileId
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

        // 2. 删除GridFS上的文件
        gridFSBucket.delete(new ObjectId(articleMongoId));

        // 3. 删除消费端的HTML文件
//        doDeleteArticleHTML(articleId);
        doDeleteArticleHTMLByMQ(articleId);

        //4. 删除ES中的数据 TODO ES
        DeleteRequest request = new DeleteRequest("articles", articleId);

        try {
            restHighLevelClient.delete(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("ES中没有他的该文章的数据");
            e.printStackTrace();
        }
    }

    /**
     * 文章撤回删除后，删除静态化的html
     */
    private void deleteHTML(String articleId) {
        // 1. 查询文章的mongoFileId
        Article pending = articleMapper.selectByPrimaryKey(articleId);
        String articleMongoId = pending.getMongoFileId();
        if(StringUtils.isBlank(articleMongoId)){
            return;
        }

        // 2. 删除GridFS上的文件
        gridFSBucket.delete(new ObjectId(articleMongoId));

        // 3. 删除消费端的HTML文件
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
     * 撤回文章
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

        //4. 删除ES中的数据 TODO ES
        DeleteRequest request = new DeleteRequest("articles", articleId);

        try {
            restHighLevelClient.delete(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("ES中没有他的该文章的数据");
            e.printStackTrace();
        }
    }


    /**
     * article有一个字端，与mongo关联，这里是更新
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
     * mq 收到消息，开始处理 也需要把文章上传到es中
     * @param articleId
     */

    @Transactional
    @Override
    public void updateArticleToPublish(String articleId) {
        // 更新状态
        Article article = new Article();
        article.setId(articleId);
        article.setIsAppoint(ArticleAppointType.IMMEDIATELY.type);

        // 上传到ES中
        Article articleEO = articleMapper.selectByPrimaryKey(articleId);
        IndexRequest request = new IndexRequest("articles").id(articleEO.getId().toString());
        // 2.准备请求参数DSL，其实就是文档的JSON字符串
        request.source(JsonUtils.objectToJson(articleEO), XContentType.JSON).type();
        // 3.发送请求
        try {
            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.info("上传到ES中失败", e);
            e.printStackTrace();
        }
        articleMapper.updateByPrimaryKeySelective(article);
    }


    /**
     * 构建条件
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
