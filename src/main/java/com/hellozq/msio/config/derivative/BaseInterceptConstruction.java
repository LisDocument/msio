package com.hellozq.msio.config.derivative;

import com.alibaba.druid.util.StringUtils;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.hellozq.msio.bean.others.FileInterceptItem;
import lombok.NonNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bin
 * 拦截项，对上传来的文件辨识并根据大小以及各种因素进行拦截
 */
@SuppressWarnings("unused")
public abstract class BaseInterceptConstruction {

    /**
     * 只在第一次初始化的情况下执行，因此只需要考虑获取速度，因此不考虑链式结构
     */
    private Multimap<String,FileInterceptItem> filterMapping = ArrayListMultimap.create();

    private final String ALL_PATH = "/**";

    private final String PATH_SEPARATOR = "/";

    /**
     * 添加一个路径监听
     * @param path 路径
     * @param fileInterceptItem 拦截选项
     * @return  返回
     */
    public BaseInterceptConstruction add(String path,@NonNull FileInterceptItem fileInterceptItem){

        if(StringUtils.isEmpty(path)){
            filterMapping.put("/**",fileInterceptItem);
            return this;
        }
        int i;
        while((i = path.lastIndexOf(PATH_SEPARATOR)) != -1){
            path = path.substring(0,i);
            filterMapping.put(path + ALL_PATH,fileInterceptItem);
        }
        return this;
    }

    /**
     * 根据排序序号进行排序返回
     * @param path 监听路径
     * @return 排序后返回的过滤情况
     */
    protected List<FileInterceptItem> getFilterList(@NonNull String path){

        Collection<FileInterceptItem> filters = filterMapping.get(path);
        return filters.stream().sorted(Comparator.comparing(FileInterceptItem::getOrderNo)).collect(Collectors.toList());
    }

    /**
     * 提供默认的构造函数
     * @return 默认对象
     */
    public static BaseInterceptConstruction instance(){
        return new BaseInterceptConstruction() {
        };
    }

}
