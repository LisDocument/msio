package com.hellozq.msio.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.hellozq.msio.bean.common.IFormatConversion;
import com.hellozq.msio.bean.common.ITransFunctionContainer;
import com.hellozq.msio.bean.common.MsIoServlet;
import com.hellozq.msio.bean.common.impl.DefaultFormatConversion;
import com.hellozq.msio.config.derivative.BaseInterceptConstruction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

import javax.sql.DataSource;

/**
 * @author bin
 * 提供统一的容器供使用,
 * 协同总配置类，框架入口
 */
@Configuration
class MsIoAutoConfiguration {

    @Value("${spring.micro.listen.url:/upload/*}")
    private String listenerUrl;

    private final static Log log = LogFactory.getLog(MsIoAutoConfiguration.class);

    /**
     * 导出容器初始化
     * @return 初始化
     */
    @ConditionalOnMissingBean(ITransFunctionContainer.class)
    @Bean
    public ITransFunctionContainer transFunctionContainer(){
        return new ITransFunctionContainer() {};
    }

    /**
     * 转换容器初始化
     * @return 初始化
     */
    @ConditionalOnMissingBean(IFormatConversion.class)
    @Bean
    public IFormatConversion formatConversion(){
        return new DefaultFormatConversion();
    }

    /**
     * servlet容器初始化
     * @return 自定义servlet
     */
    @ConditionalOnExpression("${spring.msIo.autoServlet:true}")
    @Bean
    public MsIoServlet msIoServlet(){
        return new MsIoServlet();
    }

    /**
     * servlet注册
     * @param msIoServlet servlet实体项
     * @return 注册集
     */
    @ConditionalOnExpression("${spring.msIo.autoServlet:true}")
    @Bean
    @Autowired
    public ServletRegistrationBean<DispatcherServlet> restServlet(MsIoServlet msIoServlet){
        log.info("msServlet->Bind->"+listenerUrl);
        //注册新的servlet用于监听上传文件的接口
        return new ServletRegistrationBean<>(msIoServlet,listenerUrl.split(","));
    }

    /**
     * 构造类初始化（构造类可自定义添加）
     * @return 初始化
     */
    @ConditionalOnMissingBean(AbstractMsConfigure.class)
    @Bean
    public AbstractMsConfigure configure(){
        return new AbstractMsConfigure() {};
    }

    /**
     * 初始化拦截器容器基本信息
     * @param abstractMsConfigure 注册拦截器
     * @return 拦截器容器
     */
    @Bean
    @Autowired
    public BaseInterceptConstruction interceptConstruction(AbstractMsConfigure abstractMsConfigure){
        BaseInterceptConstruction instance = BaseInterceptConstruction.instance();
        instance = abstractMsConfigure.addInterceptors(instance);

        return instance;
    }

    /**
     * 初始化全局容器
     * @param abstractMsConfigure 注册缓存映射
     * @return 全局容器
     */
    @Bean
    @Autowired
    public MsIoContainer msIoContainer(AbstractMsConfigure abstractMsConfigure, ITransFunctionContainer ITransFunctionContainer){
        MsIoContainer msIoContainer = new MsIoContainer(ITransFunctionContainer);
        try {
            msIoContainer = abstractMsConfigure.configContainer(msIoContainer);
        }catch (Exception e){
            e.printStackTrace();
        }
        msIoContainer.init(abstractMsConfigure);

        return msIoContainer;
    }

    @ConditionalOnMissingBean(DataSource.class)
    @ConditionalOnProperty(name = "spring.msIo.dbLog",havingValue = "true")
    @Bean(name = "msIoSource")
    public DataSource dataSource(){
        DruidDataSource dataSource = new DruidDataSource();
        return dataSource;
    }
}
