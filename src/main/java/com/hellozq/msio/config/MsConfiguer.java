package com.hellozq.msio.config;

import com.hellozq.msio.config.derivative.InterceptConstruction;

/**
 * @author bin
 * 全文配置类，按照需求配置数据
 */
public abstract class MsConfiguer {

    public InterceptConstruction addInterceptors(InterceptConstruction intercepts){
        return intercepts;
    }
}
