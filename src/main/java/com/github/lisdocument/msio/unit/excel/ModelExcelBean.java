package com.github.lisdocument.msio.unit.excel;

import com.github.lisdocument.msio.utils.ClassUtils;
import com.github.lisdocument.msio.utils.MsUtils;
import com.github.lisdocument.msio.utils.StringRegexUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.util.ResourceUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * 创建excel，model导出方式bean
 * 传入的List为每页的数据，可传入map或者pojo，会根据key或者属性名称将数据填入模板
 * @author Libin
 * @version 1.0.2
 */
public class ModelExcelBean  implements IExcelBeanReverse{
    /**
     * 模板
     */
    private Workbook workbook;
    /**
     * 模板名称，模板
     */
    private String fileName;
    /**
     * 数据
     */
    private List data;

    ModelExcelBean(String fileName, List data) throws FileNotFoundException {
        File file = ResourceUtils.getFile("classpath:model/" + fileName);
        this.workbook = MsUtils.transWorkbook(file);
        this.data = data;
        this.fileName = fileName;
        transport();
    }

    private void transport(){
        for (int i = 0; i < data.size(); i++) {
            Object dataItem = data.get(i);
            Sheet sheet = workbook.getSheetAt(i);
            int lastRowNum = sheet.getLastRowNum();
            for (int rowNum = 0; rowNum < lastRowNum; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if(null == row){
                    continue;
                }
                int lastCellNum = row.getLastCellNum() + 0;
                for (int cellNum = 0; cellNum < lastCellNum; cellNum++) {
                    Cell cell = row.getCell(cellNum);
                    if(null == cell){
                        continue;
                    }
                    String cellValue = MsUtils.getStringValueFromCell(cell);
                    if(cellValue.startsWith("${")){
                        String key = cellValue.substring(1);
                        key = key.substring(key.length() - 2);
                        Object value = dataItem;
                        if(dataItem instanceof Map){
                            for (String s : key.split("\\.")) {
                                value = ((Map) value).getOrDefault(s, "");
                            }
                        }else{
                            for (String s : key.split("\\.")) {
                                value = ClassUtils.getFieldValue(s, value, dataItem.getClass());
                            }

                        }
                        cell.setCellValue(StringRegexUtils.getOrDefault(value, ""));
                    }
                }
            }
        }
    }

    @Override
    public Workbook getWorkbook() {
        return workbook;
    }

    @Override
    public void write(OutputStream out) throws IOException{
        workbook.write(out);
    }

    @Override
    public void write(HttpServletResponse response, String fileName) throws IOException {
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + MsUtils.toUtf8String(fileName));
        workbook.write(response.getOutputStream());
    }
}
