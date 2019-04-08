package com.github.lisdocument.msio.unit.excel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.github.lisdocument.msio.unit.func.OutExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bin
 * excel文件的处理工厂
 */
@SuppressWarnings("unused")
public class ExcelFactory {

    private static final Logger log = LoggerFactory.getLogger(ExcelFactory.class);

    private static ConcurrentHashMap<String,SimpleExcelBeanReverse> beanCache = new ConcurrentHashMap<>();

    /**
     * @author bin
     */
    public enum ExcelDealType{
        /**
         * .xls
         */
        XLS(".xls"),
        /**
         * .xlsx
         */
        XLSX(".xlsx");

        private String value;

        private ExcelDealType(String value){
            this.value = value;
        }

        public String getValue(){
            return value;
        }
    }

    /**
     * 自助excel解析结果
     * @param file 文件流
     * @return 新的简单excel实例
     */
    public static IExcelBean getSingleSimpleInstance(@NotNull MultipartFile file){
        return new SimpleExcelBean(file,true);
    }

    /**
     * 自助excel解析结果
     * @param file 文件流
     * @return 新的简单excel实例
     */
    public static IExcelBean getSingleSimpleInstance(@NotNull File file){
        return new SimpleExcelBean(file,true);
    }

    /**
     * 获得单页的excel解析结果
     * @param id 指定映射的id，如果为空的话自动寻找
     * @param file 文件流
     * @param pageNo 页码,默认为0
     * @return 新的简单excel实例
     */
    public static IExcelBean getSingleSimpleInstance(String id, @NotNull MultipartFile file,Integer pageNo){
        return new SimpleExcelBean(id,file,pageNo == null ? 0 : pageNo);
    }

    /**
     * 获得单页的excel解析结果
     * @param id 指定映射的id，如果为空的话自动寻找
     * @param file 文件流
     * @param pageNo 页码,默认为0
     * @return 新的简单excel实例
     */
    public static IExcelBean getSingleSimpleInstance(String id, @NotNull File file,Integer pageNo){
        return new SimpleExcelBean(id,file,pageNo == null ? 0 : pageNo);
    }

    /**
     * 复数页的Excel解析结果
     * @param ids 单页id集合，会根据id索引每页，保证长度和页数一致，若不确定id，可设置isChangeClass为true，自动查询索引，若为false，且那个位置的元素为null，则跳过该数据
     * @param file 文件流
     * @param isChangeClass 是否自动加载映射
     * @return 新的实例
     */
    public static IExcelBean getMultipleSimpleInstance(List<String> ids,@NotNull MultipartFile file,boolean isChangeClass){
        if(ids != null && !ids.isEmpty()){
            return new SimpleExcelBean(ids,file,isChangeClass);
        }
        return new SimpleExcelBean(file,isChangeClass);
    }

    /**
     * 复数页的Excel解析结果
     * @param ids 单页id集合，会根据id索引每页，保证长度和页数一致，若不确定id，可设置isChangeClass为true，自动查询索引，若为false，且那个位置的元素为null，则跳过该数据
     * @param file 文件流
     * @param isChangeClass 是否自动加载映射
     * @return 新的实例
     */
    public static IExcelBean getMultipleSimpleInstance(List<String> ids,@NotNull File file,boolean isChangeClass){
        if(ids != null && !ids.isEmpty()){
            return new SimpleExcelBean(ids,file,isChangeClass);
        }
        return new SimpleExcelBean(file,isChangeClass);
    }

    /**
     * 获取导出集成类
     * @param data 数据
     * @param asycSign 集散多线程同步处理
     * @param localCache 本地缓存使用SXXSF解析
     * @param localCacheSize 本地缓存数量
     * @param type 解析文件名
     * @param handler 错误处理方法
     * @param pageSize 每页允许的最大数量
     * @param title 内容标题，处于首行，定义为数组为了应对多页的情况，如果仅有1个，将所有页应用该标题，如果仅有一个且为空，无标题
     * @return 封装好的处理
     */
    public static IExcelBeanReverse getSimpleExcelBeanReverseInstance(Map<Integer,List> data, boolean asycSign,
                                                                      boolean localCache, int localCacheSize, ExcelDealType type,
                                                                      int pageSize,String[] title,OutExceptionHandler handler){
        return new SimpleExcelBeanReverse(data, asycSign, localCache
            , localCacheSize, type, pageSize, Maps.newHashMapWithExpectedSize(64), title, handler);
    }

    /**
     * 获取导出集成类
     * @param data 数据
     * @param handler 错误处理方法
     * @param title 内容标题，处于首行，定义为数组为了应对多页的情况，如果仅有1个，将所有页应用该标题，如果仅有一个且为空，无标题
     * @return 封装好的处理类
     */
    public static IExcelBeanReverse getSimpleExcelBeanReverseInstance(Map<Integer,List> data,String[] title, boolean localCache, OutExceptionHandler handler){
        return new SimpleExcelBeanReverse(data, title, localCache, handler);
    }

    /**
     * 获取导出集成类
     * @param data 数据
     * @param handler 错误处理方法
     * @param title 内容标题，处于首行，定义为数组为了应对多页的情况，如果仅有1个，将所有页应用该标题，如果仅有一个且为空，无标题
     * @
     * @return 封装好的处理类
     */
    public static IExcelBeanReverse getSimpleExcelBeanReverseInstance(Map<Integer,List> data,String[] title, OutExceptionHandler handler){
        return new SimpleExcelBeanReverse(data, title, handler);
    }

    /**
     * 获取导出集成类
     * @param data 数据(单页)
     * @param handler 错误处理方法
     * @param title 内容标题，处于首行，定义为数组为了应对多页的情况，如果仅有1个，将所有页应用该标题，如果仅有一个且为空，无标题
     * @return 封装好的处理类
     */
    public static IExcelBeanReverse getSimpleExcelBeanReverseInstance(List data, String title, OutExceptionHandler handler){
        return new SimpleExcelBeanReverse(ImmutableMap.of(1,data), new String[]{title}, handler);
    }



    /**
     * 获得导出集成类
     * @param data 数据（单页）
     * @param type 导出类型
     * @param handler 错误处理机制
     * @param title 内容标题，处于首行，定义为数组为了应对多页的情况，如果仅有1个，将所有页应用该标题，如果仅有一个且为空，无标题
     * @return 封装好的处理类
     */
    public static IExcelBeanReverse getSimpleExcelBeanReverseInstance(List data,ExcelDealType type,String[] title, OutExceptionHandler handler){
        return new SimpleExcelBeanReverse(data, type, title, handler);
    }

    /**
     * 获取导出集成类
     * @param data 代转换数据
     * @param asycSign 是否开启多线程共同输出
     * @param localCache 本地缓存
     * @param localCacheSize 本地缓存大小
     * @param type 导出种类
     * @param pageSize 每页显示记录数
     * @param handler 错误操作
     * @param mapKey 每页的映射id对应
     * @param title 内容标题，处于首行，定义为数组为了应对多页的情况，如果仅有1个，将所有页应用该标题，如果仅有一个且为空，无标题
     * @return 分装好的处理类
     */
    public static IExcelBeanReverse getSimpleExcelBeanReverseInstance(Map<Integer,List> data, boolean asycSign, boolean localCache
            , int localCacheSize, ExcelDealType type, int pageSize,Map<Integer,String> mapKey,String[] title, OutExceptionHandler handler){
        return new SimpleExcelBeanReverse(data,asycSign,localCache,localCacheSize,type,pageSize,mapKey,title,handler);
    }

    /**
     * 获取复杂导出集成类
     * @param id 复杂导出无法自动索引需要指定映射
     * @param data 数据（单页）
     * @param handler 错误处理方法
     * @param title 内容标题，处于首行，定义为数组为了应对多页的情况，如果仅有1个，将所有页应用该标题，如果仅有一个且为空，无标题
     * @return 封装好的处理类
     */
    public static IExcelBeanReverse getComplexExcelBeanReverseInstance(String id,List data,String[] title,OutExceptionHandler handler){
        return new ComplexExcelBeanReverse(id,data,title,handler);
    }

    /**
     * 获取复杂导出集成类
     * @param id 复杂导出无法自动索引需要指定映射
     * @param data 数据（单页）
     * @param type 导出数据种类
     * @param handler 错误处理方法
     * @param title 内容标题，处于首行，定义为数组为了应对多页的情况，如果仅有1个，将所有页应用该标题，如果仅有一个且为空，无标题
     * @return 封装好的处理类
     */
    public static IExcelBeanReverse getComplexExcelBeanReverseInstance(String id,List data,ExcelDealType type,String[] title,OutExceptionHandler handler){
        return new ComplexExcelBeanReverse(id,data,type,title,handler);
    }

    /**
     * 获取复杂导出集成类
     * @param ids 复杂导出多页指定映射
     * @param data 数据（多页）
     * @param handler 错误处理方法
     * @param title 内容标题，处于首行，定义为数组为了应对多页的情况，如果仅有1个，将所有页应用该标题，如果仅有一个且为空，无标题
     * @return 封装好的处理类
     */
    public static IExcelBeanReverse getComplexExcelBeanReverseInstance(Map<Integer,String> ids,Map<Integer,List> data,String[] title,OutExceptionHandler handler){
        return new ComplexExcelBeanReverse(ids,data,title,handler);
    }

    /**
     * 获取复杂导出集成类
     * @param data 代转换数据
     * @param asycSign 是否开启多线程共同输出
     * @param localCache 本地缓存
     * @param localCacheSize 本地缓存大小
     * @param type 导出种类
     * @param pageSize 每页显示记录数
     * @param handler 错误操作
     * @param mapKey 每页的映射id对应页码
     * @param title 内容标题，处于首行，定义为数组为了应对多页的情况，如果仅有1个，将所有页应用该标题，如果仅有一个且为空，无标题
     * @return 处理单元
     */
    public static IExcelBeanReverse getComplexExcelBeanReverseInstance(Map<Integer,List> data,boolean asycSign, boolean localCache, ExcelFactory.ExcelDealType type,
                            int localCacheSize, int pageSize, Map<Integer, String> mapKey,String[] title, OutExceptionHandler handler) {
        return new ComplexExcelBeanReverse(data,asycSign,localCache,type,localCacheSize,pageSize,mapKey,title,handler);
    }

    /**
     * 获取模板导出集成类
     * @param fileName 模板名
     * @param data 导入的映射数据
     * @return 处理单元
     * @throws FileNotFoundException 模板名未找到对应的文件
     */
    public static IExcelBeanReverse getModelExcelBeanReverseInstance(String fileName, List data) throws FileNotFoundException {
        return new ModelExcelBean(fileName, data);
    }
    /**
     * 获取模板导出集成类
     * @param fileName 模板名
     * @param data 导入的映射数据
     * @return 处理单元
     * @throws FileNotFoundException 模板名未找到对应的文件
     */
    public static IExcelBeanReverse getModelExcelBeanReverseInstance(String fileName, Object data) throws FileNotFoundException{
        return new ModelExcelBean(fileName, Collections.singletonList(data));
    }
}
