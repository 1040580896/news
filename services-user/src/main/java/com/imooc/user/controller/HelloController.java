package com.imooc.user.controller;

import com.imooc.api.controller.user.HelloControllerApi;
import com.imooc.grace.result.IMOOCJSONResult;
import com.imooc.utils.RedisOperator;
import com.sun.org.apache.xpath.internal.operations.Gt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private RedisOperator redis;


    public Object hello(){
        log.debug("debug hello");
        log.info("debug hello");
        log.warn("debug hello");
        log.error("debug hello");
        return IMOOCJSONResult.ok("hello");
    }

    @GetMapping("/redis")
    public Object redis(){
        redis.set("age","22");
        return IMOOCJSONResult.ok(redis.get("age"));
    }
}
