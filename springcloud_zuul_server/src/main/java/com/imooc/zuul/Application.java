package com.imooc.zuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.ComponentScan;


/**
 * @program: news
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-31 19:44
 **/
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,MongoAutoConfiguration.class})
@ComponentScan(basePackages = {"com.imooc", "org.n3r.idworker"})
@EnableEurekaClient
//@EnableZuulServer
@EnableZuulProxy       // @EnableZuulProxy是@EnableZuulServer的一个增强升级版，当zuul和eureka、ribbon等组件共同使用，则使用增强版即可
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
