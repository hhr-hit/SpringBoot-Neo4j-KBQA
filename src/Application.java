package com.hhr;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

/**
 * SpringBoot启动类
 * @SpringBootApplication
 * 等价于
 * @Configuration
 * @EnableAutoConfiguration
 * @ComponentScan
 */
@SpringBootApplication
public class Application extends SpringBootServletInitializer {

	/**
	 * SpringBoot的启动类
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);

		//指定jre系统属性，允许 特殊符号 | 做入参   详情见 tomcat  HttpParser类
		//System.setProperty("tomcat.util.http.parser.HttpParser.requestTargetAllow","|");
	}

	/**
	 * 打包成war
	 * 需要一个ServletInitializer类
	 * @param application
	 * @return
	 */
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Application.class);
	}

	/**
	 * 配置修改
	 * 请求路径容纳特殊字符
	 * @return
	 */
	@Bean
	public TomcatServletWebServerFactory webServerFactory() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
		factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
			@Override
			public void customize(Connector connector) {
				connector.setProperty("relaxedPathChars", "\"<>[\\]^`{|}()=");
				connector.setProperty("relaxedQueryChars", "\"<>[\\]^`{|}()=");
			}
		});
		return factory;
	}
}
