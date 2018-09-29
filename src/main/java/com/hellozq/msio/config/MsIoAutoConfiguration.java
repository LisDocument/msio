package com.hellozq.msio.config;

import com.hellozq.msio.bean.common.IFormatConversion;
import com.hellozq.msio.bean.common.ITransFunctionContainer;
import com.hellozq.msio.config.derivative.BaseInterceptConstruction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author bin
 * 提供统一的容器供使用
 */
@Configuration
@Slf4j
public class MsIoAutoConfiguration {

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
        return new IFormatConversion() {
        };
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
            log.error("初始化失败");
        }
        msIoContainer.init(abstractMsConfigure);

        return msIoContainer;
    }
}
