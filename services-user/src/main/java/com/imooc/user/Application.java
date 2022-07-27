package com.imooc.user;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import tk.mybatis.spring.annotation.MapperScan;


/**
 * @program: news
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-31 19:44
 **/
@SpringBootApplication(exclude = MongoAutoConfiguration.class)
@MapperScan(basePackages = "com.imooc.user.mapper")
@ComponentScan(basePackages = {"com.imooc","org.n3r.idworker"})
@EnableEurekaClient  // 开启eureka client 注册到server中
@EnableCircuitBreaker  // 开启hystrix 熔断机制
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // elastic search
    @Bean
    public RestHighLevelClient restHighLevelClient(){
        return new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://124.222.219.104:9200")
        ));
    }

}
