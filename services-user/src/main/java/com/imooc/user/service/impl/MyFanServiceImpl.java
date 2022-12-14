package com.imooc.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.imooc.api.service.BaseService;
import com.imooc.enums.Sex;
import com.imooc.pojo.AppUser;
import com.imooc.pojo.Fans;
import com.imooc.pojo.eo.ArticleEO;
import com.imooc.pojo.eo.FansEO;
import com.imooc.pojo.vo.FansCountsVO;
import com.imooc.pojo.vo.RegionRatioVO;
import com.imooc.user.mapper.FansMapper;
import com.imooc.user.service.MyFanService;
import com.imooc.user.service.UserService;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.PagedGridResult;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.n3r.idworker.Sid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MyFanServiceImpl extends BaseService implements MyFanService {

    private static final Logger log = LoggerFactory.getLogger(MyFanServiceImpl.class);
    @Autowired
    private FansMapper fansMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private Sid sid;

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Override
    public boolean isMeFollowThisWriter(String writerId, String fanId) {

        Fans fan = new Fans();
        fan.setFanId(fanId);
        fan.setWriterId(writerId);

        int count = fansMapper.selectCount(fan);

        return count > 0 ? true : false;
    }

    /**
     * ?????????????????????????????????
     * @param writerId
     * @param fanId
     */
    @Transactional
    @Override
    public void follow(String writerId, String fanId) {
        // ???????????????????????????
        AppUser fanInfo = userService.getUser(fanId);

        String fanPkId = sid.nextShort();

        Fans fans = new Fans();
        fans.setId(fanPkId);
        fans.setFanId(fanId);
        fans.setWriterId(writerId);

        fans.setFace(fanInfo.getFace());
        fans.setFanNickname(fanInfo.getNickname());
        fans.setSex(fanInfo.getSex());
        fans.setProvince(fanInfo.getProvince());

        fansMapper.insert(fans);

        // redis ?????????????????????
        redis.increment(REDIS_WRITER_FANS_COUNTS + ":" + writerId, 1);
        // redis ??????????????????????????????????????????
        redis.increment(REDIS_MY_FOLLOW_COUNTS + ":" + fanId, 1);

        // ???????????????es??? todo
        try {
            FansEO fansEO = new FansEO();
            BeanUtils.copyProperties(fans,fansEO);
            IndexRequest request = new IndexRequest("fans").id(fansEO.getId().toString());
            request.source(JsonUtils.objectToJson(fansEO), XContentType.JSON);
            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("???????????????es?????????");
            e.printStackTrace();
        }
    }

    /**
     * ????????????
     * @param writerId
     * @param fanId
     */
    @Transactional
    @Override
    public void unfollow(String writerId, String fanId) {

        Fans fans = new Fans();
        fans.setWriterId(writerId);
        fans.setFanId(fanId);
        Fans fans1 = fansMapper.selectOne(fans);
        log.info(fans1.toString());
        System.out.println(fans1.getFanId());

        fansMapper.delete(fans);

        // redis ?????????????????????
        redis.decrement(REDIS_WRITER_FANS_COUNTS + ":" + writerId, 1);
        // redis ??????????????????????????????????????????
        redis.decrement(REDIS_MY_FOLLOW_COUNTS + ":" + fanId, 1);

        try {
            // ??????ES???????????? todo
            DeleteRequest request = new DeleteRequest("fans", fans1.getId());
            restHighLevelClient.delete(request,RequestOptions.DEFAULT);
        } catch (IOException e) {
            System.out.println("???es?????????????????????");
            e.printStackTrace();
        }


    }

    /**
     * ??????????????????????????????
     * @param writerId
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PagedGridResult queryMyFansList(String writerId,
                                           Integer page,
                                           Integer pageSize) {
        Fans fans = new Fans();
        fans.setWriterId(writerId);

        PageHelper.startPage(page, pageSize);
        List<Fans> list = fansMapper.select(fans);
        return setterPagedGrid(list, page);
    }

    // todo ??? ES ???????????????
    @Override
    public PagedGridResult queryMyFansESList(String writerId, Integer page, Integer pageSize) throws IOException {
        // 1?????????request
        if(page<1) return null;
        SearchRequest request = new SearchRequest("fans");
        // 2?????????
        request.source().from((page-1)*pageSize).size(pageSize);
        // 3???term??????
        request.source().query(QueryBuilders.termQuery("writerId",writerId));
        // 4???????????????
        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        // 5???????????????
        List<FansEO> fansEOS = handleResponse(response);
        PagedGridResult gridResult =new PagedGridResult();
        gridResult.setRecords(searchHits.getTotalHits().value);
        gridResult.setTotal(searchHits.getTotalHits().value%pageSize==0?searchHits.getTotalHits().value/pageSize:searchHits.getTotalHits().value/pageSize+1);
        gridResult.setRows(fansEOS);
        return gridResult;
    }

    SearchHits searchHits;

    // ??????ES??????
    private List<FansEO> handleResponse(SearchResponse response) {
        List<FansEO> data = new ArrayList<>();
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
            FansEO fansEO = JSON.parseObject(json, FansEO.class);
            // 4.6.??????????????????
            // 1)????????????map
            Map<String, HighlightField> map = hit.getHighlightFields();
            // if (map != null && !map.isEmpty()) {
            //     // 2???????????????????????????????????????
            //     HighlightField highlightField = map.get("title");
            //     // 3?????????????????????????????????????????????1?????????
            //     String hName = highlightField.getFragments()[0].toString();
            //     // 4????????????????????????HotelDoc???
            //     articleEO.setTitle(hName);
            // }
            // 4.7.??????
            // System.out.println(hotelDoc);
            data.add(fansEO);
        }
        return data;

    }



    /**
     * ????????????????????????
     * @param writerId
     * @param sex
     * @return
     */
    @Override
    public Integer queryFansCounts(String writerId, Sex sex) {
        Fans fans = new Fans();
        fans.setWriterId(writerId);
        fans.setSex(sex.type);

        Integer count = fansMapper.selectCount(fans);
        return count;
    }

    public static final String[] regions = {"??????", "??????", "??????", "??????",
            "??????", "??????", "??????", "??????", "?????????", "??????", "??????", "??????", "??????", "??????", "??????",
            "??????", "??????", "??????", "??????", "??????", "??????", "??????", "??????", "??????", "??????", "??????", "??????",
            "?????????", "??????", "??????", "??????", "??????",
            "??????", "??????"};

    /**
     * ??????????????????????????????
     * @param writerId
     * @return
     */
    @Override
    public List<RegionRatioVO> queryRegionRatioCounts(String writerId) {
        Fans fans = new Fans();
        fans.setWriterId(writerId);

        List<RegionRatioVO> list = new ArrayList<>();
        for (String r : regions) {
            fans.setProvince(r);
            Integer count = fansMapper.selectCount(fans);

            RegionRatioVO regionRatioVO = new RegionRatioVO();
            regionRatioVO.setName(r);
            regionRatioVO.setValue(count);

            list.add(regionRatioVO);
        }

        return list;
    }


    // todo
    @Override
    public FansCountsVO queryFansESCounts(String writerId) throws IOException {
        FansCountsVO fansCountsVO = new FansCountsVO();
        /**
         * # ??????????????????
         * GET /fans/_search
         * {
         *   "query": {
         *     "term": {
         *       "writerId":"220614FN44ZCP46W"
         *     }
         *     },
         *   "size": 0,
         *   "aggs": {
         *     "sexCounts": {
         *       "terms": {
         *         "field": "sex"
         *       }
         *     }
         *   }
         * }
         */
        // 1.??????Request
        SearchRequest request = new SearchRequest("fans");
        // 2?????????????????????
        request.source().query(QueryBuilders.termQuery("writerId",writerId));
        // 3???agg
        request.source().aggregation(AggregationBuilders
                .terms("sexCounts")
                .field("sex")
                .size(2));

        // 4.????????????
        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        Aggregations aggregations = response.getAggregations();
        // List<String> sexCounts = getAggByName(aggregations, "sexCounts");
        Terms brandTerms = aggregations.get("sexCounts");
        // ??????buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();

        // ????????????
        for (Terms.Bucket bucket : buckets) {
            Long docCount = bucket.getDocCount();
            Long key = (Long)bucket.getKey();

            if (key.intValue() == Sex.woman.type) {
                fansCountsVO.setWomanCounts(docCount.intValue());
            } else if (key.intValue() == Sex.man.type) {
                fansCountsVO.setManCounts(docCount.intValue());
            }
        }
        if (buckets == null || buckets.size() == 0) {
            fansCountsVO.setManCounts(0);
            fansCountsVO.setWomanCounts(0);
        }
        return fansCountsVO;
    }


    // todo
    @Override
    public List<RegionRatioVO> queryRegionRatioESCounts(String writerId) throws IOException {

        /**
         * # ??????????????????
         * GET /fans/_search
         * {
         *   "query": {
         *     "term": {
         *       "writerId":"220614FN44ZCP46W"
         *     }
         *     },
         *   "size": 0,
         *   "aggs": {
         *     "regionCounts": {
         *       "terms": {
         *         "field": "province"
         *       }
         *     }
         *   }
         * }
         */
        // 1.??????Request
        SearchRequest request = new SearchRequest("fans");
        // 2?????????????????????
        request.source().query(QueryBuilders.termQuery("writerId",writerId));
        // 3???agg
        request.source().aggregation(AggregationBuilders
                .terms("regionCounts")
                .field("province")
                .size(100));

        // 4.????????????
        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        Aggregations aggregations = response.getAggregations();
        // List<String> sexCounts = getAggByName(aggregations, "sexCounts");
        Terms brandTerms = aggregations.get("regionCounts");
        // ??????buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();

        List<RegionRatioVO> list = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            Long docCount = bucket.getDocCount();
            String key = (String)bucket.getKey();

            System.out.println(key);
            System.out.println(docCount);

            RegionRatioVO regionRatioVO = new RegionRatioVO();
            regionRatioVO.setName(key);
            regionRatioVO.setValue(docCount.intValue());
            list.add(regionRatioVO);
        }
        return list;
    }
    /**
     * ????????????????????????
     * @param relationId
     * @param fanId
     */
    @Override
    public void forceUpdateFanInfo(String relationId, String fanId) throws IOException {

        // 1. ??????fanId??????????????????
        AppUser user = userService.getUser(fanId);

        // 2. ?????????????????????db???es???
        Fans fans = new Fans();
        fans.setId(relationId);

        fans.setFace(user.getFace());
        fans.setFanNickname(user.getNickname());
        fans.setSex(user.getSex());
        fans.setProvince(user.getProvince());

        fansMapper.updateByPrimaryKeySelective(fans);

        // 1?????????request
        UpdateRequest request = new UpdateRequest("fans", relationId);
        //2???????????????

        request.doc(
                "face",user.getFace(),
                "fanNickname",user.getNickname(),
                "sex",user.getSex(),
                "province",user.getProvince()
        );
        // 3???????????????
        restHighLevelClient.update(request,RequestOptions.DEFAULT);

    }
}
