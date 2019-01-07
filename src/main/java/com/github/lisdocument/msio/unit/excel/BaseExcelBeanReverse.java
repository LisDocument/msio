package com.github.lisdocument.msio.unit.excel;

import com.github.lisdocument.msio.unit.func.OutExceptionHandler;
import com.github.lisdocument.msio.utils.SpringUtils;
import com.github.lisdocument.msio.config.MsIoContainer;
import com.github.lisdocument.msio.exception.DataUnCatchException;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * 基本抽取类导出
 * @author ThisLi(Bin)
 * time: 15:58
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseExcelBeanReverse implements IExcelBeanReverse{
    /**
     * 用户传入的数据，需要导出
     */
    Map<Integer,List> data;
    /**
     * 导出之后产生的workbook，用于写
     */
    Workbook workbook;

    /**
     * 标记是否集散多线程同时处理
     */
    boolean asycSign;
    /**
     * 是否开启本地缓存，本地缓存默认开启，仅对XLSX有效
     */
    boolean localCache;
    /**
     * 用于处理中途错误的问题
     */
    OutExceptionHandler handler;
    /**
     * 开启本地缓存后，缓存的数量
     */
    int localCacheSize;
    /**
     * 页码，存储当前页码指针位置
     */
    int pageIndex = 0;
    /**
     * 每页数据最大承受值，如果达到了会自动进行翻页
     */
    int pageSize;

    /**
     * 文件导出后的格式
     */
    ExcelFactory.ExcelDealType type;

    /**
     * 缓存的页码索引对象
     */
    Map<Integer,String> mapKey;

    MsIoContainer msIoContainer = SpringUtils.getBean(MsIoContainer.class);

    @Override
    public Workbook getWorkbook() {
        return this.workbook;
    }

    abstract void translator() throws DataUnCatchException;

    BaseExcelBeanReverse(Map<Integer, List> data, boolean asycSign, boolean localCache,
                                OutExceptionHandler handler, int localCacheSize, int pageSize,
                                ExcelFactory.ExcelDealType type, Map<Integer,String> mapKey) {
        this.data = data;
        this.asycSign = asycSign;
        this.localCache = localCache;
        this.handler = handler;
        this.localCacheSize = localCacheSize;
        this.pageSize = pageSize;
        this.type = type;
        this.mapKey = mapKey;
        translator();
    }
}
