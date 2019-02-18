package com.github.lisdocument.msio.unit.excel;

import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created with IntelliJ IDEA.
 * excel的导出公用方法
 * @author ThisLi(Bin)
 * time: 9:57
 * To change this template use File | Settings | File Templates.
 */
public interface IExcelBeanReverse {

    /**
     * 获取已经完成导出的workbook集
     * @return workbook集
     */
    Workbook getWorkbook();

    /**
     * 文件写出方法
     * @param response 响应
     * @param fileName 文件名称
     * @throws IOException 写入时出错
     */
    void write(HttpServletResponse response, String fileName) throws IOException;

    /**
     * 文件写出方法
     * @param out 输出流
     * @throws IOException 写入时出错
     */
    void write(OutputStream out) throws IOException;
}
