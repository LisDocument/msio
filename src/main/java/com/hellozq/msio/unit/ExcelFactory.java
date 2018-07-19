package com.hellozq.msio.unit;

import com.hellozq.msio.config.MsIoContainer;
import com.hellozq.msio.exception.IndexOutOfSheetSizeException;
import com.hellozq.msio.exception.UnsupportFormatException;
import com.hellozq.msio.utils.MsUtils;
import com.hellozq.msio.utils.SpringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bin
 * excel文件的处理工厂
 */
@SuppressWarnings("unused")
public class ExcelFactory {

    public SimpleExcelBean getSimpleInstance(Class<?> generateClass, boolean isTuring,@NotNull InputStream file){
        return new SimpleExcelBean(generateClass,isTuring,file);
    }

    /**
     * 设置方法
     * @param i 输入链接
     */

    private Workbook setWorkbook(@NotNull InputStream i){
        Workbook workbook;
        try {
            workbook = new XSSFWorkbook(i);
        }catch (Exception e){
            try{
                workbook = new HSSFWorkbook(i);
            }catch (Exception e1){
                e1.printStackTrace();
                throw new IllegalArgumentException("文件格式不符合，无法加入");
            }
        }
        return workbook;
    }

    /**
     * 简单excel实例单元
     * 简单Excel：没有复杂的合并单元格选项，一比一对比
     */
    private final class SimpleExcelBean{
        /**
         * 是否需要自动翻页
         */
        private boolean isTuring = true;
        /**
         * 是否根据自动更新Class
         */
        private boolean isChangeClass = false;

        private Class<?> clazz;

        private Workbook workbook;

        private Map<Integer, List> dataCache;

        private MsIoContainer msIoContainer;

        /**
         * 初始化
         * @param generateClass 指派导出类型，为null则自行查询
         * @param isTuring 是否自动迭代页面,自动迭代时导出类型无效
         * @param file 文件流
         */
        private SimpleExcelBean(Class<?> generateClass, boolean isTuring, InputStream file){
            this.clazz = generateClass;
            this.isTuring = isTuring;
            this.workbook = setWorkbook(file);
            msIoContainer = SpringUtils.getBean(MsIoContainer.class);
        }

        /**
         * 获取总页数
         * @return
         */
        protected int getPageSize(){
            return workbook.getNumberOfSheets();
        }


        protected List getPageContent(int pageIndex) throws IndexOutOfSheetSizeException,UnsupportFormatException{
            if(getPageSize() <= pageIndex){
                throw new IndexOutOfSheetSizeException("页码最大值为"+getPageSize()+"的数据，强行获取"+pageIndex+"页数据");
            }
            Sheet sheetNow = workbook.getSheetAt(pageIndex);
            int regionNum = sheetNow.getNumMergedRegions();
            //初始行
            int rowIndex = 0;
            if(regionNum > 1){
                throw new UnsupportFormatException("当前模式不支持多个合并单元格格式的解析，请切换解析方式为复杂方式");
            }
            //标题切除
            if(regionNum == 1){
                CellRangeAddress mergedRegion = sheetNow.getMergedRegion(0);
                if(mergedRegion.getFirstRow() != 0){
                    throw new UnsupportFormatException("当前模式仅支持首行标题合并解析，请切换解析方式为复杂模式");
                }
                rowIndex = mergedRegion.getLastRow() + 1;
            }
            //正式解析
            List<String> titles = MsUtils.getRowDataInString(rowIndex, 0, 0, sheetNow);
            LinkedHashMap<String, MsIoContainer.Information> mapping = null;
            //若clazz为null，则自动匹配
            if(clazz == null || isChangeClass){
                String match = msIoContainer.match(titles);
                mapping = msIoContainer.get(match);
                clazz = msIoContainer.getClazz(match);
            }else{
                mapping = msIoContainer.get(clazz);
            }
            
            return null;
        }


    }

    /**
     * 复杂excel实例单元
     * 复杂Excel：格式复杂，有合并单元格选项，需要筛选
     */
    private final class FlexExcelBean{

    }


}
