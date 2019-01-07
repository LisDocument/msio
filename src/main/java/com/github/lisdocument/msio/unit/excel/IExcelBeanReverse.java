package com.github.lisdocument.msio.unit.excel;

import org.apache.poi.ss.usermodel.Workbook;

/**
 * Created with IntelliJ IDEA.
 * excel的导出公用方法
 * @author ThisLi(Bin)
 * @date 2018/12/13
 * time: 9:57
 * To change this template use File | Settings | File Templates.
 */
public interface IExcelBeanReverse {

    /**
     * 获取已经完成导出的workbook集
     * @return workbook集
     */
    Workbook getWorkbook();

}
