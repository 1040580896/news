package com.imooc.article.controller;

import com.alibaba.fastjson.JSON;
import com.imooc.api.BaseController;
import com.imooc.api.controller.article.ArticlePortalControllerApi;
import com.imooc.api.controller.user.UserControllerApi;
import com.imooc.article.service.ArticlePortalService;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.pojo.Article;
import com.imooc.pojo.eo.ArticleEO;
import com.imooc.pojo.vo.AppUserVO;
import com.imooc.pojo.vo.ArticleDetailVO;
import com.imooc.pojo.vo.IndexArticleVO;
import com.imooc.utils.IPUtil;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.PagedGridResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import tk.mybatis.mapper.entity.Example;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

@RestController
public class ArticlePortalController extends BaseController implements ArticlePortalControllerApi {

    final static Logger logger = LoggerFactory.getLogger(ArticlePortalController.class);

    @Autowired
    private ArticlePortalService articlePortalService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    RestHighLevelClient restHighLevelClient;


    // todo ES 搜索
    @Override
    public GraceJSONResult eslist(String keyword, Integer category, Integer page, Integer pageSize) throws IOException {
        /**
         * es查询：
         *      1. 首页默认查询，不带参数
         *      2. 按照文章分类查询
         *      3. 按照关键字查询
         */

        // es的页面是从0开始计算的，所以在这里page需要-1
        if (page < 1) return null;
        // 1.准备request
        SearchRequest request = new SearchRequest("articles");

        // 符合第1种情况
        if (StringUtils.isBlank(keyword) && category == null) {
            //http://127.0.0.1:8001/portal/article/es/list?page=1&pageSize=10&keyword=&category
            // 2.准备请求参数
            request.source().from((page - 1) * pageSize).size(pageSize);
        }

        // 符合第2种情况
        if (StringUtils.isBlank(keyword) && category != null) {
            request.source().query(QueryBuilders.termQuery("categoryId", category));
        }

        // 符合第3种情况
        if (StringUtils.isNotBlank(keyword) && category == null) {
            //  开启高亮 https://developer.aliyun.com/article/827856
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("title");
            highlightBuilder.requireFieldMatch(false);
            highlightBuilder.preTags("<font color='red'>");
            highlightBuilder.postTags("</font>");
            request.source().highlighter(highlightBuilder);
            request.source().query(QueryBuilders.matchQuery("title", keyword));
        }


        // 3.发送请求，得到响应
        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        List<ArticleEO> articleEOS = handleResponse(response);
        List<Article> articleList = new ArrayList<>();
        for (ArticleEO a : articleEOS) {
//            System.out.println(a);
            Article article = new Article();
            BeanUtils.copyProperties(a, article);
            articleList.add(article);
        }

        // 重新封装成之前的grid格式
        PagedGridResult gridResult = new PagedGridResult();
        gridResult.setRows(articleList);
        gridResult.setPage(page + 1);
        // 总页数 todo 有问题
        gridResult.setTotal(searchHits.getTotalHits().value%pageSize==0?searchHits.getTotalHits().value/pageSize:searchHits.getTotalHits().value/pageSize+1);
        // 总条数
        gridResult.setRecords(searchHits.getTotalHits().value);

        gridResult = rebuildArticleGrid(gridResult);

        return GraceJSONResult.ok(gridResult);

    }

    SearchHits searchHits;

    // 处理ES数据
    private List<ArticleEO> handleResponse(SearchResponse response) {
        List<ArticleEO> data = new ArrayList<>();
        searchHits = response.getHits();
        // 4.1.总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("总条数：" + total);
        // 4.2.获取文档数组
        SearchHit[] hits = searchHits.getHits();
        // 4.3.遍历
        for (SearchHit hit : hits) {
            // 4.4.获取source
            String json = hit.getSourceAsString();
            // 4.5.反序列化，非高亮的
            ArticleEO articleEO = JSON.parseObject(json, ArticleEO.class);
            // 4.6.处理高亮结果
            // 1)获取高亮map
            Map<String, HighlightField> map = hit.getHighlightFields();
            if (map != null && !map.isEmpty()) {
                // 2）根据字段名，获取高亮结果
                HighlightField highlightField = map.get("title");
                // 3）获取高亮结果字符串数组中的第1个元素
                String hName = highlightField.getFragments()[0].toString();
                // 4）把高亮结果放到HotelDoc中
                articleEO.setTitle(hName);
            }
            // 4.7.打印
            // System.out.println(hotelDoc);
            data.add(articleEO);
        }
        return data;

    }


    @Override
    public GraceJSONResult list(String keyword,
                                Integer category,
                                Integer page,
                                Integer pageSize) {
        if (page == null) {
            page = COMMON_START_PAGE;
        }

        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        // 查询
        PagedGridResult gridResult
                = articlePortalService.queryIndexArticleList(keyword,
                category,
                page,
                pageSize);

        // // START
        // List<Article> list = (List<Article>) gridResult.getRows();
        //
        // // 1、构建发布者id列表
        // Set<String > idSet = new HashSet<>();
        // for (Article article : list) {
        //     // System.out.println(article.getPublishUserId());
        //     idSet.add(article.getPublishUserId());
        // }
        //
        // // 2、发起远程调用(restTemplate) 、请求用户服务获得用户 列表
        // String userServerUrlExecute = "http://user.imoocnews.com:8003/user/queryByIds?userIds=" + JsonUtils.objectToJson(idSet);
        //
        // ResponseEntity<GraceJSONResult> responseEntity
        //         = restTemplate.getForEntity(userServerUrlExecute, GraceJSONResult.class);
        // GraceJSONResult bodyResult = responseEntity.getBody();
        // List<AppUserVO> publisherList = null;
        //
        // if (bodyResult.getStatus() == 200) {
        //     String userJson = JsonUtils.objectToJson(bodyResult.getData());
        //     publisherList = JsonUtils.jsonToList(userJson, AppUserVO.class);
        // }
        //
        //
        // // 3、拼接两个list，重组文章列表
        // List<IndexArticleVO> indexArticleList = new ArrayList<>();
        // for (Article a : list) {
        //     IndexArticleVO indexArticleVO = new IndexArticleVO();
        //     BeanUtils.copyProperties(a, indexArticleVO);
        //
        //     // 3.1 从publisherList中获得发布者的基本信息
        //     AppUserVO publisher  = getUserIfPublisher(a.getPublishUserId(), publisherList);
        //     indexArticleVO.setPublisherVO(publisher);
        //     indexArticleList.add(indexArticleVO);
        // }
        //
        // gridResult.setRows(indexArticleList);
        // END

        gridResult = rebuildArticleGrid(gridResult);
        return GraceJSONResult.ok(gridResult);
    }

    /**
     * 首页查询热闻列表
     *
     * @return
     */
    @Override
    public GraceJSONResult hotList() {
        return GraceJSONResult.ok(articlePortalService.queryHotList());
    }

    @Override
    public GraceJSONResult queryArticleListOfWriter(String writerId, Integer page, Integer pageSize) {
        System.out.println("writerId=" + writerId);

        if (page == null) {
            page = COMMON_START_PAGE;
        }

        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        PagedGridResult gridResult = articlePortalService.queryArticleListOfWriter(writerId, page, pageSize);
        gridResult = rebuildArticleGrid(gridResult);
        return GraceJSONResult.ok(gridResult);
    }

    /**
     * 查询近期佳文
     *
     * @param writerId
     * @return
     */
    @Override
    public GraceJSONResult queryGoodArticleListOfWriter(String writerId) {
        PagedGridResult gridResult = articlePortalService.queryGoodArticleListOfWriter(writerId);
        return GraceJSONResult.ok(gridResult);
    }

    /**
     * 文章详情查询
     *
     * @param articleId
     * @return
     */
    public GraceJSONResult detail(String articleId) {
        ArticleDetailVO detailVO = articlePortalService.queryDetail(articleId);

        Set<String> idSet = new HashSet();
        idSet.add(detailVO.getPublishUserId());
        // 远程调用获取用户信息
        List<AppUserVO> publisherList = getPublisherList(idSet);

        if (!publisherList.isEmpty()) {
            detailVO.setPublishUserName(publisherList.get(0).getNickname());
        }

        // 文章阅读数+1
        detailVO.setReadCounts(
                getCountsFromRedis(REDIS_ARTICLE_READ_COUNTS + ":" + articleId));

        return GraceJSONResult.ok(detailVO);
    }

    /**
     * 获得文章阅读数
     *
     * @param articleId
     * @return
     */
    @Override
    public Integer readCounts(String articleId) {
        return getCountsFromRedis(REDIS_ARTICLE_READ_COUNTS + ":" + articleId);
    }


    /**
     * 根据用户id匹配信息
     *
     * @param publishUserId
     * @param publisherList
     * @return
     */
    private AppUserVO getUserIfPublisher(String publishUserId, List<AppUserVO> publisherList) {
        for (AppUserVO appUserVO : publisherList) {
            if (appUserVO.getId().equalsIgnoreCase(publishUserId)) {
                return appUserVO;
            }
        }
        return null;
    }


    /**
     * 构建文章信息
     *
     * @param gridResult
     * @return
     */
    private PagedGridResult rebuildArticleGrid(PagedGridResult gridResult) {
        // START

        List<Article> list = (List<Article>) gridResult.getRows();

        // 1. 构建发布者id列表
        Set<String> idSet = new HashSet<>();
        List<String> idList = new ArrayList<>();
        for (Article a : list) {
//            System.out.println(a.getPublishUserId());
            // 1.1 构建发布者的set
            idSet.add(a.getPublishUserId());
            // 1.2 构建文章id的list
            idList.add(REDIS_ARTICLE_READ_COUNTS + ":" + a.getId());
        }
        System.out.println(idSet.toString());
        // 2 发起redis的mget批量查询api，获得对应的值
        List<String> readCountsRedisList = redis.mget(idList);

        // 发起远程调用，获得用户的基本信息
        List<AppUserVO> publisherList = getPublisherList(idSet);
//        for (AppUserVO u : publisherList) {
//            System.out.println(u.toString());
//        }

        // 3. 拼接两个list，重组文章列表
        List<IndexArticleVO> indexArticleList = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            IndexArticleVO indexArticleVO = new IndexArticleVO();
            Article a = list.get(i);
            BeanUtils.copyProperties(a, indexArticleVO);

            // 3.1 从publisherList中获得发布者的基本信息
            AppUserVO publisher = getUserIfPublisher(a.getPublishUserId(), publisherList);
            indexArticleVO.setPublisherVO(publisher);

            // 3.2 重新组装设置文章列表中的阅读量
            String redisCountsStr = readCountsRedisList.get(i);
            int readCounts = 0;
            if (StringUtils.isNotBlank(redisCountsStr)) {
                readCounts = Integer.valueOf(redisCountsStr);
            }

            //3.3 开始组装
            indexArticleVO.setReadCounts(readCounts);

            indexArticleList.add(indexArticleVO);
        }


        gridResult.setRows(indexArticleList);
// END
        return gridResult;
    }

    // 注入服务发现，可以获得已经注册的服务相关信息
    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private UserControllerApi userControllerApi;

    // 发起远程调用，获得用户的基本信息
    private List<AppUserVO> getPublisherList(Set idSet) {

        String serviceId = "SERVICE-USER";

        GraceJSONResult bodyResult = userControllerApi.queryByIds(JsonUtils.objectToJson(idSet));

        List<AppUserVO> publisherList = null;
        if (bodyResult.getStatus() == 200) {
            String userJson = JsonUtils.objectToJson(bodyResult.getData());
            publisherList = JsonUtils.jsonToList(userJson, AppUserVO.class);
        } else {
            publisherList = new ArrayList<>();
        }
        return publisherList;
    }


    @Override
    public GraceJSONResult readArticle(String articleId, HttpServletRequest request) {

        String userIp = IPUtil.getRequestIp(request);
        // 设置针对当前用户ip的永久存在的key，存入到redis，表示该ip的用户已经阅读过了，无法累加阅读量
        redis.setnx(REDIS_ALREADY_READ + ":" + articleId + ":" + userIp, userIp);

        redis.increment(REDIS_ARTICLE_READ_COUNTS + ":" + articleId, 1);
        return GraceJSONResult.ok();
    }

}
