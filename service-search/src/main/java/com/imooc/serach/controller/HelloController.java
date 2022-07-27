package com.imooc.serach.controller;

import com.imooc.api.controller.user.HelloControllerApi;
import com.imooc.grace.result.IMOOCJSONResult;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * @program: news
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-31 19:40
 **/
@RestController
public class HelloController implements HelloControllerApi {

    private static final Logger log = LoggerFactory.getLogger(HelloController.class);

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @GetMapping("/hello")
    public Object hello() throws IOException {
        // 1.准备Request
        GetIndexRequest request = new GetIndexRequest("articles");
        // 3.发送请求
        boolean isExists = restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);

        System.out.println(isExists ? "存在" : "不存在");
        return IMOOCJSONResult.ok("hello");
    }

}
