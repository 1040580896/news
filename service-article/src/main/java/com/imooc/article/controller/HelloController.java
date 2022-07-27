package com.imooc.article.controller;

import com.imooc.api.config.RabbitMQConfig;
import com.imooc.api.config.RabbitMQDelayConfig;
import com.imooc.article.stream.StreamService;
import com.imooc.grace.result.IMOOCJSONResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @program: news
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-31 19:40
 **/
@RestController
@RequestMapping("producer")
public class HelloController {

    private static final Logger log = LoggerFactory.getLogger(HelloController.class);


    @Autowired
    private StreamService streamService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/stream")
    public Object stream() {
       streamService.sendMsg();

        for (int i = 0 ; i < 10 ; i ++ ) {
            streamService.eat("我吃了第" + (i+1) + "只饺子~");
        }

        return "ok~~!!!";
    }

    @GetMapping("/hello")
    public Object hello() {

        /**
         * RabbitMQ 的路由规则 routing key
         *  display.*.* -> * 代表一个占位符
         *      例：
         *          display.do.download  匹配
         *          display.do.upload.done   不匹配
         *
         * display.# -> # 代表任意多个占位符
         *      例:
         *          display.do.download  匹配
         *          display.do.upload.done.over   匹配
         *
         *
         */

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_ARTICLE,
                "article.6.do",
                "这是发送者");
        return IMOOCJSONResult.ok("hello");
    }

    @GetMapping("/delay")
    public Object delay() {

        MessagePostProcessor messagePostProcessor = new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                // 设置消息的持久
                message.getMessageProperties()
                        .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                // 设置消息延迟的时间，单位ms毫秒
                message.getMessageProperties()
                        .setDelay(5000);
                return message;
            }
        };

        rabbitTemplate.convertAndSend(
                RabbitMQDelayConfig.EXCHANGE_DELAY,
                "delay.demo",
                "这是一条延迟消息~~",
                messagePostProcessor);

        System.out.println("生产者发送的延迟消息：" + new Date());

        return "OK";
    }

    @GetMapping("/delay2")
    public Object delay2() {
        Message message = MessageBuilder.withBody("这是一条延迟消息~~".getBytes(StandardCharsets.UTF_8))
                .setHeader("x-delay", 5000)
                .build();
        rabbitTemplate.convertAndSend(RabbitMQDelayConfig.EXCHANGE_DELAY, "publish.delay.6", message);

        return "ok";
    }


}
