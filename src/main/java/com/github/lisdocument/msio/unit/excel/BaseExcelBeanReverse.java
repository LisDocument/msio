package com.github.lisdocument.msio.unit.excel;

import com.github.lisdocument.msio.unit.func.OutExceptionHandler;
import com.github.lisdocument.msio.utils.MsUtils;
import com.github.lisdocument.msio.utils.SpringUtils;
import com.github.lisdocument.msio.config.MsIoContainer;
import com.github.lisdocument.msio.exception.DataUnCatchException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
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
    /**
     * 标题
     */
    String[] title;

    MsIoContainer msIoContainer = SpringUtils.getBean(MsIoContainer.class);

    @Override
    public Workbook getWorkbook() {
        return this.workbook;
    }

    /**
     * 钩子函数，翻译用
     * @throws DataUnCatchException 错误
     */
    abstract void translator() throws DataUnCatchException;

    BaseExcelBeanReverse(Map<Integer, List> data, boolean asycSign, boolean localCache,
                                OutExceptionHandler handler, int localCacheSize, int pageSize,
                                ExcelFactory.ExcelDealType type, Map<Integer,String> mapKey,String[] title) {
        this.data = data;
        this.asycSign = asycSign;
        this.localCache = localCache;
        this.handler = handler;
        this.localCacheSize = localCacheSize;
        this.pageSize = pageSize;
        this.type = type;
        this.mapKey = mapKey;
        this.title = title;
        translator();
    }

    @Override
    public void write(HttpServletResponse response, String fileName) throws IOException {
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + MsUtils.toUtf8String(fileName));
        workbook.write(response.getOutputStream());
    }

    @Override
    public void write(OutputStream out) throws IOException{
        workbook.write(out);
    }

    /**
     * 获取链式样式修改类
     * @param sheetNo 工作簿序号，0开始
     * @return 样式封装类
     */
    public StyleBuilder builder(Integer sheetNo){
        if(localCache){
            throw new IllegalArgumentException("本地缓存需要关闭态，否则无法修改样式");
        }
        return new StyleBuilder(workbook.getSheetAt(sheetNo), workbook);
    }

    /**
     * 获取链式样式修改类
     * @param sheetName 工作簿名称
     * @return 样式封装类
     */
    public StyleBuilder builder(String sheetName){
        if(localCache){
            throw new IllegalArgumentException("本地缓存需要关闭态，否则无法修改样式");
        }
        return new StyleBuilder(workbook.getSheet(sheetName), workbook);
    }

    /**
     * 样式生成，此方法无法在本地缓存情况下开启，在大量行数据生成时
     * 前面内容会被直接缓存到本地，内存中无法缓存内容，若使用此工具模块，
     * 必须保证是非缓存阵列，即localCache关闭态
     * @author bin
     * @version 1.0.1
     */
    public class StyleBuilder{

        private Sheet sheet;

        private Workbook workbook;

        public StyleBuilder(Sheet sheet,Workbook workbook) {
            this.sheet = sheet;
            this.workbook = workbook;
        }

        public BaseExcelBeanReverse build(){
            return BaseExcelBeanReverse.this;
        }

        /**
         * 单元格居中
         * @param rowNo 行号
         * @param cellNo 列号
         * @return this
         */
        public StyleBuilder center(int rowNo, int cellNo){
            CellStyle cellStyle = workbook.createCellStyle();
            Cell cell = MsUtils.createOrGetCell(sheet, rowNo, cellNo);
            cellStyle.cloneStyleFrom(cell.getCellStyle());
            cellStyle.setAlignment(XSSFCellStyle.ALIGN_CENTER);
            cellStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
            cell.setCellStyle(cellStyle);

            return this;
        }

        /**
         * 单元格赋值
         * @param rowNo 行号
         * @param cellNo 列号
         * @param value 单元格值
         * @return this
         */
        public StyleBuilder set(int rowNo, int cellNo, String value){
            Cell cell = MsUtils.createOrGetCell(sheet, rowNo, cellNo);
            cell.setCellValue(value);

            return this;
        }

        /**
         * 单元格字体修改
         * @param rowNo 行号
         * @param cellNo 列号
         * @param fontName 字体名称，‘黑体’亲测无法成功
         * @param fontSize 字体大小
         * @return this
         */
        public StyleBuilder setFont(int rowNo, int cellNo, String fontName, short fontSize){
            CellStyle cellStyle = workbook.createCellStyle();
            Cell cell = MsUtils.createOrGetCell(sheet, rowNo, cellNo);
            cellStyle.cloneStyleFrom(cell.getCellStyle());
            Font font = workbook.createFont();
            font.setFontName(fontName);
            // 设置字体大小
            font.setFontHeightInPoints(fontSize);
            cellStyle.setFont(font);

            return this;
        }


//        /**
//         * 单元背景色
//         * @param rowNo 行号
//         * @param cellNo 列号
//         * @return this
//         */
//        public StyleBuilder color(int rowNo, int cellNo){
//            CellStyle cellStyle = workbook.createCellStyle();
//            Cell cell = MsUtils.createOrGetCell(sheet, rowNo, cellNo);
//            cellStyle.cloneStyleFrom(cell.getCellStyle());
//            cell.setCellStyle(cellStyle);
//
//            return this;
//        }

        /**
         * 合并单元格,默认居中处理
         * @param value 单元格需要填入数据
         * @param startRowNo 开始行
         * @param endRowNo 结束行/包括
         * @param startColumnNo 开始列
         * @param endColumnNo 结束列/包括
         * @param isCover 是否覆盖 ，true会删除之前的数据，false若是之前有数据会进行拼接放入，value会被无视
         * @param isTitle 是否标题，标题格式会被处理,true为标题
         * @return this
         */
        public StyleBuilder merge(String value, int startRowNo, int endRowNo,
                                  int startColumnNo, int endColumnNo,boolean isCover,boolean isTitle){
            MsUtils.mergeAndCenteredCell(sheet, value, startRowNo, endRowNo, startColumnNo, endColumnNo, isCover, isTitle);

            return this;
        }

    }
}
