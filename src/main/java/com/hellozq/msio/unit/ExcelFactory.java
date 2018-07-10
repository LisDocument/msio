package com.hellozq.msio.unit;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.List;

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

    private Workbook setWorkbook(InputStream i){
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
        private boolean isTuring;

        private Class<?> clazz;

        private Workbook workbook;

        private List<List> dataCache;

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
        }


    }

    /**
     * 复杂excel实例单元
     * 复杂Excel：格式复杂，有合并单元格选项，需要筛选
     */
    private final class FlexExcelBean{

    }


}
