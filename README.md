
```
http://user.imoocnews.com:8003/doc.html
http://writer.imoocnews.com:9090/imooc-news/writer/passport.html
```



# day 1

## 聚合工程

```
1. 聚合工程可以分为顶级项目（顶级工程，父工程）与子工程（子module模块），
    这两者的关系其实就是父子继承的关系，子工程在maven中可以称之为module，
    模块与模块之间是平级的，是可以相互依赖的。
2. 子模块可以使用顶级工程中所有的资源（依赖），子模块之间如果有要使用资源的话，必须构建依赖（构建关系）
3. 一个顶级工程是可以由多个不同的子工程共同组合而成。
```



> dependencyManagement

```
使用dependencyManagement的目的是为了保证父工程的干净，
也就是说父工程他只负责管理依赖，以及依赖的版本，而不会导入额外的jar依赖。
如此一来父工程的职责就很单一了，而且也符合了面向对象开发的父子继承关系，
依赖的导入只有在各自的子工程中才会进行导入。
```





```
common:
通用工程
包含了一些工具类，枚举类，封装的一些公共方法以及一些第三方组件等


model
模型工程，所有的子工程以及微服务中所涉及到的模型实体类都在此管理
可以包含一些 *pojo, *Bean, *Entity, vo, bo, dto 等。

service-api
接口工程，集中管理所有的controller中的接口，为了更好的统一管理微服务
        
```





# day 2

## API

```java
api 的作用：
api 就相当于企业的领导，老板，部门经理
其他的服务层都是实现，他们就相当于员工，只做事情
老板（开发人员）来看一下每个人（服务）的进度，做什么事。
老板不会去问员工，他只会对接部门经理。
那么这个里的所有的api 接口就是统一在这里进行管理和调度的，微服务也是如此



运作：
现在的所有接口都在此暴露，实现都是在各自的微服务中
本项目只写接口，不写实现，实现在各自的微服务工程中，因为以业务来划分的微服务有很多
controller也会分散在各个微服务工程中，一旦多了就很难统一管理和查看
其次，微服务之间的调用都是基于接口的
如果不这么做，微服务之间的调用就需要相互依赖了，
耦合度也就高了，接口的目的为了能够提供解耦。
此外，本工程的接口其实就是一套规范。实现都是由各自的工程去做的处理。
目前我们使用springboot作为接口的实现的。
如果未来以后出现新的java web框架，那么我们不需要修改接口，
只需要去修改对应的实现就可以了。这其实也是解耦的一个体现。
Swagger2，基于接口的自动文档生成。
所有的配置文件只需要一份，就能在当前项目中去构建了。
管理起来很方便。
综上所述，如此的做法，可以提高多服务的项目可扩展性。

```



## 日志

**logback-spring**

```xml
<?xml version="1.0" encoding="UTF-8"?>

<configuration>
    <!-- 指定日志文件的存储地址，使用绝对路径 -->
    <property name="LOG_HOME" value="/Users/xiaokaixin/Desktop/course/logs/"/>

    <!-- Console 输出设置 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!--格式化输出：%d表示日期，%thread表示线程名，%-5level：级别从左显示5个字符宽度%msg：日志消息，%n是换行符-->
            <pattern>%white(%d{mm:ss.SSS}) %green([%thread]) %cyan(%-5level) %yellow(%logger{36}) %magenta(-) %black(%msg%n)</pattern>
            <charset>utf8</charset>
        </encoder>
    </appender>

    <!-- 按照每天生成日志文件 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 日志文件输出的文件名 -->
            <fileNamePattern>${LOG_HOME}/service-admin.%d{yyyy-MM-dd}.log</fileNamePattern>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!--<logger name="org.apache.ibatis.cache.decorators.LoggingCache" level="DEBUG" additivity="false">-->
        <!--<appender-ref ref="CONSOLE"/>-->
    <!--</logger>-->

    <root level="info">
        <appender-ref ref="FILE"/>
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```



## 多环境

![image-20220605203236821](img/image-20220605203236821.png)



# day3

## 封装统一返回结果

![image-20220607133948344](img/image-20220607133948344.png)

```java
package com.imooc.grace.result;

/**
 *
 * @Title: IMOOCJSONResult.java
 * @Package com.imooc.utils
 * @Description: 自定义响应数据结构
 * 				本类可提供给 H5/ios/安卓/公众号/小程序 使用
 * 				前端接受此类数据（json object)后，可自行根据业务去实现相关功能
 *
 * 				200：表示成功
 * 				500：表示错误，错误信息在msg字段中
 * 				501：bean验证错误，不管多少个错误都以map形式返回
 * 				502：拦截器拦截到用户token出错
 * 				555：异常抛出信息
 * 				556: 用户qq校验异常
 * 			    557: 校验用户是否在CAS登录，用户门票的校验
 */
public class IMOOCJSONResult {

    // 响应业务状态
    private Integer status;

    // 响应消息
    private String msg;

    // 响应中的数据
    private Object data;

    private String ok;	// 不使用

    public static IMOOCJSONResult build(Integer status, String msg, Object data) {
        return new IMOOCJSONResult(status, msg, data);
    }

    public static IMOOCJSONResult build(Integer status, String msg, Object data, String ok) {
        return new IMOOCJSONResult(status, msg, data, ok);
    }

    public static IMOOCJSONResult ok(Object data) {
        return new IMOOCJSONResult(data);
    }

    public static IMOOCJSONResult ok() {
        return new IMOOCJSONResult(null);
    }

    public static IMOOCJSONResult errorMsg(String msg) {
        return new IMOOCJSONResult(500, msg, null);
    }

    public static IMOOCJSONResult errorUserTicket(String msg) {
        return new IMOOCJSONResult(557, msg, null);
    }

    public static IMOOCJSONResult errorMap(Object data) {
        return new IMOOCJSONResult(501, "error", data);
    }

    public static IMOOCJSONResult errorTokenMsg(String msg) {
        return new IMOOCJSONResult(502, msg, null);
    }

    public static IMOOCJSONResult errorException(String msg) {
        return new IMOOCJSONResult(555, msg, null);
    }

    public static IMOOCJSONResult errorUserQQ(String msg) {
        return new IMOOCJSONResult(556, msg, null);
    }

    public IMOOCJSONResult() {

    }

    public IMOOCJSONResult(Integer status, String msg, Object data) {
        this.status = status;
        this.msg = msg;
        this.data = data;
    }

    public IMOOCJSONResult(Integer status, String msg, Object data, String ok) {
        this.status = status;
        this.msg = msg;
        this.data = data;
        this.ok = ok;
    }

    public IMOOCJSONResult(Object data) {
        this.status = 200;
        this.msg = "OK";
        this.data = data;
    }

    public Boolean isOK() {
        return this.status == 200;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getOk() {
        return ok;
    }

    public void setOk(String ok) {
        this.ok = ok;
    }

}

```



## 配置数据库逆向生成工具

**先拷贝项目到主目录下**

![image-20220607134348055](img/image-20220607134348055.png)



# day 4

## 整合mybatis

```properties
############################################################
#
# 用户微服务
# web访问端口号  约定：8003
#
############################################################
server:
#  port: 8003
  tomcat:
    uri-encoding: UTF-8
    max-swallow-size: -1  # tomcat默认大小2M，超过2M的文件不会被捕获，需要调整此处大小为100MB或者-1即可

############################################################
#
# 配置项目信息
#
############################################################
spring:
  profiles:
    active: dev   # yml中配置文件的环境配置，dev：开发环境，test：测试环境，prod：生产环境
  application:
    name: service-user
  datasource:                                         # 数据源的相关配置
    type: com.zaxxer.hikari.HikariDataSource          # 数据源类型：HikariCP
    driver-class-name: com.mysql.cj.jdbc.Driver          # mysql驱动
    url: jdbc:mysql://localhost:3306/imooc-news-dev?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true
    username: root
    password: th123456
    hikari:
      connection-timeout: 30000       # 等待连接池分配连接的最大时长（毫秒），超过这个时长还没可用的连接则发生SQLException， 默认:30秒
      minimum-idle: 5                 # 最小连接数
      maximum-pool-size: 20           # 最大连接数
      auto-commit: true               # 自动提交
      idle-timeout: 600000            # 连接超时的最大时长（毫秒），超时则被释放（retired），默认:10分钟
      pool-name: DateSourceHikariCP     # 连接池名字
      max-lifetime: 1800000           # 连接的生命时长（毫秒），超时而且没被使用则被释放（retired），默认:30分钟 1800000ms
      connection-test-query: SELECT 1
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8


############################################################
#
# mybatis 配置
#
############################################################
mybatis:
  type-aliases-package: com.imooc.pojo          # 所有POJO类所在包路径
  mapper-locations: classpath:mapper/*.xml      # mapper映射文件

############################################################
#
# mybatis mapper 配置
#
############################################################
# 通用 Mapper 配置
mapper:
  mappers: com.imooc.my.mapper.MyMapper
  not-empty: false    # 在进行数据库操作的的时候，判断表达式 username != null, 是否追加 username != ''
  identity: MYSQL
# 分页插件配置
pagehelper:
  helperDialect: mysql
  supportMethodsArguments: true

```





```java
@SpringBootApplication
@MapperScan(basePackages = "com.imooc.user.mapper")
@ComponentScan("com.imooc")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

```



## Swagger

```xml
<!-- swagger2 配置 -->
<dependency>
  <groupId>io.springfox</groupId>
  <artifactId>springfox-swagger2</artifactId>
  <version>${springfox-swagger2.version}</version>
</dependency>
<dependency>
  <groupId>io.springfox</groupId>
  <artifactId>springfox-swagger-ui</artifactId>
  <version>${springfox-swagger2.version}</version>
</dependency>
<dependency>
  <groupId>com.github.xiaoymin</groupId>
  <artifactId>swagger-bootstrap-ui</artifactId>
  <version>${swagger-bootstrap-ui.version}</version>
</dependency>
```





**配置**

```java
        Predicate<RequestHandler> userPredicate = RequestHandlerSelectors.basePackage("com.imooc.user.controller");
```



```java
package com.imooc.api.config;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class Swagger2 {

    //    http://localhost:8088/swagger-ui.html     原路径
    //    http://localhost:8088/doc.html            新路径

    // 配置swagger2核心配置 docket
    @Bean
    public Docket createRestApi() {
        Predicate<RequestHandler> adminPredicate = RequestHandlerSelectors.basePackage("com.imooc.admin.controller");
//        Predicate<RequestHandler> articlePredicate = RequestHandlerSelectors.basePackage("com.imooc.article.controller");
        Predicate<RequestHandler> userPredicate = RequestHandlerSelectors.basePackage("com.imooc.user.controller");
        Predicate<RequestHandler> filesPredicate = RequestHandlerSelectors.basePackage("com.imooc.files.controller");

        return new Docket(DocumentationType.SWAGGER_2)  // 指定api类型为swagger2
                .apiInfo(apiInfo())                 // 用于定义api文档汇总信息
                .select()
                .apis(Predicates.or(userPredicate, adminPredicate, filesPredicate))
//                .apis(Predicates.or(adminPredicate, articlePredicate, userPredicate, filesPredicate))
                .paths(PathSelectors.any())         // 所有controller
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("慕课新闻·自媒体接口api")                       // 文档页标题
                .contact(new Contact("imooc",
                        "https://www.imooc.com",
                        "abc@imooc.com"))                   // 联系人信息
                .description("专为慕课新闻·自媒体平台提供的api文档")      // 详细信息
                .version("1.0.1")                               // 文档版本号
                .termsOfServiceUrl("https://www.imooc.com")     // 网站地址
                .build();
    }
}
```





**注解**

在项目service-api中

![image-20220608184659808](img/image-20220608184659808.png)



# day 5

## 短信登录

![IMG_C6160F942D30-1](img/IMG_C6160F942D30-1.jpeg)

![IMG_D8BC5D8DFBBF-1](img/IMG_D8BC5D8DFBBF-1.jpeg)

> 整合第三方短信服务

```xml
        <!--第三方短信短信-->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.15</version>
        </dependency>
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
            <version>4.2.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpcore</artifactId>
            <version>4.2.1</version>
        </dependency>
        <dependency>
            <groupId>commons-lang</groupId>
            <artifactId>commons-lang</artifactId>
            <version>2.6</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-util</artifactId>
            <version>9.3.7.v20160115</version>
        </dependency>
```





>  **工具类**

```java
package com.imooc.utils.extend;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 发送手机验证码的HttpClient工具类
 * @author Administrator
 *
 */
public class HttpUtils {

	/**
	 * get
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doGet(String host, String path, String method, Map<String, String> headers,
									 Map<String, String> querys) throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpGet request = new HttpGet(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		return httpClient.execute(request);
	}

	/**
	 * post form
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @param bodys
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doPost(String host, String path, String method, Map<String, String> headers,
			Map<String, String> querys, Map<String, String> bodys) throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpPost request = new HttpPost(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		if (bodys != null) {
			List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();

			for (String key : bodys.keySet()) {
				nameValuePairList.add(new BasicNameValuePair(key, bodys.get(key)));
			}
			UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairList, "utf-8");
			formEntity.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
			request.setEntity(formEntity);
		}

		return httpClient.execute(request);
	}

	/**
	 * Post String
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @param body
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doPost(String host, String path, String method, Map<String, String> headers,
			Map<String, String> querys, String body) throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpPost request = new HttpPost(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		if (StringUtils.isNotBlank(body)) {
			request.setEntity(new StringEntity(body, "utf-8"));
		}

		return httpClient.execute(request);
	}

	/**
	 * Post stream
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @param body
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doPost(String host, String path, String method, Map<String, String> headers,
			Map<String, String> querys, byte[] body) throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpPost request = new HttpPost(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		if (body != null) {
			request.setEntity(new ByteArrayEntity(body));
		}

		return httpClient.execute(request);
	}

	/**
	 * Put String
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @param body
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doPut(String host, String path, String method, Map<String, String> headers,
			Map<String, String> querys, String body) throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpPut request = new HttpPut(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		if (StringUtils.isNotBlank(body)) {
			request.setEntity(new StringEntity(body, "utf-8"));
		}

		return httpClient.execute(request);
	}

	/**
	 * Put stream
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @param body
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doPut(String host, String path, String method, Map<String, String> headers,
			Map<String, String> querys, byte[] body) throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpPut request = new HttpPut(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		if (body != null) {
			request.setEntity(new ByteArrayEntity(body));
		}

		return httpClient.execute(request);
	}

	/**
	 * Delete
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doDelete(String host, String path, String method, Map<String, String> headers,
			Map<String, String> querys) throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpDelete request = new HttpDelete(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		return httpClient.execute(request);
	}

	private static String buildUrl(String host, String path, Map<String, String> querys)
			throws UnsupportedEncodingException {
		StringBuilder sbUrl = new StringBuilder();
		sbUrl.append(host);
		if (!StringUtils.isBlank(path)) {
			sbUrl.append(path);
		}
		if (null != querys) {
			StringBuilder sbQuery = new StringBuilder();
			for (Map.Entry<String, String> query : querys.entrySet()) {
				if (0 < sbQuery.length()) {
					sbQuery.append("&");
				}
				if (StringUtils.isBlank(query.getKey()) && !StringUtils.isBlank(query.getValue())) {
					sbQuery.append(query.getValue());
				}
				if (!StringUtils.isBlank(query.getKey())) {
					sbQuery.append(query.getKey());
					if (!StringUtils.isBlank(query.getValue())) {
						sbQuery.append("=");
						sbQuery.append(URLEncoder.encode(query.getValue(), "utf-8"));
					}
				}
			}
			if (0 < sbQuery.length()) {
				sbUrl.append("?").append(sbQuery);
			}
		}

		return sbUrl.toString();
	}

	private static HttpClient wrapClient(String host) {
		HttpClient httpClient = new DefaultHttpClient();
		if (host.startsWith("https://")) {
			sslClient(httpClient);
		}

		return httpClient;
	}

	private static void sslClient(HttpClient httpClient) {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] xcs, String str) {

				}

				public void checkServerTrusted(X509Certificate[] xcs, String str) {

				}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory ssf = new SSLSocketFactory(ctx);
			ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			ClientConnectionManager ccm = httpClient.getConnectionManager();
			SchemeRegistry registry = ccm.getSchemeRegistry();
			registry.register(new Scheme("https", 443, ssf));
		} catch (KeyManagementException ex) {
			throw new RuntimeException(ex);
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
	}
}

```



>  **controller**

```java
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

```



# day 6

```
http://writer.imoocnews.com:9090/imooc-news/writer/passport.html
```



## 安装redis



## 整合redis

```
spring:
  redis:
    database: 0
    host: 124.222.219.104
    port: 6379
    password: asd112211
```



## 完善发送短信接口

```java
    @Override
    public GraceJSONResult getSMSCode(String mobile, HttpServletRequest request) {
        // 获得用户ip
        String userIp = IPUtil.getRequestIp(request);

        // 根据用户的ip进行限制，限制用户在60秒内只能获得一次验证码
        redis.setnx60s(MOBILE_SMSCODE + ":" + userIp, userIp);

        // 生成随机验证码并且发送短信
        String random = (int) ((Math.random() * 9 + 1) * 1000) + "";
        smsUtils.sendSmsCode(mobile, random);

        // 把验证码存入redis，用于后续进行验证
        redis.set(MOBILE_SMSCODE + ":" + mobile, random, 30 * 60);

        return GraceJSONResult.ok();
    }
```





## 跨域

```java
@Configuration
public class CorsConfig {

    public CorsConfig() {
    }

    @Bean
    public CorsFilter corsFilter() {
        // 1. 添加cors配置信息
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        // 设置是否发送cookie信息
        config.setAllowCredentials(true);
        // 设置允许请求的方式
        config.addAllowedMethod("*");
        // 设置允许的header
        config.addAllowedHeader("*");
        // 2. 为url添加映射路径
        UrlBasedCorsConfigurationSource corsSource = new UrlBasedCorsConfigurationSource();
        corsSource.registerCorsConfiguration("/**", config);
        // 3. 返回重新定义好的corsSource
        return new CorsFilter(corsSource);
    }

}
```



# day 7

## 设置拦截器限制60s发送

```java
public class PassportInterceptor implements HandlerInterceptor {

    @Autowired
    public RedisOperator redis;

    public static final String MOBILE_SMSCODE = "mobile:smscode";

    /**
     * 拦截请求，访问controller之前
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 获得用户ip
        String userIp = IPUtil.getRequestIp(request);

        boolean keyIsExist = redis.keyIsExist(MOBILE_SMSCODE + ":" + userIp);

        if (keyIsExist) {
            GraceException.display(ResponseStatusEnum.SMS_NEED_WAIT_ERROR);
//            System.out.println("短信发送频率太大！");
            return false;
        }

        /**
         * false：请求被拦截
         * true：请求通过验证，放行
         */
        return true;
    }
```



## 配置拦截器

```java
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Bean
    public PassportInterceptor passportInterceptor(){
        return new PassportInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(passportInterceptor())
                .addPathPatterns("/passport/getSMSCode");
    }
}

```



## 全局统一异常处理

```java
package com.imooc.exception;

import com.imooc.grace.result.GraceJSONResult;
import com.imooc.grace.result.ResponseStatusEnum;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 统一异常拦截处理
 * 可以针对异常的类型进行捕获，然后返回json信息到前端
 */
@ControllerAdvice
public class GraceExceptionHandler {

    @ExceptionHandler(MyCustomException.class)
    @ResponseBody
    public GraceJSONResult returnMyException(MyCustomException e) {
        e.printStackTrace();
        return GraceJSONResult.exception(e.getResponseStatusEnum());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public GraceJSONResult returnMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return GraceJSONResult.errorCustom(ResponseStatusEnum.FILE_MAX_SIZE_ERROR);
    }

}

```



## 注册登陆

```java
@Override
public GraceJSONResult doLogin(RegistLoginBO registLoginBO,
                               BindingResult result,
                               HttpServletRequest request,
                               HttpServletResponse response) {
    // 0.判断BindingResult中是否保存了错误的验证信息，如果有，则需要返回
    if (result.hasErrors()) {
        Map<String, String> map = getErrors(result);
        return GraceJSONResult.errorMap(map);
    }

    String mobile = registLoginBO.getMobile();
    String smsCode = registLoginBO.getSmsCode();

    // 1. 校验验证码是否匹配
    String redisSMSCode = redis.get(MOBILE_SMSCODE + ":" + mobile);
    if (StringUtils.isBlank(redisSMSCode) || !redisSMSCode.equalsIgnoreCase(smsCode)) {
        return GraceJSONResult.errorCustom(ResponseStatusEnum.SMS_CODE_ERROR);
    }



    // 4. 用户登录或注册成功以后，需要删除redis中的短信验证码，验证码只能使用一次，用过后则作废
    redis.del(MOBILE_SMSCODE + ":" + mobile);

    // 5. 返回用户状态
    return GraceJSONResult.ok();
}
```



# day 8 

## 登陆逻辑（1）

接口

```java
@Service
public class UserServiceImpl implements UserService {


    @Autowired
    public AppUserMapper appUserMapper;

    @Autowired
    public Sid sid;

    @Autowired
    public RedisOperator redis;

    public static final String REDIS_USER_INFO = "redis_user_info";

    // 头像
    private static final String USER_FACE0 = "http://122.152.205.72:88/group1/M00/00/05/CpoxxFw_8_qAIlFXAAAcIhVPdSg994.png";
    private static final String USER_FACE1 = "http://122.152.205.72:88/group1/M00/00/05/CpoxxF6ZUySASMbOAABBAXhjY0Y649.png";
    private static final String USER_FACE2 = "http://122.152.205.72:88/group1/M00/00/05/CpoxxF6ZUx6ANoEMAABTntpyjOo395.png";




    @Override
    public AppUser queryMobileIsExist(String mobile) {
        Example userExample = new Example(AppUser.class);
        Example.Criteria userCriteria = userExample.createCriteria();
        userCriteria.andEqualTo("mobile", mobile);
        AppUser user = appUserMapper.selectOneByExample(userExample);
        return user;
    }

    @Transactional
    @Override
    public AppUser createUser(String mobile) {
        /**
         * 互联网项目都要考虑可扩展性
         * 如果未来的业务激增，那么就需要分库分表
         * 那么数据库表主键id必须保证全局（全库）唯一，不得重复
         */
        String userId = sid.nextShort();

        AppUser user = new AppUser();

        user.setId(userId);
        user.setMobile(mobile);
        user.setNickname("用户：" + DesensitizationUtil.commonDisplay(mobile));
        user.setFace(USER_FACE0);

        user.setBirthday(DateUtil.stringToDate("1900-01-01"));
        user.setSex(Sex.secret.type);
        user.setActiveStatus(UserStatus.INACTIVE.type);

        user.setTotalIncome(0);
        user.setCreatedTime(new Date());
        user.setUpdatedTime(new Date());

        appUserMapper.insert(user);

        return user;
    }
}

```



# day 9

## 登陆逻辑(2)

```java
@RestController
@Slf4j
public class PassportController extends BaseController implements PassportControllerApi {

    @Autowired
    private SMSUtils smsUtils;


    @Autowired
    private UserService userService;

    @Override
    public GraceJSONResult getSMSCode(String mobile, HttpServletRequest request) {
        // 获得用户ip
        String userIp = IPUtil.getRequestIp(request);

        // 根据用户的ip进行限制，限制用户在60秒内只能获得一次验证码
        redis.setnx60s(MOBILE_SMSCODE + ":" + userIp, userIp);

        // 生成随机验证码并且发送短信
        String random = (int) ((Math.random() * 9 + 1) * 1000) + "";
        smsUtils.sendSmsCode(mobile, random);

        // 把验证码存入redis，用于后续进行验证
        redis.set(MOBILE_SMSCODE + ":" + mobile, random, 30 * 60);

        return GraceJSONResult.ok();
    }

    @Override
    public GraceJSONResult doLogin(RegistLoginBO registLoginBO,
                                   BindingResult result,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        // 0.判断BindingResult中是否保存了错误的验证信息，如果有，则需要返回
        if (result.hasErrors()) {
            Map<String, String> map = getErrors(result);
            return GraceJSONResult.errorMap(map);
        }

        String mobile = registLoginBO.getMobile();
        String smsCode = registLoginBO.getSmsCode();

        // 1. 校验验证码是否匹配
        String redisSMSCode = redis.get(MOBILE_SMSCODE + ":" + mobile);
        if (StringUtils.isBlank(redisSMSCode) || !redisSMSCode.equalsIgnoreCase(smsCode)) {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.SMS_CODE_ERROR);
        }

        // 2. 查询数据库，判断该用户注册
        AppUser user = userService.queryMobileIsExist(mobile);
        if (user != null && user.getActiveStatus() == UserStatus.FROZEN.type) {
            // 如果用户不为空，并且状态为冻结，则直接抛出异常，禁止登录
            return GraceJSONResult.errorCustom(ResponseStatusEnum.USER_FROZEN);
        } else if (user == null) {
            // 如果用户没有注册过，则为null，需要注册信息入库
            user = userService.createUser(mobile);
        }



        // 4. 用户登录或注册成功以后，需要删除redis中的短信验证码，验证码只能使用一次，用过后则作废
        redis.del(MOBILE_SMSCODE + ":" + mobile);

        // 5. 返回用户状态
        return GraceJSONResult.ok();
    }



}
```



## 设置会话与cookie信息



```java
    public void setCookie(HttpServletRequest request,
                          HttpServletResponse response,
                          String cookieName,
                          String cookieValue,
                          Integer maxAge) {
        try {
            cookieValue = URLEncoder.encode(cookieValue, "utf-8");
            setCookieValue(request, response, cookieName, cookieValue, maxAge);
//            Cookie cookie = new Cookie(cookieName, cookieValue);
//            cookie.setMaxAge(maxAge);
//            cookie.setDomain("imoocnews.com");
//            cookie.setPath("/");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void setCookieValue(HttpServletRequest request,
                               HttpServletResponse response,
                               String cookieName,
                               String cookieValue,
                               Integer maxAge) {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setMaxAge(maxAge);
       cookie.setDomain("imoocnews.com");
//         cookie.setDomain(DOMAIN_NAME);
        cookie.setPath("/");
        response.addCookie(cookie);
    }



    public void deleteCookie(HttpServletRequest request,
                             HttpServletResponse response,
                             String cookieName) {
        try {
            String deleteValue = URLEncoder.encode("", "utf-8");
            setCookieValue(request, response, cookieName, deleteValue, COOKIE_DELETE);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }
```





```java
    @Override
    public GraceJSONResult getSMSCode(String mobile, HttpServletRequest request) {
        // 获得用户ip
        String userIp = IPUtil.getRequestIp(request);

        // 根据用户的ip进行限制，限制用户在60秒内只能获得一次验证码
        redis.setnx60s(MOBILE_SMSCODE + ":" + userIp, userIp);

        // 生成随机验证码并且发送短信
        String random = (int) ((Math.random() * 9 + 1) * 1000) + "";
        smsUtils.sendSmsCode(mobile, random);

        // 把验证码存入redis，用于后续进行验证
        redis.set(MOBILE_SMSCODE + ":" + mobile, random, 30 * 60);

        return GraceJSONResult.ok();
    }

    @Override
    public GraceJSONResult doLogin(RegistLoginBO registLoginBO,
                                   BindingResult result,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        // 0.判断BindingResult中是否保存了错误的验证信息，如果有，则需要返回
        if (result.hasErrors()) {
            Map<String, String> map = getErrors(result);
            return GraceJSONResult.errorMap(map);
        }

        String mobile = registLoginBO.getMobile();
        String smsCode = registLoginBO.getSmsCode();

        // 1. 校验验证码是否匹配
        String redisSMSCode = redis.get(MOBILE_SMSCODE + ":" + mobile);
        if (StringUtils.isBlank(redisSMSCode) || !redisSMSCode.equalsIgnoreCase(smsCode)) {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.SMS_CODE_ERROR);
        }

        // 2. 查询数据库，判断该用户注册
        AppUser user = userService.queryMobileIsExist(mobile);
        if (user != null && user.getActiveStatus() == UserStatus.FROZEN.type) {
            // 如果用户不为空，并且状态为冻结，则直接抛出异常，禁止登录
            return GraceJSONResult.errorCustom(ResponseStatusEnum.USER_FROZEN);
        } else if (user == null) {
            // 如果用户没有注册过，则为null，需要注册信息入库
            user = userService.createUser(mobile);
        }


        // 3. 保存用户分布式会话的相关操作
        int userActiveStatus = user.getActiveStatus();
        if (userActiveStatus != UserStatus.FROZEN.type) {
            // 保存token到redis
            String uToken = UUID.randomUUID().toString();
            redis.set(REDIS_USER_TOKEN + ":" + user.getId(), uToken);
            redis.set(REDIS_USER_INFO + ":" + user.getId(), JsonUtils.objectToJson(user));

            // 保存用户id和token到cookie中
            setCookie(request, response, "utoken", uToken, COOKIE_MONTH);
            setCookie(request, response, "uid", user.getId(), COOKIE_MONTH);
        }

        // 4. 用户登录或注册成功以后，需要删除redis中的短信验证码，验证码只能使用一次，用过后则作废
        redis.del(MOBILE_SMSCODE + ":" + mobile);

        // 5. 返回用户状态
        return GraceJSONResult.ok(userActiveStatus);
    }


```



<img src="img/image-20220614205702335.png" alt="image-20220614205702335" style="zoom:50%;" />



# day 10

key:mobile:smscode:15281823643

## 查询账户信息

```java

    @Override
    public GraceJSONResult getAccountInfo(String userId) {

        // 0. 判断参数不能为空
        if (StringUtils.isBlank(userId)) {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.UN_LOGIN);
        }

        // 1. 根据userId查询用户的信息
        AppUser user = getUser(userId);

        // 2. 返回用户信息
        UserAccountInfoVO accountInfoVO = new UserAccountInfoVO();
        BeanUtils.copyProperties(user, accountInfoVO);

        return GraceJSONResult.ok(accountInfoVO);
    }
```



## 信息校验：用户资料完善

```java
@Override
public GraceJSONResult updateUserInfo(
        @Valid UpdateUserInfoBO updateUserInfoBO,
        BindingResult result) {

    // 0. 校验BO
    if (result.hasErrors()) {
        Map<String, String> map = getErrors(result);
        return GraceJSONResult.errorMap(map);
    }

    // 1. 执行更新操作
    userService.updateUserInfo(updateUserInfoBO);
    return GraceJSONResult.ok();
}

 /**
     * 获取用户对象
     * @param userId
     * @return
     */
    private AppUser getUser(String userId){
        // TODO 本方法后续公用，并且扩展
        return userService.getUser(userId);
    }

```



# day 11

## 用户信息更新

```java
    @Override
    public void updateUserInfo(UpdateUserInfoBO updateUserInfoBO) {
        String userId = updateUserInfoBO.getId();
        // 保证双写一致，先删除redis中的数据，后更新数据库
        redis.del(REDIS_USER_INFO + ":" + userId);

        AppUser userInfo = new AppUser();
        BeanUtils.copyProperties(updateUserInfoBO, userInfo);

        userInfo.setUpdatedTime(new Date());
        userInfo.setActiveStatus(UserStatus.ACTIVE.type);

        int result = appUserMapper.updateByPrimaryKeySelective(userInfo);
        if (result != 1) {
            GraceException.display(ResponseStatusEnum.USER_UPDATE_ERROR);
        }

        // 再次查询用户的最新信息，放入redis中
        AppUser user = getUser(userId);
        redis.set(REDIS_USER_INFO + ":" + userId, JsonUtils.objectToJson(user));

        // 缓存双删策略
        try {
            Thread.sleep(100);
            redis.del(REDIS_USER_INFO + ":" + userId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```



## 获取用户信息

![image-20220616205855901](img/image-20220616205855901.png)



## 浏览器存储介质

- cookie
- SessionStorage：临时保存
- localStorage:永久保存





# day 12

## 缓存用户信息

==更新的时候把redis的信息也进行删除或者更新==

```java
/**
     * 获取用户对象
     *
     * @param userId
     * @return
     */
    private AppUser getUser(String userId) {
        // 查询判断redis中是否包含用户信息，如果包含，则查询后直接返回，就不去查询数据库了
        String userJson = redis.get(REDIS_USER_INFO + ":" + userId);
        AppUser user = null;
        if (StringUtils.isNotBlank(userJson)) {
            user = JsonUtils.jsonToPojo(userJson, AppUser.class);
        } else {
            user = userService.getUser(userId);
            // 由于用户信息不怎么会变动，对于一些千万级别的网站来说，这类信息不会直接去查询数据库
            // 那么完全可以依靠redis，直接把查询后的数据存入到redis中
            redis.set(REDIS_USER_INFO + ":" + userId, JsonUtils.objectToJson(user));
        }
        return user;
    }
```



## 双写数据不一致

https://cloud.tencent.com/developer/news/634004

```java
@Override
public void updateUserInfo(UpdateUserInfoBO updateUserInfoBO) {
    String userId = updateUserInfoBO.getId();
    // 保证双写一致，先删除redis中的数据，后更新数据库
    redis.del(REDIS_USER_INFO + ":" + userId);

    AppUser userInfo = new AppUser();
    BeanUtils.copyProperties(updateUserInfoBO, userInfo);

    userInfo.setUpdatedTime(new Date());
    userInfo.setActiveStatus(UserStatus.ACTIVE.type);

    int result = appUserMapper.updateByPrimaryKeySelective(userInfo);
    if (result != 1) {
        GraceException.display(ResponseStatusEnum.USER_UPDATE_ERROR);
    }

    // 再次查询用户的最新信息，放入redis中
    AppUser user = getUser(userId);
    redis.set(REDIS_USER_INFO + ":" + userId, JsonUtils.objectToJson(user));

    // 缓存双删策略
    try {
        Thread.sleep(100);
        redis.del(REDIS_USER_INFO + ":" + userId);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}
```

### 缓存延时双删

有些小伙伴可能会说，不一定要先操作数据库呀，采用**缓存延时双删**策略就好啦？什么是延时双删呢？

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/dc942a69d367464d9e778faf635f6448~tplv-k3u1fbpfcp-zoom-in-crop-mark:3024:0:0:0.awebp)

1. 先删除缓存
2. 再更新数据库
3. 休眠一会（比如1秒），再次删除缓存。



# day 13

## CAP

- 一致性
- 可用性
- 分区容错性



## 用户会话拦截器(登陆)

```java
@Autowired
public RedisOperator redis;

public static final String REDIS_USER_TOKEN = "redis_user_token";
public static final String REDIS_USER_INFO = "redis_user_info";
public static final String REDIS_ADMIN_TOKEN = "redis_admin_token";

public boolean verifyUserIdToken(String id, String token, String redisKeyPrefix) {
    if (StringUtils.isNotBlank(id) && StringUtils.isNotBlank(token)) {
        String redisToken = redis.get(redisKeyPrefix + ":" + id);
        if (StringUtils.isBlank(redisToken)) {
            GraceException.display(ResponseStatusEnum.UN_LOGIN);
            return false;
        } else {
            if (redisToken.equalsIgnoreCase(token)) {
                GraceException.display(ResponseStatusEnum.TICKET_INVALID);
                return false;
            }
        }
    } else {
        GraceException.display(ResponseStatusEnum.UN_LOGIN);
        return false;
    }

    return true;
}
```



## 用户状态激活拦截器

```java
/**
 * 用户激活状态检查拦截器
 * 发文章，修改文章，删除文章，
 * 发表评论，查看评论等等
 * 这些接口都是需要在用户激活以后，才能进行
 * 否则需要提示用户前往[账号设置]去修改信息
 */
public class UserActiveInterceptor extends BaseInterceptor implements HandlerInterceptor {

    /**
     * 拦截请求，访问controller之前
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String userId = request.getHeader("headerUserId");
        String userJson = redis.get(REDIS_USER_INFO + ":" + userId);
        AppUser user = null;
        if (StringUtils.isNotBlank(userJson)) {
            user = JsonUtils.jsonToPojo(userJson, AppUser.class);
        } else {
            GraceException.display(ResponseStatusEnum.UN_LOGIN);
            return false;
        }

        if (user.getActiveStatus() == null
                || user.getActiveStatus() != UserStatus.ACTIVE.type) {
            GraceException.display(ResponseStatusEnum.USER_INACTIVE_ERROR);
            return false;
        }

        /**
         * false：请求被拦截
         * true：请求通过验证，放行
         */
        return true;
    }

```



>  记得配置拦截器





## AOP记录日志

```java
@Aspect
@Component
public class ServiceLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);


    /**
     * AOP通知：
     * 1. 前置通知
     * 2. 后置通知
     * 3. 环绕通知
     * 4. 异常通知
     * 5. 最终通知
     */

    @Around("execution(* com.imooc.*.service.impl..*.*(..))")
    public Object recordTimeOfService(ProceedingJoinPoint joinPoint)
            throws Throwable {

        logger.info("==== 开始执行 {}.{}====",
                joinPoint.getTarget().getClass(),
                joinPoint.getSignature().getName());

        long start = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long end = System.currentTimeMillis();
        long takeTime = end - start;

        if (takeTime > 3000) {
            logger.error("当前执行耗时：{}", takeTime);
        } else if (takeTime > 2000) {
            logger.warn("当前执行耗时：{}", takeTime);
        } else {
            logger.info("当前执行耗时：{}", takeTime);
        }

        return result;
    }
}
```





```yml

# 在dev环境开启日志
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```





## 退出登陆

```java
    public GraceJSONResult logout(String userId, HttpServletRequest request, HttpServletResponse response) {

        redis.del(REDIS_USER_TOKEN + ":" + userId);

        setCookie(request,response,"utoken","",COOKIE_DELETE);
        setCookie(request,response,"uid","",COOKIE_DELETE);
        return GraceJSONResult.ok();
    }
```



# 文件服务

# day 14

## 文件服务器

- 传统服务器
- FastDFS
- OSS
- GridFS



## FastDFS

Tracker：协调者，调度器

![image.png](https://pic.rmb.bdstatic.com/bjh/fee426d5966440d8fea769524710da01.png)



### 上传过程





### 下载过程



## 安装

![image-20220626215442333](img/image-20220626215442333.png)

![image-20220626213003740](img/image-20220626213003740.png)



# day 15

## nginx

![image-20220627205912184](img/image-20220627205912184.png)





```
./configure \
--prefix=/usr/local/nginx \
--pid-path=/var/run/nginx/nginx.pid \
--lock-path=/var/lock/nginx.lock\
--error-log-path=/var/log/nginx/error.log \
--http-log-path=/var/log/nginx/access.log \
--with-http_gzip_static_module \
--http-client-body-temp-path=/var/temp/nginx/client \
--http-proxy-temp-path=/var/temp/nginx/proxy \
--http-fastcgi-temp-path=/var/temp/nginx/fastcgi \
--http-uwsgi-temp-path=/var/temp/nginx/uwsgi \
--http-scgi-temp-path=/var/temp/nginx/scgi \
--add-module=/home/software/fastdfs-nginx-module-1.22/src
```



## 失败了



# day 16

## 搭建文件项目



# day 17

## fafs文件上传

### 配置信息获取

```java
@Component
@PropertySource("classpath:file-${spring.profiles.active}.properties")
@ConfigurationProperties(prefix = "file")
public class FileResource {
```



## 阿里云





# day 18

## 自动审核

https://help.aliyun.com/document_detail/53424.html#section-1jq-vd3-ogo

```java
package com.imooc.utils.extend;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.green.model.v20180509.ImageSyncScanRequest;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.http.ProtocolType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.imooc.enums.ArticleReviewLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Component
public class AliImageReviewUtils {

//    文档地址：https://help.aliyun.com/document_detail/70292.html?spm=a2c4g.11186623.2.49.6f9c75fdjaW30p#reference-fzy-ztm-v2b

    @Autowired
    private AliyunResource aliyunResource;

    public boolean reviewImage(String imgUrl) throws Exception {
        IClientProfile profile = DefaultProfile
                .getProfile("cn-shanghai", aliyunResource.getAccessKeyID(), aliyunResource.getAccessKeySecret());
        DefaultProfile
                .addEndpoint("cn-shanghai", "cn-shanghai", "Green", "green.cn-shanghai.aliyuncs.com");
        IAcsClient client = new DefaultAcsClient(profile);

        ImageSyncScanRequest imageSyncScanRequest = new ImageSyncScanRequest();
        // 指定api返回格式
        imageSyncScanRequest.setAcceptFormat(FormatType.JSON);
        // 指定请求方法
        imageSyncScanRequest.setMethod(MethodType.POST);
        imageSyncScanRequest.setEncoding("utf-8");
        //支持http和https
        imageSyncScanRequest.setProtocol(ProtocolType.HTTP);


        JSONObject httpBody = new JSONObject();
        /**
         * 设置要检测的场景, 计费是按照该处传递的场景进行
         * 一次请求中可以同时检测多张图片，每张图片可以同时检测多个风险场景，计费按照场景计算
         * 例如：检测2张图片，场景传递porn、terrorism，计费会按照2张图片鉴黄，2张图片暴恐检测计算
         * porn: porn表示色情场景检测
         * logo: 商标
         * 其他详见官方文档
         */
        httpBody.put("scenes", Arrays.asList("terrorism"));

        /**
         * 设置待检测图片， 一张图片一个task
         * 多张图片同时检测时，处理的时间由最后一个处理完的图片决定
         * 通常情况下批量检测的平均rt比单张检测的要长, 一次批量提交的图片数越多，rt被拉长的概率越高
         * 这里以单张图片检测作为示例, 如果是批量图片检测，请自行构建多个task
         */
        JSONObject task = new JSONObject();
        task.put("dataId", UUID.randomUUID().toString());

        //设置图片链接
        task.put("url", imgUrl);
        task.put("time", new Date());
        httpBody.put("tasks", Arrays.asList(task));

        imageSyncScanRequest.setHttpContent(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(httpBody.toJSONString()),
                "UTF-8", FormatType.JSON);

        /**
         * 请设置超时时间, 服务端全链路处理超时时间为10秒，请做相应设置
         * 如果您设置的ReadTimeout小于服务端处理的时间，程序中会获得一个read timeout异常
         */
        imageSyncScanRequest.setConnectTimeout(3000);
        imageSyncScanRequest.setReadTimeout(10000);
        HttpResponse httpResponse = null;
        try {
            httpResponse = client.doAction(imageSyncScanRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //服务端接收到请求，并完成处理返回的结果
        if (httpResponse != null && httpResponse.isSuccess()) {
            JSONObject scrResponse = JSON.parseObject(org.apache.commons.codec.binary.StringUtils.newStringUtf8(httpResponse.getHttpContent()));
            System.out.println(JSON.toJSONString(scrResponse, true));
            int requestCode = scrResponse.getIntValue("code");
            //每一张图片的检测结果
            JSONArray taskResults = scrResponse.getJSONArray("data");
            if (200 == requestCode) {
                for (Object taskResult : taskResults) {
                    //单张图片的处理结果
                    int taskCode = ((JSONObject) taskResult).getIntValue("code");
                    //图片要检测的场景的处理结果, 如果是多个场景，则会有每个场景的结果
                    JSONArray sceneResults = ((JSONObject) taskResult).getJSONArray("results");
                    if (200 == taskCode) {
                        Object sceneResult = sceneResults.get(0);
//                        for (Object sceneResult : sceneResults) {
                        String scene = ((JSONObject) sceneResult).getString("scene");
                        String suggestion = ((JSONObject) sceneResult).getString("suggestion");
                        //根据scene和suggetion做相关处理
                        //do something
                        System.out.println("scene = [" + scene + "]");
                        System.out.println("suggestion = [" + suggestion + "]");

                        return suggestion.equalsIgnoreCase(ArticleReviewLevel.PASS.type) ? true : false;
//                        }
                    } else {
                        //单张图片处理失败, 原因视具体的情况详细分析
                        System.out.println("task process fail. task response:" + JSON.toJSONString(taskResult));
                        return false;
                    }
                }
            } else {
                /**
                 * 表明请求整体处理失败，原因视具体的情况详细分析
                 */
                System.out.println("the whole image scan request failed. response:" + JSON.toJSONString(scrResponse));
                return false;
            }
        }
        return false;
    }

}
```



# admin服务



# day 18-2

## 创建admin服务

test



# day 19

## 用户名和密码登录

```java
    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private RedisOperator redis;

    @Override
    public GraceJSONResult adminLogin(AdminLoginBO adminLoginBO,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        // 0. TODO 验证BO中的用户名和密码不为空

        // 1. 查询admin用户的信息
        AdminUser admin = adminUserService.queryAdminByUsername(adminLoginBO.getUsername());
        // 2. 判断admin不为空，如果为空则登录失败
        if (admin == null) {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.ADMIN_NOT_EXIT_ERROR);
        }

        // 3. 判断密码是否匹配
        boolean isPwdMatch = BCrypt.checkpw(adminLoginBO.getPassword(), admin.getPassword());
        if (isPwdMatch) {
            doLoginSettings(admin, request, response);
            return GraceJSONResult.ok();
        } else {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.ADMIN_NOT_EXIT_ERROR);
        }
    }


    /**
     * 用于admin用户登录过后的基本信息设置
     * @param admin
     * @param request
     * @param response
     */
    private void doLoginSettings(AdminUser admin,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        // 保存token放入到redis中
        String token = UUID.randomUUID().toString();
        redis.set(REDIS_ADMIN_TOKEN + ":" + admin.getId(), token);

        // 保存admin登录基本token信息到cookie中
        setCookie(request, response, "atoken", token, COOKIE_MONTH);
        setCookie(request, response, "aid", admin.getId(), COOKIE_MONTH);
        setCookie(request, response, "aname", admin.getAdminName(), COOKIE_MONTH);
    }
```





# day 20

## 判断用户的唯一性



todo:admin拦截器

## 新增管理员



# day 21

## 查看admin列表

## 封装分页admin

## 注销登录

## 前端会穿数据，拦截器判断

```
'adminUserId': aid,
'adminUserToken': atoken
```



# day 22 -人脸登陆

chrome://flags/#unsafely-treat-insecure-origin-as-secure

http://admin.imoocnews.com:9090,http://admin.imoocnews.com

![IMG_0215](img/IMG_0215.PNG)

![IMG_0214](img/IMG_0214.PNG)



# day 22

## MongoDB

## Springboot整合MongDB



```yaml
spring:
  data:
    mongodb:
      uri: mongodb://admin:123456@124.222.219.104:27017
      database: imooc-news
```



```java
@Component
public class GridFSConfig {

    @Value("${spring.data.mongodb.database}")
    private String mongodb;

    @Bean
    public GridFSBucket gridFSBucket(MongoClient mongoClient) {
        MongoDatabase mongoDatabase = mongoClient.getDatabase(mongodb);
        GridFSBucket bucket = GridFSBuckets.create(mongoDatabase);
        return bucket;
    }

}
```



```java
   @Override
    public GraceJSONResult uploadToGridFS(NewAdminBO newAdminBO)
            throws Exception {

        // 获得图片的base64字符串
        String file64 = newAdminBO.getImg64();

        // 将base64字符串转换为byte数组
        byte[] bytes = new BASE64Decoder().decodeBuffer(file64.trim());

        // 转换为输入流
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        // 上传到gridfs中
        ObjectId fileId = gridFSBucket.uploadFromStream(newAdminBO.getUsername() + ".png", inputStream);

        // 获得文件在gridfs中的主键id
        String fileIdStr = fileId.toString();

        return GraceJSONResult.ok(fileIdStr);
    }
```





# day 23

## 查看人脸信息



## 阿里云人脸识别，开通不了

## admin管理员的人脸登录

- restTemplat远程调用



# day 23-2

## 友情链接

## MongoDB使用场景

- GridFS小文件存储
- 历史数据快照
- 用户浏览记录
- 客服聊天记录

## JSR校验

- 自定义校验

## MongoDB

持久层操作

实体类 s



## 前端

可以引入：element-ui

```
<!-- 引入样式 -->
<link rel="stylesheet" href="https://unpkg.com/element-ui/lib/theme-chalk/index.css">
<!-- 引入组件库 -->
<script src="https://unpkg.com/element-ui/lib/index.js"></script>
```



# day 24

## 文章分类



## JSON

```java
package com.imooc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class JsonUtils {

    // 定义jackson对象
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 将对象转换成json字符串。
     * @param data
     * @return
     */
    public static String objectToJson(Object data) {
    	try {
			String string = MAPPER.writeValueAsString(data);
			return string;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
    	return null;
    }
    
    /**
     * 将json结果集转化为对象
     * 
     * @param jsonData json数据
     * @param beanType 对象中的object类型
     * @return
     */
    public static <T> T jsonToPojo(String jsonData, Class<T> beanType) {
        try {
            T t = MAPPER.readValue(jsonData, beanType);
            return t;
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 将json数据转换成pojo对象list
     * @param jsonData
     * @param beanType
     * @return
     */
    public static <T>List<T> jsonToList(String jsonData, Class<T> beanType) {
    	JavaType javaType = MAPPER.getTypeFactory().constructParametricType(List.class, beanType);
    	try {
    		List<T> list = MAPPER.readValue(jsonData, javaType);
    		return list;
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return null;
    }
    
}

```



## 日期自动转换

https://blog.csdn.net/qq_40542534/article/details/110632544

```java
package com.imooc.api.config;


import com.imooc.exception.GraceException;
import com.imooc.grace.result.ResponseStatusEnum;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 请求路径url中的参数进行时间日期类型的转换，字符串->日期Date
 */
@Configuration
public class DateConverterConfig implements Converter<String, Date> {

    private static final List<String> formatterList = new ArrayList<>(4);
    static{
        formatterList.add("yyyy-MM");
        formatterList.add("yyyy-MM-dd");
        formatterList.add("yyyy-MM-dd hh:mm");
        formatterList.add("yyyy-MM-dd hh:mm:ss");
    }

    @Override
    public Date convert(String source) {
        String value = source.trim();
        if ("".equals(value)) {
            return null;
        }
        if(source.matches("^\\d{4}-\\d{1,2}$")){
            return parseDate(source, formatterList.get(0));
        }else if(source.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")){
            return parseDate(source, formatterList.get(1));
        }else if(source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}$")){
            return parseDate(source, formatterList.get(2));
        }else if(source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}$")){
            return parseDate(source, formatterList.get(3));
        }else {
            GraceException.display(ResponseStatusEnum.SYSTEM_DATE_PARSER_ERROR);
        }
        return null;
    }

    /**
     * 日期转换方法
     * @param dateStr
     * @param formatter
     * @return
     */
    public Date parseDate(String dateStr, String formatter) {
        Date date=null;
        try {
            DateFormat dateFormat = new SimpleDateFormat(formatter);
            date = dateFormat.parse(dateStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }
}

```



## 查询用户账户_冻结和解封_

![image.png](https://pic.rmb.bdstatic.com/bjh/878d7bf597b95c5b87c4218bfcf3b90d.png)



# day24-2

## 文章表的结构

![image.png](https://pic.rmb.bdstatic.com/bjh/63d01ec3cb68347781245c325dd2b797.png)



## 搭建文章服务



## summernode 与 多文件上传

https://summernote.org/



## 实现多文件上传



## Redis缓存-文章分类

做缓存



## 文章发布



# day 25

## 定时任务-后期优化为MQ

```java
@Configuration      // 1. 标记配置类，使得springboot容器扫描到
@EnableScheduling   // 2. 开启定时任务
public class TaskPublishArticles {

    @Autowired
    private ArticleService articleService;

    // 添加定时任务，注明定时任务的表达式
    @Scheduled(cron = "0/3 * * * * ?")
    private void publishArticles() {
        System.out.println("执行定时任务：" + LocalDateTime.now());

        // 4. 调用文章service，把当前时间应该发布的定时文章，状态改为即时
        articleService.updateAppointToPublish();
    }

}
```



## 后台文章列表

```java
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
```



## 阿里文本检测

```java
package com.imooc.utils.extend;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.green.model.v20180509.TextScanRequest;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.*;

@Component
public class AliTextReviewUtils {

    @Autowired
    private AliyunResource aliyunResource;

    public String reviewTextContent(String content) {
        IClientProfile profile = DefaultProfile.getProfile("cn-shanghai",
                aliyunResource.getAccessKeyID(),
                aliyunResource.getAccessKeySecret());
        IAcsClient client = new DefaultAcsClient(profile);
        TextScanRequest textScanRequest = new TextScanRequest();
        textScanRequest.setAcceptFormat(FormatType.JSON); // 指定api返回格式
        textScanRequest.setHttpContentType(FormatType.JSON);
        textScanRequest.setMethod(com.aliyuncs.http.MethodType.POST); // 指定请求方法
        textScanRequest.setEncoding("UTF-8");
        textScanRequest.setRegionId("cn-shanghai");
        List<Map<String, Object>> tasks = new ArrayList<Map<String, Object>>();
        Map<String, Object> task1 = new LinkedHashMap<String, Object>();
        task1.put("dataId", UUID.randomUUID().toString());
        /**
         * 待检测的文本，长度不超过10000个字符
         */
//        抵制毒品交易
//          尼玛
        task1.put("content", content);
        tasks.add(task1);
        JSONObject data = new JSONObject();

        /**
         * 检测场景，文本垃圾检测传递：antispam
         **/
        data.put("scenes", Arrays.asList("antispam"));
        data.put("tasks", tasks);
        System.out.println(JSON.toJSONString(data, true));

        try {
            textScanRequest.setHttpContent(data.toJSONString().getBytes("UTF-8"), "UTF-8", FormatType.JSON);
            // 请务必设置超时时间
            textScanRequest.setConnectTimeout(3000);
            textScanRequest.setReadTimeout(6000);

            HttpResponse httpResponse = client.doAction(textScanRequest);
            if(httpResponse.isSuccess()){
                JSONObject scrResponse = JSON.parseObject(new String(httpResponse.getHttpContent(), "UTF-8"));
                System.out.println(JSON.toJSONString(scrResponse, true));
                if (200 == scrResponse.getInteger("code")) {
                    JSONArray taskResults = scrResponse.getJSONArray("data");
                    for (Object taskResult : taskResults) {
                        if(200 == ((JSONObject)taskResult).getInteger("code")){
                            JSONArray sceneResults = ((JSONObject)taskResult).getJSONArray("results");
                            JSONObject sceneResult = (JSONObject)sceneResults.get(0);
        //                            for (Object sceneResult : sceneResults) {
                                String scene = sceneResult.getString("scene");
                                String suggestion = sceneResult.getString("suggestion");
                                //根据scene和suggetion做相关处理
                                //suggestion == pass 未命中垃圾  suggestion == block 命中了垃圾，可以通过label字段查看命中的垃圾分类
                                System.out.println("args = [" + scene + "]");
                                System.out.println("args = [" + suggestion + "]");

        //                            suggestion=pass：文本正常，文章状态改为发布通过
        //                            review：需要人工审核，需要在后台管理系统中进行人工审核（很多自媒体平台都会采用机审+人工审的方式）
        //                            block：文本违规，可以直接删除或者做限制处理，审核不通过
        //                            }
                                return suggestion;
                        }else{
                            System.out.println("task process fail:" + ((JSONObject)taskResult).getInteger("code"));
                            return null;
                        }
                    }
                } else {
                    System.out.println("detect not success. code:" + scrResponse.getInteger("code"));
                    return null;
                }
            }else{
                System.out.println("response not success. status:" + httpResponse.getStatus());
                return null;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return null;
    }

}
```



## 检测代码

```java
    /**
     * 发布文章
     * @param newArticleBO
     * @param category
     */
    @Transactional
    @Override
    public void createArticle(NewArticleBO newArticleBO, Category category) {

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

        /**
         * FIXME: 我们只检测正常的词汇，非正常词汇大家课后去检测
         */
        // 通过阿里智能AI实现对文章文本的自动检测（自动审核）
       String reviewTextResult = aliTextReviewUtils.reviewTextContent(newArticleBO.getContent());
        // String reviewTextResult = ArticleReviewLevel.REVIEW.type;

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
```



## 管理员给文章审核和查询列表

```java
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
```



## 删除和撤回文章

```java
    @Transactional
    @Override
    public void deleteArticle(String userId, String articleId) {
        Example articleExample = makeExampleCriteria(userId, articleId);

        Article pending = new Article();
        pending.setIsDelete(YesOrNo.YES.type);

        int result = articleMapper.updateByExampleSelective(pending, articleExample);
        if (result != 1) {
            GraceException.display(ResponseStatusEnum.ARTICLE_DELETE_ERROR);
        }
    }

    @Transactional
    @Override
    public void withdrawArticle(String userId, String articleId) {
        Example articleExample = makeExampleCriteria(userId, articleId);

        Article pending = new Article();
        pending.setArticleStatus(ArticleReviewStatus.WITHDRAW.type);

        int result = articleMapper.updateByExampleSelective(pending, articleExample);
        if (result != 1) {
            GraceException.display(ResponseStatusEnum.ARTICLE_WITHDRAW_ERROR);
        }
    }

    private Example makeExampleCriteria(String userId, String articleId) {
        Example articleExample = new Example(Article.class);
        Example.Criteria criteria = articleExample.createCriteria();
        criteria.andEqualTo("publishUserId", userId);
        criteria.andEqualTo("id", articleId);
        return articleExample;
    }
```



# day 25-2

- 开发首页与作家个人展示页面
- 文章列表、友情链接查询
- 粉丝关注和取消关注



## 首页友情链接

## 首页文章列表



# day 26

## 重组文章列表

远程调用

## 查询热文 首页



## 作者主页和近期文章

xxx



## 查询用户关注状态

> 查询当前用户是否关注作家



> 用户关注作家，成为粉丝

```java
    /**
     * 用户关注作家，成为粉丝
     * @param writerId
     * @param fanId
     */
    @Transactional
    @Override
    public void follow(String writerId, String fanId) {
        // 获得粉丝用户的信息
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

        // redis 作家粉丝数累加
        redis.increment(REDIS_WRITER_FANS_COUNTS + ":" + writerId, 1);
        // redis 当前用户的（我的）关注数累加
        redis.increment(REDIS_MY_FOLLOW_COUNTS + ":" + fanId, 1);
    }
```



> ```
> 取消关注，作家损失粉丝
> ```



> 粉丝数与关注数页面显示

```java
@Override
public GraceJSONResult getUserInfo(String userId) {
    // 0. 判断参数不能为空
    if (StringUtils.isBlank(userId)) {
        return GraceJSONResult.errorCustom(ResponseStatusEnum.UN_LOGIN);
    }

    // 1. 根据userId查询用户的信息
    AppUser user = getUser(userId);

    // 2. 返回用户信息
    AppUserVO userVO = new AppUserVO();
    BeanUtils.copyProperties(user, userVO);

    // 3. 查询redis中用户的关注数和粉丝数，放入userVO到前端渲染
    userVO.setMyFansCounts(getCountsFromRedis(REDIS_WRITER_FANS_COUNTS + ":" + userId));
    userVO.setMyFollowCounts(getCountsFromRedis(REDIS_MY_FOLLOW_COUNTS + ":" + userId));

    return GraceJSONResult.ok(userVO);
}
```



> 查询我的所有粉丝列表



```java
@ApiOperation(value = "查询当前用户是否关注作家", notes = "查询当前用户是否关注作家", httpMethod = "POST")
@PostMapping("/isMeFollowThisWriter")
public GraceJSONResult isMeFollowThisWriter(@RequestParam String writerId,
                                            @RequestParam String fanId);

@ApiOperation(value = "用户关注作家，成为粉丝", notes = "用户关注作家，成为粉丝", httpMethod = "POST")
@PostMapping("/follow")
public GraceJSONResult follow(@RequestParam String writerId,
                              @RequestParam String fanId);

@ApiOperation(value = "取消关注，作家损失粉丝", notes = "取消关注，作家损失粉丝", httpMethod = "POST")
@PostMapping("/unfollow")
public GraceJSONResult unfollow(@RequestParam String writerId,
                                @RequestParam String fanId);

@ApiOperation(value = "查询我的所有粉丝列表", notes = "查询我的所有粉丝列表", httpMethod = "POST")
@PostMapping("/queryAll")
public GraceJSONResult queryAll(
        @RequestParam String writerId,
        @ApiParam(name = "page", value = "查询下一页的第几页", required = false)
        @RequestParam Integer page,
        @ApiParam(name = "pageSize", value = "分页查询每一页显示的条数", required = false)
        @RequestParam Integer pageSize);

@ApiOperation(value = "查询男女粉丝数量", notes = "查询男女粉丝数量", httpMethod = "POST")
@PostMapping("/queryRatio")
public GraceJSONResult queryRatio(@RequestParam String writerId);

@ApiOperation(value = "根据地域查询粉丝数量", notes = "根据地域查询粉丝数量", httpMethod = "POST")
@PostMapping("/queryRatioByRegion")
public GraceJSONResult queryRatioByRegion(@RequestParam String writerId);
```



> echarts 





# day 26-2

## 开发文章详情接口

- 文章详情查询：封装VO，阅读量也要+1

## 文章评论模块

## 评论管理



# day 27



## 阅读量自增

```java
@Override
public GraceJSONResult readArticle(String articleId, HttpServletRequest request) {

    String userIp = IPUtil.getRequestIp(request);
    // 设置针对当前用户ip的永久存在的key，存入到redis，表示该ip的用户已经阅读过了，无法累加阅读量
    redis.setnx(REDIS_ALREADY_READ + ":" +  articleId + ":" + userIp, userIp);

    redis.increment(REDIS_ARTICLE_READ_COUNTS + ":" + articleId, 1);
    return GraceJSONResult.ok();
}
```

## 阅读防刷方案

### ip 唯一

### 拦截器判断

唯一标识



## meget 批量插叙阅读量

首页需要查询出每个文章的阅读量

![IMG_0218.jpg](https://pic.rmb.bdstatic.com/bjh/4887abd370afad4805553e9cf3d4c735.jpeg)



## 文章评论

- 发表评论
- 所有评论
- 回复评论





### 发表:评论数++，redis

```java
@RestController
public class CommentController extends BaseController implements CommentControllerApi {

    final static Logger logger = LoggerFactory.getLogger(CommentController.class);

    @Autowired
    private CommentPortalService commentPortalService;

    @Override
    public GraceJSONResult createArticle(@Valid CommentReplyBO commentReplyBO,
                                         BindingResult result) {

        // 0. 判断BindingResult是否保存错误的验证信息，如果有，则直接return
        if (result.hasErrors()) {
            Map<String, String> errorMap = getErrors(result);
            return GraceJSONResult.errorMap(errorMap);
        }

        // 1. 根据留言用户的id查询他的昵称，用于存入到数据表进行字段的冗余处理，从而避免多表关联查询的性能影响
        String userId = commentReplyBO.getCommentUserId();

        // 2. 发起restTemplate调用用户服务，获得用户侧昵称
        Set<String> idSet = new HashSet<>();
        idSet.add(userId);
        String nickname = getBasicUserList(idSet).get(0).getNickname();

        // 3. 保存用户评论的信息到数据库
        commentPortalService.createComment(commentReplyBO.getArticleId(),
                commentReplyBO.getFatherId(),
                commentReplyBO.getContent(),
                userId,
                nickname);

        return GraceJSONResult.ok();
    }

}

```



## 文章评论sql关联查询

```sql
SELECT
	c.id AS COMMENTId,
	c.father_id AS fatherId,
	c.comment_user_id AS commentUserId,
	c.comment_user_nickname AS commentUserNickname,
	c.article_id AS articleId,
	c. `content` AS `content`,
	c.create_time AS createTime,
	f.comment_user_nickname as quoteUserNickname,
	f.`content` as quoteContent
FROM
	`comments` c
LEFT JOIN 
	`comments` f 	
on c.father_id = f.id	
where c.article_id = '220712A463GZKPM8'
ORDER by c.create_time
DESC
```



## &查询我的评论管理列表&作者删除评论

```
查询我的评论管理列表
```





## day 27 基础班小结

![IMG_0222](img/IMG_0222.jpg)

![IMG_0221](img/IMG_0221.jpg)

![IMG_0220](img/IMG_0220.PNG)

![IMG_0219](img/IMG_0219.jpg)



# day 28

## 自动生成Html

```java
/**
 * 管理员对文章进行审核通过或者失败
 * @param articleId
 * @param passOrNot
 * @return
 */
@Override
public GraceJSONResult doReview(String articleId, Integer passOrNot) {
    Integer pendingStatus;
    if (passOrNot == YesOrNo.YES.type) {
        // 审核成功
        pendingStatus = ArticleReviewStatus.SUCCESS.type;
    } else if (passOrNot == YesOrNo.NO.type) {
        // 审核失败
        pendingStatus = ArticleReviewStatus.FAILED.type;
    } else {
        return GraceJSONResult.errorCustom(ResponseStatusEnum.ARTICLE_REVIEW_ERROR);
    }

    // 保存到数据库，更改文章的状态为审核成功或者失败
    articleService.updateArticleStatus(articleId, pendingStatus);
    if(pendingStatus==ArticleReviewStatus.SUCCESS.type){
        // 审核成功，生成文章详情页静态html
        try {
            createArticleHTML(articleId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    return GraceJSONResult.ok();
}




// 文章生成HTML
public void createArticleHTML(String articleId) throws Exception {

    Configuration cfg = new Configuration(Configuration.getVersion());
    String classpath = this.getClass().getResource("/").getPath();
    cfg.setDirectoryForTemplateLoading(new File(classpath + "templates"));

    Template template = cfg.getTemplate("detail.ftl", "utf-8");

    // 获得文章的详情数据
    ArticleDetailVO detailVO = getArticleDetail(articleId);
    Map<String, Object> map = new HashMap<>();
    map.put("articleDetail", detailVO);

    File tempDic = new File(articlePath);
    if (!tempDic.exists()) {
        tempDic.mkdirs();
    }

    String path = articlePath + File.separator + detailVO.getId() + ".html";

    Writer out = new FileWriter(path);
    template.process(map, out);
    out.close();
}


// 发起远程调用rest，获得文章详情数据
public ArticleDetailVO getArticleDetail(String articleId) {
    String url
            = "http://www.imoocnews.com:8001/portal/article/detail?articleId=" + articleId;
    ResponseEntity<GraceJSONResult> responseEntity
            = restTemplate.getForEntity(url, GraceJSONResult.class);
    GraceJSONResult bodyResult = responseEntity.getBody();
    ArticleDetailVO detailVO = null;
    if (bodyResult.getStatus() == 200) {
        String detailJson = JsonUtils.objectToJson(bodyResult.getData());
        detailVO = JsonUtils.jsonToPojo(detailJson, ArticleDetailVO.class);
    }
    return detailVO;
}
```



## 修改bug

模版也需要修改

![image-20220715102820916](img/image-20220715102820916.png)



## 静态页面增加阅读量接口





## 解耦静态化



生产端存储html到gridfs并关联文章表

消费端从gridfs下载到tomcat

![IMG_0225](img/IMG_0225.PNG)

![IMG_0224](img/IMG_0224.PNG)





# day29 MQ

解耦合度



很重要，挺有意思的

<img src="img/image-20220715203335918.png" alt="image-20220715203335918" style="zoom:50%;" />

![image-20220715203632457](img/image-20220715203632457.png)



## 文章演时发布

延时队列key写错，消息丢失

```java
        // 发送延迟消息到mq，计算定时发布时间和当前时间的时间差，则为往后延迟的时间
        if (article.getIsAppoint() == ArticleAppointType.TIMING.type) {

            Date endDate = newArticleBO.getPublishTime();
            Date startDate = new Date();

//            int delayTimes = (int)(endDate.getTime() - startDate.getTime());

            System.out.println(DateUtil.timeBetween(startDate, endDate));

            // FIXME: 为了测试方便，写死10s
            int delayTimes = 10 * 1000;

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

```



# day 30

## 使用RestTemplate的弊端



## Eureka-注册中心

![image-20220716092659692](img/image-20220716092659692.png)

![image-20220716092711839](img/image-20220716092711839.png)

![image-20220716095042634](img/image-20220716095042634.png)



## 负载均衡

![image-20220716100628562](img/image-20220716100628562.png)

### 自定义



## 重试机制

```
 <dependency>
            <groupId>org.springframework.retry</groupId>
            <artifactId>spring-retry</artifactId>
        </dependency>
```



```yml
ribbon:
  ConnectTimeout: 5000          # 创建连接的超时时间，单位：ms
  ReadTimeout: 5000             # 在连接创建好以后，调用接口的超时时间，单位：ms
  MaxAutoRetries: 1             # 最大重试次数
  MaxAutoRetriesNextServer: 2   # 切换到下个微服务实例的重试次数
  # 当请求到某个微服务5s，超时后会进行重试，先重试连接自己当前的这个实例
  # 如果当前重试失败1次，则会切换到访问集群中的下一个微服务实例，切换最大为2次
```



## eureka自我保护

![image-20220716102549016](img/image-20220716102549016.png)

![image-20220716102529421](img/image-20220716102529421.png)





## Feign

### 错误

 Method has too many Body parameters: public abstract co

调用放和提供方如何配置



### 日志



## 统一校验处理



统一异常处理





## hystrix

> 服务提供者

### 局部降级

![image-20220716152329254](img/image-20220716152329254.png)

user 服务

```yml
# 配置hystrix
hystrix:
  command:
    default:
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 2000   # 设置hystrix超时时间，超过2秒触发降级
      circuitBreaker:   # 配置断路器
        enabled: true
        requestVolumeThreshold: 10    # 触发熔断最小请求次数，默认：20
        sleepWindowInMilliseconds: 15000    # 熔断后过几秒后尝试半开状态（请求重试），默认：5s
        errorThresholdPercentage: 50  # 触发熔断的失败率（异常率/阈值），默认：50

```



### 全局降级

![image-20220716152848942](img/image-20220716152848942.png)



### 调用方降级

> 调用方

```yml
# 配置feign
feign:
  client:
    config:
      # 配置服务提供方的名称
      service-user:
        loggerLevel: FULL
  hystrix:
    enabled: true   # 打开feign客户端的内置hystrix
```

![image-20220716154237671](img/image-20220716154237671.png)



## 断路器工作原理

```yml
# 配置hystrix
hystrix:
  command:
    default:
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 2000   # 设置hystrix超时时间，超过2秒触发降级
      circuitBreaker:   # 配置断路器
        enabled: true
        requestVolumeThreshold: 10    # 触发熔断最小请求次数，默认：20
        sleepWindowInMilliseconds: 15000    # 熔断后过几秒后尝试半开状态（请求重试），默认：5s
        errorThresholdPercentage: 50  # 触发熔断的失败率（异常率/阈值），默认：50
```



# 项目报错





## zuul

- 过滤器
- 配置



## 配置中心

```yml
  <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

```

springcloud -config 看文档

### 动态刷新



postman



## SpringCloud bus

看文档



## SpringCloud Stream

看文档

持久化



## Sleuth

链路追踪

http://127.0.0.1:9091/api/service-article/portal/article/detail?articleId=20061171HKPGC3XP



# 修改

![image-20220720144746963](img/image-20220720144746963.png)



# 提升

ES

> 文章创建索引

```java
PUT /articles
{
  "mappings": {
    "properties": {
      "id":{
        "type": "text"
      },
      "title":{
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "categoryId":{
        "type": "integer"
      },
      "articleType":{
        "type": "integer"
      },
      "articleCover":{
        "type": "keyword"
      },
      "publishUserId":{
        "type": "text"
      },
      "publishTime":{
        "type": "date"
      }
    }
    
  }
}
```



## article 服务

```xml
   <!--elasticsearch 7.12.1-->
        <dependency>
            <groupId>org.elasticsearch.client</groupId>
            <artifactId>elasticsearch-rest-high-level-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.elasticsearch.client</groupId>
            <artifactId>elasticsearch-rest-client</artifactId>

        </dependency>
        <dependency>
            <groupId>org.elasticsearch</groupId>
            <artifactId>elasticsearch</artifactId>
        </dependency>

```



```java
   // elastic search
    @Bean
    public RestHighLevelClient restHighLevelClient(){
        return new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://124.222.219.104:9200")
        ));
    }
```



> 发布

```java
/**
     * 更新文章状态
     * @param articleId
     * @param pendingStatus
     */
    @Override
    public void updateArticleStatus(String articleId, Integer pendingStatus) {
        // Example example = new Example(Article.class);
        // Example.Criteria criteria = example.createCriteria();
        // criteria.andEqualTo("id", articleId);
        //
        // Article pendingArticle = new Article();
        // pendingArticle.setArticleStatus(pendingStatus);

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
```



> 查看数据

```json
GET /articles/_search
{
  "query": {
    "match_all": {}
  }
}

{
  "took" : 291,
  "timed_out" : false,
  "_shards" : {
    "total" : 1,
    "successful" : 1,
    "skipped" : 0,
    "failed" : 0
  },
  "hits" : {
    "total" : {
      "value" : 2,
      "relation" : "eq"
    },
    "max_score" : 1.0,
    "hits" : [
      {
        "_index" : "articles",
        "_type" : "_doc",
        "_id" : "220720GBBC4ZGXP0",
        "_score" : 1.0,
        "_source" : {
          "id" : "220720GBBC4ZGXP0",
          "title" : "测试ES3",
          "categoryId" : 2,
          "articleType" : 1,
          "articleCover" : "https://news-xiaokaixin.oss-cn-shanghai.aliyuncs.com/news/220614FN44ZCP46W/220720GBB0PWD9GC.jpeg",
          "publishUserId" : "220614FN44ZCP46W",
          "publishTime" : 1658324118000
        }
      },
      {
        "_index" : "articles",
        "_type" : "_doc",
        "_id" : "220720GCT59RKX68",
        "_score" : 1.0,
        "_source" : {
          "id" : "220720GCT59RKX68",
          "title" : "测试ES4",
          "categoryId" : 6,
          "articleType" : 1,
          "articleCover" : "https://news-xiaokaixin.oss-cn-shanghai.aliyuncs.com/news/220614FN44ZCP46W/220720GCSSCX10BC.jpeg",
          "publishUserId" : "220614FN44ZCP46W",
          "publishTime" : 1658324374000
        }
      }
    ]
  }
}

```



## 撤回和删除

根据id删除即可



## 搜索

```java
// ES 搜索
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
        //  开启高亮
        request.source().highlighter(new HighlightBuilder().field("title").requireFieldMatch(false));
        request.source().query(QueryBuilders.matchQuery("title", keyword));
    }


    // 3.发送请求，得到响应
    SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
    List<ArticleEO> articleEOS = handleResponse(response);
    return GraceJSONResult.ok(articleEOS);
}

// 处理ES数据
private List<ArticleEO> handleResponse(SearchResponse response) {
    List<ArticleEO> data = new ArrayList<>();
    SearchHits searchHits = response.getHits();
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
```



## 粉丝

```json
# 创建粉丝索引
PUT /fans
{
  "mappings": {
    "properties": {
      "id":{
        "type": "text"
      },
      "fanNickname":{
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "sex":{
        "type": "integer"
      },
      "face":{
        "type": "keyword"
      },
      "province":{
        "type": "keyword"
      },
      "fanId":{
        "type": "keyword"
      },
      "writerId":{
        "type": "keyword"
      }
    }
    
  }
}

```



```java
    // todo 从 ES 中查询数据
    @Override
    public PagedGridResult queryMyFansESList(String writerId, Integer page, Integer pageSize) throws IOException {
        // 1、准备request
        if(page<1) return null;
        SearchRequest request = new SearchRequest("fans");
        // 2、分页
        request.source().from((page-1)*pageSize).size(pageSize);
        // 3、term查询
        request.source().query(QueryBuilders.termQuery("writerId",writerId));
        // 4、发送请求
        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        // 5、处理数据
        List<FansEO> fansEOS = handleResponse(response);
        PagedGridResult gridResult =new PagedGridResult();
        gridResult.setRecords(searchHits.getTotalHits().value);
        gridResult.setTotal(searchHits.getTotalHits().value%pageSize==0?searchHits.getTotalHits().value/pageSize:searchHits.getTotalHits().value/pageSize+1);
        gridResult.setRows(fansEOS);
        return gridResult;
    }
```



## 粉丝信息被动更新

![image.png](https://pic.rmb.bdstatic.com/bjh/4e53b0017a432c3344deda80791b4e34.png)

诺一致性

不足，不能监听

```java

    // todo
    @Override
    public FansCountsVO queryFansESCounts(String writerId) throws IOException {
        FansCountsVO fansCountsVO = new FansCountsVO();
        /**
         * # 粉丝男女统计
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
        // 1.准备Request
        SearchRequest request = new SearchRequest("fans");
        // 2、准备请求参数
        request.source().query(QueryBuilders.termQuery("writerId",writerId));
        // 3、agg
        request.source().aggregation(AggregationBuilders
                .terms("sexCounts")
                .field("sex")
                .size(2));

        // 4.发出请求
        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        Aggregations aggregations = response.getAggregations();
        // List<String> sexCounts = getAggByName(aggregations, "sexCounts");
        Terms brandTerms = aggregations.get("sexCounts");
        // 获取buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();

        // 封装数据
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

```

