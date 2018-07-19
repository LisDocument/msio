package com.hellozq.msio.config;

import com.hellozq.msio.config.derivative.InterceptConstruction;

/**
 * @author bin
 * 全文配置类，按照需求配置数据
 */
@SuppressWarnings("unused")
public abstract class AbstractMsConfigure {

    /**
     * 添加拦截器
     * @param intercept 拦截器实体
     * @return 返回
     */
    public InterceptConstruction addInterceptors(InterceptConstruction intercept){
        return intercept;
    }


    /**
     * 添加额外映射
     * @param container 容器实体，会自动注入属性
     * @return 返回
     */
    public MsIoContainer addClassCache(MsIoContainer container){
        return container;
    }

}
