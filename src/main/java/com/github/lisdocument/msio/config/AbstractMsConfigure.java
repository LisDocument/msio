package com.github.lisdocument.msio.config;

import com.github.lisdocument.msio.config.derivative.BaseInterceptConstruction;

/**
 * @author bin
 * @version 1.0.1
 * <p>全文用户配置类，按照需求配置数据，用户介入唯一入口，通过继承配置类来配置</>
 * <p>在1.0.1版本中提供了新的方法configDataSource</p>
 * <p>模块是应用该方法创建,组件构建介入口</>
 *
 */
@SuppressWarnings("unused")
public abstract class AbstractMsConfigure {

    /**
     * 添加拦截器
     * @param intercept 拦截器实体
     * @see MsIoAutoConfiguration#interceptConstruction(AbstractMsConfigure)  自动注入位置
     * @return 返回
     */
    public BaseInterceptConstruction addInterceptors(BaseInterceptConstruction intercept){
        return intercept;
    }


    /**
     * 添加额外映射
     * @param container 容器实体，会自动注入,注入后可通过container提供的方法进行映射插入
     * @see MsIoContainer#addMappings(Class[])
     * @return 返回
     * @throws Exception 配置错误
     */
    public MsIoContainer configContainer(MsIoContainer container) throws Exception{
        return container;
    }

    /**
     * 配置用来存储的数据源
     * @return 数据源/保存数据的节点
     */
    public <T> T configDataSource(){
        return null;
    }
}
