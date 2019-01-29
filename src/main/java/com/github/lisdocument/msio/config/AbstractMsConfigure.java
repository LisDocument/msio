package com.github.lisdocument.msio.config;

import com.github.lisdocument.msio.config.derivative.BaseInterceptConstruction;

import javax.sql.DataSource;

/**
 * @author bin
 * 全文用户配置类，按照需求配置数据，用户介入唯一入口，通过继承配置类来配置
 * 模块是应用该方法创建
 */
@SuppressWarnings("unused")
public abstract class AbstractMsConfigure {

    /**
     * 添加拦截器
     * @param intercept 拦截器实体
     * @see MsIoAutoConfiguration
     * @return 返回
     */
    public BaseInterceptConstruction addInterceptors(BaseInterceptConstruction intercept){
        return intercept;
    }


    /**
     * 添加额外映射
     * @param container 容器实体，会自动注入属性
     * @return 返回
     * @throws Exception 配置错误
     */
    public MsIoContainer configContainer(MsIoContainer container) throws Exception{
        return container;
    }

    /**
     *
     * @return
     */
    public <T> T configDataSource(){
        return null;
    }
}
