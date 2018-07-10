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
     * @return
     */
    @ConditionalOnMissingBean(TransFunctionContainer.class)
    @Bean
    public TransFunctionContainer transFunctionContainer(){
        return new TransFunctionContainer() {};
    }
}
