package com.hellozq.msio.config.derivative;

import com.hellozq.msio.bean.others.FileInterceptItem;

/**
 * @author bin
 * 拦截项，对上传来的文件辨识并根据大小以及各种因素进行拦截
 */
@SuppressWarnings("unused")
public interface InterceptConstruction {

    /**
     * 添加一个路径监听
     * @param path 路径
     * @param fileInterceptItem 拦截选项
     * @return  返回
     */
    InterceptConstruction add(String path, FileInterceptItem fileInterceptItem);

}
