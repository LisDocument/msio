package com.github.lisdocument.msio.unit.excel;

import com.github.lisdocument.msio.utils.MsUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.http.HttpServletResponse;

/**
 * Created with IntelliJ IDEA.
 * 做一部分实现的基本类
 * @author Libin
 */
public abstract class AbstractExcelWorkbook implements IExcelWorkbook{

    private Workbook workbook;

    private String fileName = "download.xlsx";

    @Override
    public void write(HttpServletResponse response) {
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + MsUtils.toUtf8String(fileName));
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void setWorkbook(Workbook workbook) {
        this.workbook = workbook;
    }

    @Override
    public Workbook getWorkbook() {
        return workbook;
    }

    @Override
    public void setColumnWidth(Integer sheetNum,Integer cellIndex, Integer width) {
        Sheet sheetAt = workbook.getSheetAt(sheetNum);
        sheetAt.setColumnWidth(cellIndex,width);
    }

    @Override
    public void mergeAndCenteredCell(Integer sheetNo, String value, int startRowNo, int endRowNo, int startColumnNo, int endColumnNo, boolean isCover, boolean isTitle) {

    }

    @Override
    public void setFont(Cell cell,String fontName, short fontSize) {

    }

    @Override
    public Row getRow(Integer sheetNum, Integer rowNum) {
        return null;
    }

    @Override
    public Cell getCell(Integer sheetNum, Integer rowNum, Integer cellNum) {
        return null;
    }
}
