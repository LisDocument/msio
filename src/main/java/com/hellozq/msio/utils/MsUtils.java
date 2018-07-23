package com.hellozq.msio.utils;

import com.hellozq.msio.config.MsIoContainer;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.util.StringUtils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author bin
 * 读取数据时用到的工具方法
 */
@SuppressWarnings("unused")
public class MsUtils {

    /**
     * 映射关系转换方法
     * @param data 映射关系
     * @return 转换后的简单反转映射关系
     */
    public static LinkedHashMap<String, String> mapInversion(LinkedHashMap<String,MsIoContainer.Information> data){
        LinkedHashMap<String, String> result = new LinkedHashMap<>(16);
        data.forEach((k,v) -> result.put(v.getName(),k));
        return result;
    }

    /**
     * 判断指定的单元格是否是合并单元格
     * @param row 行下标
     * @param column 列下标
     * @return 合并单元格制式id
     */
    public static int isMergedRegion(int row , int column, Sheet sheet) {
        int sheetMergeCount = sheet.getNumMergedRegions();
        for (int i = 0; i < sheetMergeCount; i++) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            int firstColumn = range.getFirstColumn();
            int lastColumn = range.getLastColumn();
            int firstRow = range.getFirstRow();
            int lastRow = range.getLastRow();
            if(row >= firstRow && row <= lastRow){
                if(column >= firstColumn && column <= lastColumn){
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 根据行号，起始列，读取长度获取一段String类型数据
     * @param rowNum 行号
     * @param columnNo 起始列
     * @param size 数据读取长度
     * @return 当前行产生的String数据
     */
    public static List<String> getRowDataInString(int rowNum, int columnNo, int size, Sheet sheet){
        List<String> result = new ArrayList<>();
        Row row = sheet.getRow(rowNum);
        if(size == 0){
            size = row.getLastCellNum() - columnNo;
        }
        boolean sign = true;
        for (int i = columnNo; i < columnNo + size; i++) {
            String value = getStringValueFromCell(row.getCell(i));
            if(StringUtils.isEmpty(value)||"".equals(value.trim())){
                result.add(null);
                continue;
            }
            result.add(value);
            sign = false;
        }
        //此行为空
        if(sign){
            return null;
        }
        return result;
    }

    /**
     * 将一切格式以String格式录入
     * @param cell 单元格
     * @return 读取到的数据
     */
    public static String getStringValueFromCell(Cell cell) {
        SimpleDateFormat sFormat = new SimpleDateFormat("MM/dd/yyyy");
        DecimalFormat decimalFormat = new DecimalFormat("#.#");
        String cellValue = "";
        if(cell == null) {
            return cellValue;
        }
        else if(cell.getCellType() == Cell.CELL_TYPE_STRING) {
            cellValue = cell.getStringCellValue();
        }

        else if(cell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {
            if(HSSFDateUtil.isCellDateFormatted(cell)) {
                double d = cell.getNumericCellValue();
                Date date = HSSFDateUtil.getJavaDate(d);
                cellValue = sFormat.format(date);
            }
            else {
                cellValue = decimalFormat.format((cell.getNumericCellValue()));
            }
        }
        else if(cell.getCellType() == Cell.CELL_TYPE_BLANK) {
            cellValue = "";
        }
        else if(cell.getCellType() == Cell.CELL_TYPE_BOOLEAN) {
            cellValue = String.valueOf(cell.getBooleanCellValue());
        }
        else if(cell.getCellType() == Cell.CELL_TYPE_ERROR) {
            cellValue = "";
        }
        else if(cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
            cellValue = cell.getCellFormula();
        }
        return cellValue;
    }
}
