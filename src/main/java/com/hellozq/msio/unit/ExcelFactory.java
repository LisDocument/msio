package com.hellozq.msio.unit;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.hellozq.msio.bean.common.IFormatConversion;
import com.hellozq.msio.config.MsIoContainer;
import com.hellozq.msio.exception.IndexOutOfSheetSizeException;
import com.hellozq.msio.exception.UnsupportFormatException;
import com.hellozq.msio.utils.ClassUtils;
import com.hellozq.msio.utils.MsUtils;
import com.hellozq.msio.utils.SpringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.*;

/**
 * @author bin
 * excel文件的处理工厂
 */
@SuppressWarnings("unused")
public class ExcelFactory {

    private static final Log log = LogFactory.getLog(ExcelFactory.class);

    /**
     * 获得单页的excel解析结果
     * @param id 指定映射的id，如果为空的话自动寻找
     * @param file 文件流
     * @param pageNo 页码,默认为0
     * @return 新的简单excel实例
     */
    public static SimpleExcelBean getSingleSimpleInstance(String id, @NotNull InputStream file,Integer pageNo){
        return new SimpleExcelBean(id,file,pageNo == null ? 0 : pageNo);
    }

    /**
     * 复数也的Excel解析结果
     * @param ids 单页id集合，会根据id索引每页，保证长度和页数一致，若不确定id，可设置isChangeClass为true，自动查询索引，若为false，且那个位置的元素为null，则跳过该数据
     * @param file 文件流
     * @param isChangeClass 是否自动加载映射
     * @return 新的实例
     */
    public static SimpleExcelBean getMultipleSimpleInstance(List<String> ids,@NotNull InputStream file,boolean isChangeClass){
        if(ids != null && !ids.isEmpty()){
            return new SimpleExcelBean(ids,file,isChangeClass);
        }
        return new SimpleExcelBean(file,isChangeClass);
    }

    /**
     * 设置方法
     * @param i 输入链接
     */

    private static Workbook setWorkbook(@NotNull InputStream i){
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
    private static final class SimpleExcelBean{
        /**
         * 是否需要自动翻页
         */
        private boolean isTuring = true;
        /**
         * 是否根据自动更新Class
         */
        private boolean isChangeClass = true;

        private List<String> idPool;

        private Class<?> clazz;

        private String id;

        private Workbook workbook;

        private Map<Integer, List> dataCache;

        private MsIoContainer msIoContainer;

        private IFormatConversion formatConversion;

        /**
         * 获取该页的数据
         * @param pageIndex 页码
         * @return 数据
         */
        public List getData(int pageIndex){
            return dataCache.getOrDefault(pageIndex,new ArrayList());
        }

        private SimpleExcelBean(@NotNull InputStream file){
            this.workbook = setWorkbook(file);
            this.msIoContainer = SpringUtils.getBean(MsIoContainer.class);
            this.formatConversion = SpringUtils.getBean(IFormatConversion.class);
        }

        /**
         * 单页初始化
         * @param id 指派导出类型，为null则自行查询
         * @param file 文件流
         */
        private SimpleExcelBean(String id, @NotNull InputStream file,@NotNull Integer pageIndex){
            this(file);
            this.isTuring = false;
            if(StringUtils.isEmpty(id)) {
                this.isChangeClass = true;
            }else {
                this.id = id;
                this.clazz = msIoContainer.getClazz(id);
                this.isChangeClass =  false;
            }
            try {
                dataCache.put(pageIndex, this.getPageContent(pageIndex));
            } catch (IndexOutOfSheetSizeException | UnsupportFormatException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        /**
         * 多页指定每页的id并初始化
         * @param idPool 每页的id池，会根据页码去索引，请根据顺序给定
         * @param file 文件流
         * @param isChangeClass 是否自动去寻找类，若设置为false则会省略当前无映射的页
         */
        private SimpleExcelBean(@NotNull List<String> idPool,@NotNull InputStream file,boolean isChangeClass){
            this(file);
            this.isTuring = true;
            this.isChangeClass = isChangeClass;
            this.idPool = idPool;
            automaticPageTurningWithMapping();
        }

        /**
         * 多页不指定每页的id初始化
         * @param file 文件流
         * @param isChangeClass 若没有指定的class对象是否进行自动寻找
         */
        private SimpleExcelBean(@NotNull InputStream file,@NotNull boolean isChangeClass){
            this(file);
            this.isTuring = true;
            this.isChangeClass = true;
            automaticPageTurningWithoutMapping();
        }

        /**
         * 未指定反射对象迭代获取
         */
        private void automaticPageTurningWithoutMapping(){
            for (int i = 0; i < getPageSize(); i++) {
                try {
                    dataCache.put(i, this.getPageContent(i));
                } catch (IndexOutOfSheetSizeException | UnsupportFormatException | NoSuchMethodException e) {
                    log.error("迭代时发生异常，异常页" + i);
                    e.printStackTrace();
                }
            }
        }
        /**
         * 指定反射对象迭代获取
         */
        private void automaticPageTurningWithMapping(){
            for (int i = 0; i < idPool.size(); i++) {
                if(StringUtils.isEmpty(idPool.get(i)) || !isChangeClass){
                    continue;
                }
                this.id = idPool.get(i);
                this.clazz = msIoContainer.getClazz(this.id);
                try {
                    dataCache.put(i, this.getPageContent(i));
                } catch (IndexOutOfSheetSizeException | UnsupportFormatException | NoSuchMethodException e) {
                    log.error("迭代时发生异常，异常页" + i);
                    e.printStackTrace();
                }
            }
        }

        /**
         * 获取总页数
         * @return 页数
         */
        @SuppressWarnings("all")
        protected int getPageSize(){
            return workbook.getNumberOfSheets();
        }

        /**
         * 获取当前页的数据
         * @param pageIndex 页码
         * @return 当前页解析的结果
         * @throws IndexOutOfSheetSizeException 输入页码数错误超过范围
         * @throws UnsupportFormatException 不支持的excel格式
         * @throws NoSuchMethodException 解析的时候未找到相应转换方法报的错误
         */
        @SuppressWarnings("unchecked")
        private List getPageContent(int pageIndex) throws IndexOutOfSheetSizeException,UnsupportFormatException,NoSuchMethodException{

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
            if(titles == null || titles.size() == 0){
                throw new NullPointerException("标题行为空，请检查格式");
            }
            LinkedHashMap<String, MsIoContainer.Information> mapping;
            //若clazz为null，则自动匹配
            if(id == null || isChangeClass){
                String match = msIoContainer.match(titles);
                mapping = msIoContainer.get(match);
                clazz = msIoContainer.getClazz(match);
            }else{
                mapping = msIoContainer.get(id);
            }
            LinkedHashMap<String, String> inversion = MsUtils.mapInversion(mapping);
            if(inversion.isEmpty()){
                titles.forEach(s -> inversion.put(s,s));
            }
            List list = new ArrayList();
            if(clazz == Map.class){
                for (int i = rowIndex; i < sheetNow.getLastRowNum(); i++) {
                    list.add(conversionMap(sheetNow.getRow(rowIndex), inversion, titles));
                }
            }else{
                for (int i = rowIndex; i < sheetNow.getLastRowNum(); i++) {
                    list.add(conversionPojo(sheetNow.getRow(rowIndex), inversion, titles, clazz, mapping));
                }
            }
            return list;
        }

        /**
         * 内置工具方法，获取当前行的解析结果
         * @param row 待处理数据
         * @param inversion 映射数据
         * @param titles 提取出的标题数据
         * @return 解析结果
         */
        private Map<String,String> conversionMap(Row row, LinkedHashMap<String, String> inversion, List<String> titles){
            Map<String, String> result = new HashMap<>(16);
            for (int i = 0; i < titles.size(); i++) {
                String value = MsUtils.getStringValueFromCell(row.getCell(i));
                result.put(inversion.get(titles.get(i)),value);
            }
            return result;
        }

        /**
         * 内置工具方法，获取当前行的解析Pojo结果
         * @param row 待处理行数据
         * @param inversion 映射数据
         * @param titles 提取出来的标题数据
         * @param clazz Pojo对应类
         * @param auto 自动赋值方法
         * @return 解析结果
         */
        @SuppressWarnings("all")
        private Object conversionPojo(Row row, LinkedHashMap<String, String> inversion, List<String> titles, Class<?> clazz,
                                      LinkedHashMap<String,MsIoContainer.Information> auto) throws NoSuchMethodException{
            Object obj = null;
            try {
                obj = clazz.newInstance();
            } catch (InstantiationException|IllegalAccessException e) {
                log.error(clazz.getName() + "创建失败，请检查是否存在无参构造函数或者是否设置构造函数为non-private");
                e.printStackTrace();
            }
            for (int i = 0; i < titles.size(); i++) {
                String value = MsUtils.getStringValueFromCell(row.getCell(i));
                String title = titles.get(i);
                String egTitle = inversion.get(title);
                MsIoContainer.Information information = auto.get(title);
                if(information.getFieldType() == String.class){
                    ClassUtils.setFieldValue(value, egTitle, obj, clazz);
                    //倘若导入目标为集合的情况
                }else if(List.class.isAssignableFrom(information.getFieldType())){
                    String simpleName = "fromStringtoListBy" + information.getFieldType().getSimpleName();
                    MethodAccess methodAccess = ClassUtils.getMethodAccess(formatConversion.getClass());
                    int methodIndex;
                    try {
                        methodIndex = methodAccess.getIndex(simpleName, String.class);
                    }catch (IllegalArgumentException e){
                        log.error("尝试使用" + simpleName + "获取方法失败，正在尝试使用全名获取");
                        String flexName = "fromStringtoSetBy" + information.getFieldType().getName().replaceAll(".", "");
                        try {
                            methodIndex = methodAccess.getIndex(flexName, String.class);
                        }catch (IllegalArgumentException e1){
                            log.error("尝试使用" + flexName + "获取方法失败，抛出异常，请检查是否存在方法或者方法是否设置为non-private");
                            throw new NoSuchMethodException("无法找到方法" + simpleName + "、" + flexName);
                        }
                    }
                    Object invoke = methodAccess.invoke(formatConversion, methodIndex, String.class, value);
                    ClassUtils.setFieldValue(value, egTitle, obj, clazz);
                }else if(Set.class.isAssignableFrom(information.getFieldType())){
                    String simpleName = "fromStringtoSetBy" + information.getFieldType().getSimpleName();
                    MethodAccess methodAccess = ClassUtils.getMethodAccess(formatConversion.getClass());
                    int methodIndex;
                    try {
                        methodIndex = methodAccess.getIndex(simpleName, String.class);
                    }catch (IllegalArgumentException e){
                        log.error("尝试使用" + simpleName + "获取方法失败，正在尝试使用全名获取");
                        String flexName = "fromStringtoSetBy" + information.getFieldType().getName().replaceAll(".", "");
                        try {
                            methodIndex = methodAccess.getIndex(flexName, String.class);
                        }catch (IllegalArgumentException e1){
                            log.error("尝试使用" + flexName + "获取方法失败，抛出异常，请检查是否存在方法或者方法是否设置为non-private");
                            throw new NoSuchMethodException("无法找到方法" + simpleName + "、" + flexName);
                        }
                    }
                    Object invoke = methodAccess.invoke(formatConversion, methodIndex, String.class, value);
                    ClassUtils.setFieldValue(value, egTitle, obj, clazz);
                }else{
                    String simpleName = "fromStringto" + information.getFieldType().getSimpleName();
                    MethodAccess methodAccess = ClassUtils.getMethodAccess(formatConversion.getClass());
                    int methodIndex;
                    try {
                        methodIndex = methodAccess.getIndex(simpleName, String.class);
                    }catch (IllegalArgumentException e){
                        log.error("尝试使用" + simpleName + "获取方法失败，正在尝试使用全名获取");
                        String flexName = "fromStringto" + information.getFieldType().getName().replaceAll(".", "");
                        try {
                            methodIndex = methodAccess.getIndex(flexName, String.class);
                        }catch (IllegalArgumentException e1){
                            log.error("尝试使用" + flexName + "获取方法失败，抛出异常，请检查是否存在方法或者方法是否设置为non-private");
                            throw new NoSuchMethodException("无法找到方法" + simpleName + "、" + flexName);
                        }
                    }
                    Object invoke = methodAccess.invoke(formatConversion, methodIndex, String.class, value);
                    ClassUtils.setFieldValue(value, egTitle, obj, clazz);
                }
            }
            return obj;
        }
    }

    /**
     * 复杂excel实例单元
     * 复杂Excel：格式复杂，有合并单元格选项，需要筛选
     */
    private final class FlexExcelBean{

    }


}
