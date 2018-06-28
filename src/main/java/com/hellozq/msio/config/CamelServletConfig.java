package com.hellozq.msio.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * 具体为上传的文件使用的接口的处理方式，定义新的servlet
 */
@Configuration
public class CamelServletConfig {

    private static final Log log = LogFactory.getLog(CamelServletConfig.class);

    @Value("${spring.micro.listen.url:/upload/*}")
    private String listenerUrl;

    @Bean
    public MsIOServlet msIOServlet(){
        return new MsIOServlet();
    }


    @Bean
    public ServletRegistrationBean<DispatcherServlet> restServlet(){
        log.info("-------------执行自定义跳转方式的servlet中---------------");
        //注册新的servlet用于监听上传文件的接口
        return new ServletRegistrationBean<>(msIOServlet(),listenerUrl.split(","));
    }
}
