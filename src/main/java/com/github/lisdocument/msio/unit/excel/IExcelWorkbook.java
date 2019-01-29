package com.github.lisdocument.msio.unit.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.http.HttpServletResponse;

/**
 * Created with IntelliJ IDEA.
 * 包装方法类
 * @author Libin
 */
public interface IExcelWorkbook {

    /**
     * 写操作
     * @param response 写入响应
     */
    void write(HttpServletResponse response);

    /**
     * 将workbook输入
     * @param workbook 工作簿
     */
    void setWorkbook(Workbook workbook);

    /**
     * 获取工作簿
     * @return 工作簿
     */
    Workbook getWorkbook();

    /**
     * 设置列长
     * @param cellIndex 列号
     * @param width 长度
     * @param sheetNum 工作簿序号
     */
    void setColumnWidth(Integer sheetNum,Integer cellIndex,Integer width);

    /**
     * 合并单元格,默认居中处理
     * @param value 单元格需要填入数据
     * @param startRowNo 开始行
     * @param endRowNo 结束行/包括
     * @param startColumnNo 开始列
     * @param endColumnNo 结束列/包括
     * @param isCover 是否覆盖 ，true会删除之前的数据，false若是之前有数据会进行拼接放入，value会被无视
     * @param isTitle 是否标题，标题格式会被处理,true为标题
     * @param sheetNum 工作簿序号
     */
    void mergeAndCenteredCell(Integer sheetNum, String value, int startRowNo, int endRowNo,
                              int startColumnNo, int endColumnNo, boolean isCover, boolean isTitle);

    /**
     * 设置字体格式
     * @param fontName 字体名称
     * @param fontSize 字体大小
     * @param cell 单元格
     */
    void setFont(Cell cell,String fontName,short fontSize);

    /**
     * 获取行
     * @param rowNum 行号
     * @param sheetNum 工作簿号
     * @return 行对象
     */
    Row getRow(Integer sheetNum,Integer rowNum);

    /**
     * 获取单元格
     * @param rowNum 行号
     * @param cellNum 列号
     * @param sheetNum 工作簿号
     * @return 单元格
     */
    Cell getCell(Integer sheetNum,Integer rowNum, Integer cellNum);

    /**
     * 设置文件名称
     * @param fileName 文件名称
     */
    void setFileName(String fileName);
}
