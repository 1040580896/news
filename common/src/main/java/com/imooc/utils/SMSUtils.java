package com.imooc.utils;

import com.imooc.utils.extend.HttpUtils;
import lombok.Data;
import org.apache.http.HttpResponse;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: news
 * @description:
 * @author: xiaokaixin
 * @create: 2022-06-09 15:51
 **/
@Component
@Data
public class SMSUtils {
    private String host;
    private String path;
    private String tpl_id;

    public void sendSmsCode(String phone,String code){


        //发送短信
        String host = "http://dingxin.market.alicloudapi.com";
        String path = "/dx/sendSms";
        String method = "POST";
        String appcode = "ac017882333e4e6cbcee96b49d0b4c50";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode); //g固定格式，注意空格

        Map<String, String> querys = new HashMap<String, String>();
        querys.put("mobile", phone);

        // String code = c;
        querys.put("param", "code:"+code);   //code：开头，支持数字和字母  验证码 转换json数据
        querys.put("tpl_id", "TP1711063"); //测试模版id


        Map<String, String> bodys = new HashMap<String, String>();


        try {
            /**
             * 重要提示如下:
             * HttpUtils请从
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java
             * 下载
             *
             * 相应的依赖请参照
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
             */
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            //System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();

        }

    }
}
