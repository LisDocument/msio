package com.hellozq.msio.config;

import com.hellozq.msio.bean.common.TransFunctionContainer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author bin
 * 提供统一的容器供使用
 */
@Configuration
public class MsIoAutoConfiguration {

    /**
     * 导出容器初始化
     * @return 初始化
     */
    @ConditionalOnMissingBean(TransFunctionContainer.class)
    @Bean
    public TransFunctionContainer transFunctionContainer(){
        return new TransFunctionContainer() {};
    }

    /**
     * 自动配置
     * @return 初始化
     */
    @ConditionalOnMissingBean(AbstractMsConfigure.class)
    @Bean
    public AbstractMsConfigure msConfigure(){
        return new AbstractMsConfigure() {
        };
    }
}
