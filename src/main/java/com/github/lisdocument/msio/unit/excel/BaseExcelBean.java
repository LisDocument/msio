package com.github.lisdocument.msio.unit.excel;

import com.github.lisdocument.msio.bean.common.IFormatConversion;
import com.github.lisdocument.msio.config.MsIoContainer;
import com.github.lisdocument.msio.utils.MsUtils;
import com.github.lisdocument.msio.utils.SpringUtils;
import com.google.common.collect.Maps;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author ThisLi(Bin)
 * time: 9:03
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseExcelBean implements IExcelBean {

    /**
     * 是否需要自动翻页
     */
    boolean isTuring;

    Workbook workbook;

    Map<Integer, List> dataCache;

    MsIoContainer msIoContainer = SpringUtils.getBean(MsIoContainer.class);

    IFormatConversion formatConversion = SpringUtils.getBean(IFormatConversion.class);

    @Override
    public List getData(Integer pageNo) {
        return dataCache.getOrDefault(pageNo,new ArrayList());
    }

    /**
     * 参数构造
     * @param file 文件实例
     * @param isTuring 是否翻页
     */
    BaseExcelBean(File file,boolean isTuring){
        this.workbook = MsUtils.transWorkbook(file);
        this.isTuring = isTuring;
        this.dataCache = Maps.newHashMapWithExpectedSize(64);
    }

    /**
     * 参数构造
     * @param file 文件实例
     * @param isTuring 是否翻页
     */
    BaseExcelBean(MultipartFile file,boolean isTuring){
        this.workbook = MsUtils.transWorkbook(file);
        this.isTuring = isTuring;
        this.dataCache = Maps.newHashMapWithExpectedSize(64);
    }
}
