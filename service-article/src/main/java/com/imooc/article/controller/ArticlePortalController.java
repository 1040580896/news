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


    // todo ES ??????
    @Override
    public GraceJSONResult eslist(String keyword, Integer category, Integer page, Integer pageSize) throws IOException {
        /**
         * es?????????
         *      1. ?????????????????????????????????
         *      2. ????????????????????????
         *      3. ?????????????????????
         */

        // es???????????????0?????????????????????????????????page??????-1
        if (page < 1) return null;
        // 1.??????request
        SearchRequest request = new SearchRequest("articles");

        // ?????????1?????????
        if (StringUtils.isBlank(keyword) && category == null) {
            //http://127.0.0.1:8001/portal/article/es/list?page=1&pageSize=10&keyword=&category
            // 2.??????????????????
            request.source().from((page - 1) * pageSize).size(pageSize);
        }

        // ?????????2?????????
        if (StringUtils.isBlank(keyword) && category != null) {
            request.source().query(QueryBuilders.termQuery("categoryId", category));
        }

        // ?????????3?????????
        if (StringUtils.isNotBlank(keyword) && category == null) {
            //  ???????????? https://developer.aliyun.com/article/827856
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("title");
            highlightBuilder.requireFieldMatch(false);
            highlightBuilder.preTags("<font color='red'>");
            highlightBuilder.postTags("</font>");
            request.source().highlighter(highlightBuilder);
            request.source().query(QueryBuilders.matchQuery("title", keyword));
        }


        // 3.???????????????????????????
        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        List<ArticleEO> articleEOS = handleResponse(response);
        List<Article> articleList = new ArrayList<>();
        for (ArticleEO a : articleEOS) {
//            System.out.println(a);
            Article article = new Article();
            BeanUtils.copyProperties(a, article);
            articleList.add(article);
        }

        // ????????????????????????grid??????
        PagedGridResult gridResult = new PagedGridResult();
        gridResult.setRows(articleList);
        gridResult.setPage(page + 1);
        // ????????? todo ?????????
        gridResult.setTotal(searchHits.getTotalHits().value%pageSize==0?searchHits.getTotalHits().value/pageSize:searchHits.getTotalHits().value/pageSize+1);
        // ?????????
        gridResult.setRecords(searchHits.getTotalHits().value);

        gridResult = rebuildArticleGrid(gridResult);

        return GraceJSONResult.ok(gridResult);

    }

    SearchHits searchHits;

    // ??????ES??????
    private List<ArticleEO> handleResponse(SearchResponse response) {
        List<ArticleEO> data = new ArrayList<>();
        searchHits = response.getHits();
        // 4.1.?????????
        long total = searchHits.getTotalHits().value;
        System.out.println("????????????" + total);
        // 4.2.??????????????????
        SearchHit[] hits = searchHits.getHits();
        // 4.3.??????
        for (SearchHit hit : hits) {
            // 4.4.??????source
            String json = hit.getSourceAsString();
            // 4.5.???????????????????????????
            ArticleEO articleEO = JSON.parseObject(json, ArticleEO.class);
            // 4.6.??????????????????
            // 1)????????????map
            Map<String, HighlightField> map = hit.getHighlightFields();
            if (map != null && !map.isEmpty()) {
                // 2???????????????????????????????????????
                HighlightField highlightField = map.get("title");
                // 3?????????????????????????????????????????????1?????????
                String hName = highlightField.getFragments()[0].toString();
                // 4????????????????????????HotelDoc???
                articleEO.setTitle(hName);
            }
            // 4.7.??????
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

        // ??????
        PagedGridResult gridResult
                = articlePortalService.queryIndexArticleList(keyword,
                category,
                page,
                pageSize);

        // // START
        // List<Article> list = (List<Article>) gridResult.getRows();
        //
        // // 1??????????????????id??????
        // Set<String > idSet = new HashSet<>();
        // for (Article article : list) {
        //     // System.out.println(article.getPublishUserId());
        //     idSet.add(article.getPublishUserId());
        // }
        //
        // // 2?????????????????????(restTemplate) ????????????????????????????????? ??????
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
        // // 3???????????????list?????????????????????
        // List<IndexArticleVO> indexArticleList = new ArrayList<>();
        // for (Article a : list) {
        //     IndexArticleVO indexArticleVO = new IndexArticleVO();
        //     BeanUtils.copyProperties(a, indexArticleVO);
        //
        //     // 3.1 ???publisherList?????????????????????????????????
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
     * ????????????????????????
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
     * ??????????????????
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
     * ??????????????????
     *
     * @param articleId
     * @return
     */
    public GraceJSONResult detail(String articleId) {
        ArticleDetailVO detailVO = articlePortalService.queryDetail(articleId);

        Set<String> idSet = new HashSet();
        idSet.add(detailVO.getPublishUserId());
        // ??????????????????????????????
        List<AppUserVO> publisherList = getPublisherList(idSet);

        if (!publisherList.isEmpty()) {
            detailVO.setPublishUserName(publisherList.get(0).getNickname());
        }

        // ???????????????+1
        detailVO.setReadCounts(
                getCountsFromRedis(REDIS_ARTICLE_READ_COUNTS + ":" + articleId));

        return GraceJSONResult.ok(detailVO);
    }

    /**
     * ?????????????????????
     *
     * @param articleId
     * @return
     */
    @Override
    public Integer readCounts(String articleId) {
        return getCountsFromRedis(REDIS_ARTICLE_READ_COUNTS + ":" + articleId);
    }


    /**
     * ????????????id????????????
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
     * ??????????????????
     *
     * @param gridResult
     * @return
     */
    private PagedGridResult rebuildArticleGrid(PagedGridResult gridResult) {
        // START

        List<Article> list = (List<Article>) gridResult.getRows();

        // 1. ???????????????id??????
        Set<String> idSet = new HashSet<>();
        List<String> idList = new ArrayList<>();
        for (Article a : list) {
//            System.out.println(a.getPublishUserId());
            // 1.1 ??????????????????set
            idSet.add(a.getPublishUserId());
            // 1.2 ????????????id???list
            idList.add(REDIS_ARTICLE_READ_COUNTS + ":" + a.getId());
        }
        System.out.println(idSet.toString());
        // 2 ??????redis???mget????????????api?????????????????????
        List<String> readCountsRedisList = redis.mget(idList);

        // ????????????????????????????????????????????????
        List<AppUserVO> publisherList = getPublisherList(idSet);
//        for (AppUserVO u : publisherList) {
//            System.out.println(u.toString());
//        }

        // 3. ????????????list?????????????????????
        List<IndexArticleVO> indexArticleList = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            IndexArticleVO indexArticleVO = new IndexArticleVO();
            Article a = list.get(i);
            BeanUtils.copyProperties(a, indexArticleVO);

            // 3.1 ???publisherList?????????????????????????????????
            AppUserVO publisher = getUserIfPublisher(a.getPublishUserId(), publisherList);
            indexArticleVO.setPublisherVO(publisher);

            // 3.2 ?????????????????????????????????????????????
            String redisCountsStr = readCountsRedisList.get(i);
            int readCounts = 0;
            if (StringUtils.isNotBlank(redisCountsStr)) {
                readCounts = Integer.valueOf(redisCountsStr);
            }

            //3.3 ????????????
            indexArticleVO.setReadCounts(readCounts);

            indexArticleList.add(indexArticleVO);
        }


        gridResult.setRows(indexArticleList);
// END
        return gridResult;
    }

    // ??????????????????????????????????????????????????????????????????
    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private UserControllerApi userControllerApi;

    // ????????????????????????????????????????????????
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
        // ????????????????????????ip??????????????????key????????????redis????????????ip???????????????????????????????????????????????????
        redis.setnx(REDIS_ALREADY_READ + ":" + articleId + ":" + userIp, userIp);

        redis.increment(REDIS_ARTICLE_READ_COUNTS + ":" + articleId, 1);
        return GraceJSONResult.ok();
    }

}
