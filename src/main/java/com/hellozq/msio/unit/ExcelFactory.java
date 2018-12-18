package com.hellozq.msio.unit;

import com.esotericsoftware.reflectasm.MethodAccess;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hellozq.msio.bean.common.IFormatConversion;
import com.hellozq.msio.config.MsIoContainer;
import com.hellozq.msio.exception.DataUnCatchException;
import com.hellozq.msio.exception.IndexOutOfSheetSizeException;
import com.hellozq.msio.exception.UnsupportFormatException;
import com.hellozq.msio.unit.func.OutExceptionHandler;
import com.hellozq.msio.utils.ClassUtils;
import com.hellozq.msio.utils.MsUtils;
import com.hellozq.msio.utils.SpringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
     * 复数页的Excel解析结果
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
     * @author bin
     */
    public enum ExcelDealType{
        /**
         * .xls
         */
        XLS,
        /**
         * .xlsx
         */
        XLSX
    }

    /**
     * 简单excel实例单元-输入单元
     * 简单Excel：没有复杂的合并单元格选项，一比一对比
     */
    public static final class SimpleExcelBean{
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
                String match = msIoContainer.match(titles,false);
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

    private static ConcurrentHashMap<String,SimpleExcelBeanReverse> beanCache = new ConcurrentHashMap<>();
    /**
     * 获取导出集成类
     * @param data 数据
     * @param asycSign 集散多线程同步处理
     * @param localCache 本地缓存使用SXXSF解析
     * @param localCacheSize 本地缓存数量
     * @param type 解析文件名
     * @param handler 错误处理方法
     * @return 封装好的处理
     */
    public static IExcelBeanReverse getSimpleExcelBeanReverseInstance(Map<Integer,List> data,boolean asycSign,
                                                                           boolean localCache,int localCacheSize, ExcelDealType type,
                                                                           OutExceptionHandler handler,int pageSize){
        SimpleExcelBeanReverse bean = new SimpleExcelBeanReverse(data, asycSign, localCache, localCacheSize, type, handler, pageSize);
        return bean;
    }

    /**
     * 获取导出集成类
     * @param data 数据
     * @param handler 错误处理方法
     * @return 封装好的处理类
     */
    public static IExcelBeanReverse getSimpleExcelBeanReverseInstance(Map<Integer,List> data,OutExceptionHandler handler){
        return new SimpleExcelBeanReverse(data,handler);
    }

    /**
     * 获取导出集成类
     * @param data 数据(单页)
     * @param handler 错误处理方法
     * @return 封装好的处理类
     */
    public static IExcelBeanReverse getSimpleExcelBeanReverseInstance(List data,OutExceptionHandler handler){
        return new SimpleExcelBeanReverse(ImmutableMap.of(1,data),handler);
    }

    /**
     * 简易excel实例单元-输出单元
     */
    public static final class SimpleExcelBeanReverse implements IExcelBeanReverse{

        /**
         * 用户传入的数据，需要导出
         */
        private Map<Integer,List> data;
        /**
         * 导出之后产生的workbook，用于写
         */
        private Workbook workbook;
        /**
         * 标记是否集散多线程同时处理
         */
        private boolean asycSign;
        /**
         * 是否开启本地缓存，本地缓存默认开启，仅对XLSX有效
         */
        private boolean localCache;
        /**
         * 用于处理中途错误的问题
         */
        private OutExceptionHandler handler = null;
        /**
         * 开启本地缓存后，缓存的数量
         */
        private int localCacheSize;
        /**
         * 页码，存储当前页码指针位置
         */
        private int pageIndex = 0;
        /**
         * 每页数据最大承受值，如果达到了会自动进行翻页
         */
        private int pageSize;
        /**
         * 缓存的页码索引对象
         */
        private Map<Integer,String> mapKey = Maps.newHashMapWithExpectedSize(64);
        /**
         * 文件导出后的格式
         */
        private ExcelDealType type;

        private MsIoContainer msIoContainer;
        /**
         * 获取到编译好的工作簿
         * @return
         */
        @Override
        public Workbook getWorkbook() {
            return workbook;
        }

        public void setData(Map<Integer, List> data){
            this.data = data;
        }

        private SimpleExcelBeanReverse(Map<Integer,List> data,boolean asycSign,boolean localCache
                ,int localCacheSize,ExcelDealType type,OutExceptionHandler handler,int pageSize){
            this.data = data;
            this.asycSign = asycSign;
            this.localCache = localCache;
            this.localCacheSize = localCacheSize;
            this.type = type;
            this.handler = handler;
            this.msIoContainer = SpringUtils.getBean(MsIoContainer.class);
            this.pageSize = pageSize;
            translator();
        }

        private SimpleExcelBeanReverse(Map<Integer,List> data,OutExceptionHandler handler){
            this(data,false,true,500,ExcelDealType.XLSX,handler,65536);
        }

        /**
         * 私有导出到workbook
         */
        private void translator() throws DataUnCatchException{
            if(data == null || data.isEmpty()){
                throw new DataUnCatchException("未找到应有的数据集,若要制造模板，请传入一个空对象");
            }
            //如果标记为导出为XLS格式的，检查65536是否达到，达到默认翻页
            if(ExcelDealType.XLS.equals(type)){
                this.workbook = new HSSFWorkbook();
            }else{
                this.workbook = new SXSSFWorkbook(localCacheSize);
            }
            TreeSet<Integer> sortKey = Sets.newTreeSet(data.keySet());
            for (Integer pageNo : sortKey) {
                List list = data.get(pageNo);
                int ceil = (int)Math.ceil(list.size() * 1.0 / pageSize);
                //自动翻页操作，pageNo作为缓存的映射key传入
                if(1 == ceil){
                    writeToSheet(list,workbook.createSheet(),1);
                    continue;
                }
                for (int i = 0; i < ceil; i++) {
                    if(list.size() < (i + 1) * pageSize){
                        writeToSheet(list.subList(i * pageSize,list.size() - 1),workbook.createSheet(),ceil);
                        continue;
                    }
                    writeToSheet(list.subList(i*pageSize,(i + 1) * pageSize),workbook.createSheet(),ceil);
                }
            }
        }

        /**
         * 实际写操作，自动执行分页操作
         * @param data 数据列
         * @param sheet 输出列
         * @param pageNo 缓存映射提取列
         */
        @SuppressWarnings("unchecked")
        private void writeToSheet(List data,Sheet sheet,int pageNo) throws DataUnCatchException{
            if(data.isEmpty()) {
                return;
            }
            Object typeStandard = data.get(0);
            //map类型读取
            Row titleRow = sheet.createRow(0);
            //全局错误
            DataUnCatchException error = null;
            if(typeStandard instanceof Map){
                String key = msIoContainer.match(((Map) typeStandard).keySet(),true);
                mapKey.put(pageNo,key);
                LinkedHashMap<String, MsIoContainer.Information> mapping = msIoContainer.get(key);
                //命名字段，作为key顺序取值并插入
                List<String> keySet = Lists.newArrayList();
                Iterator<Map.Entry<String, MsIoContainer.Information>> iterator = mapping.entrySet().iterator();
                int cellIndexTitle = 0;
                while (iterator.hasNext()){
                    Map.Entry<String, MsIoContainer.Information> v = iterator.next();
                    Cell cell = titleRow.createCell(cellIndexTitle ++);
                    cell.setCellValue(v.getValue().getName());
                    keySet.add(v.getKey());
                }
                //标记外层循环
                int rowIndex = 1;
                out:
                for (Object map : data) {
                    Row rowTemp = sheet.createRow(rowIndex ++);
                    int cellIndex = 0;
                    for (String keyTemp : keySet) {
                        Object dataItem = ((Map) map).get(keyTemp);
                        MsIoContainer.Information action = mapping.get(keyTemp);
                        Object invoke = null;
                        Cell cellTemp = rowTemp.createCell(cellIndex ++);
                        try {
                            if(action.getMethod() != null) {
                                invoke = action.getMethod().invoke(action.getInvokeObject(), dataItem);
                            }else{
                                invoke = action.getOperator().dataOperator(dataItem);
                            }
                        }catch (IllegalAccessException | InvocationTargetException e) {
                            //未找到错误处理程序，跳出循环并抛出异常，结束方法
                            if (handler == null) {
                                error = new DataUnCatchException("第" + pageNo + "页，第" + rowIndex + "行，第" + cellIndex + "列Map数据无法转换",e);
                                break out;
                            }
                            log.error("第" + pageNo + "页，第" + rowIndex + "行，第" + cellIndex + "列Map数据无法转换", e);
                            //执行错误处理程序
                            invoke = handler.handle(e,dataItem);
                        }
                        //解析完数据进入表中
                        cellTemp.setCellValue(String.valueOf(invoke));
                    }
                }
                if(error != null){
                    throw error;
                }
            }else{
                Class<?> clazz = data.get(0).getClass();
                LinkedHashMap<String, MsIoContainer.Information> mapping = msIoContainer.get(clazz);
                //excel列表顺序,且对标题行赋值
                List<String> keySet = Lists.newArrayList();
                mapping.forEach((k,v) -> {
                    keySet.add(k);
                    titleRow.createCell(titleRow.getLastCellNum() + 1).setCellValue(v.getName());
                });
                out:
                for (Object obj : data) {
                    Row dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
                    for (String keyTemp : keySet) {
                        Object value = ClassUtils.getFieldValue(keyTemp, obj, clazz);
                        MsIoContainer.Information action = mapping.get(keyTemp);
                        Cell cellTemp = dataRow.createCell(dataRow.getLastCellNum() + 1);
                        Object invoke = null;
                        try {
                            invoke = action.getMethod().invoke(action.getInvokeObject(), value);
                        }catch (IllegalAccessException | InvocationTargetException e){
                            //未找到错误处理程序，跳出循环并抛出异常，结束方法
                            if (handler == null) {
                                error = new DataUnCatchException("第" + pageNo + "页，第" + dataRow.getRowNum() + "行，第" + cellTemp.getColumnIndex() +
                                        "列数据无法转换",e);
                                break out;
                            }
                            log.error("第" + pageNo + "页，第" + dataRow.getRowNum() + "行，第" + cellTemp.getColumnIndex() +
                                    "列类数据无法转换", e);
                            //执行错误处理程序
                            invoke = handler.handle(e,value);
                        }
                        cellTemp.setCellValue(String.valueOf(invoke));
                    }
                }
                if(error != null){
                    throw error;
                }
            }
            pageIndex ++;
        }
    }
    /**
     * 复杂excel实例单元
     * 复杂Excel：格式复杂，有合并单元格选项，需要筛选
     */
    public final class ComplexExcelBean {

    }


    public static IExcelBeanReverse getComplexExcelBeanReverseInstance(String id,List data,OutExceptionHandler handler){
        return new ComplexExcelBeanReverse(id,data,handler);
    }
    /**
     * 复杂excel实例单元导出
     */
    public final static class ComplexExcelBeanReverse implements IExcelBeanReverse{
        /**
         * 用户传入的数据，需要导出
         */
        private Map<Integer,List> data;
        /**
         * 导出之后产生的workbook，用于写
         */
        private Workbook workbook;
        /**
         * 标记是否集散多线程同时处理
         */
        private boolean asycSign;
        /**
         * 是否开启本地缓存，本地缓存默认开启，仅对XLSX有效
         */
        private boolean localCache;
        /**
         * 用于处理中途错误的问题
         */
        private OutExceptionHandler handler = null;
        /**
         * 开启本地缓存后，缓存的数量
         */
        private int localCacheSize;
        /**
         * 页码，存储当前页码指针位置
         */
        private int pageIndex = 0;
        /**
         * 每页数据最大承受值，如果达到了会自动进行翻页
         */
        private int pageSize;
        /**
         * 缓存的页码索引对象
         */
        private Map<Integer,String> mapKey;
        /**
         * 文件导出后的格式
         */
        private ExcelDealType type;

        private MsIoContainer msIoContainer;

        @Override
        public Workbook getWorkbook(){
            return this.workbook;
        }

        private ComplexExcelBeanReverse(boolean asycSign, boolean localCache, ExcelDealType type,Map<Integer,List> data,
                                       int localCacheSize, int pageSize, Map<Integer, String> mapKey, OutExceptionHandler handler) {
            this.data = data;
            this.asycSign = asycSign;
            this.localCache = localCache;
            this.handler = handler;
            this.localCacheSize = localCacheSize;
            this.pageSize = pageSize;
            this.mapKey = mapKey;
            this.type = type;
            this.msIoContainer = SpringUtils.getBean(MsIoContainer.class);
            translate();
        }


        private ComplexExcelBeanReverse(String id,List data,OutExceptionHandler handler){
            this(false, true, ExcelDealType.XLSX, ImmutableMap.of(0,data), 500, 65536, ImmutableMap.of(0,id),handler);
        }


        private void translate(){
            if(data == null || data.isEmpty()){
                throw new DataUnCatchException("未找到应有的数据集,若要制造模板，请传入一个空对象");
            }
            //如果标记为导出为XLS格式的，检查65536是否达到，达到默认翻页
            if(ExcelDealType.XLS.equals(type)){
                this.workbook = new HSSFWorkbook();
            }else{
                this.workbook = new SXSSFWorkbook(localCacheSize);
            }
            TreeSet<Integer> sortKey = Sets.newTreeSet(data.keySet());
            for (Integer pageNo : sortKey) {
                List list = data.get(pageNo);
                int ceil = (int)Math.ceil(list.size() * 1.0 / pageSize);
                //自动翻页操作，pageNo作为缓存的映射key传入
                if(1 == ceil){
                    writeToSheet(list,workbook.createSheet(),1,mapKey.get(pageNo));
                    continue;
                }
                for (int i = 0; i < ceil; i++) {
                    if(list.size() < (i + 1) * pageSize){
                        writeToSheet(list.subList(i * pageSize,list.size() - 1),workbook.createSheet(),ceil, mapKey.get(pageNo));
                        continue;
                    }
                    writeToSheet(list.subList(i*pageSize,(i + 1) * pageSize),workbook.createSheet(),ceil, mapKey.get(pageNo));
                }
            }
        }

        private void writeToSheet(List list, Sheet sheet, int ceil,String key){
            if(list.isEmpty()){
                return;
            }
            Object instance = list.get(0);
            //全局错误
            DataUnCatchException error = null;
            //解析行为
            if(instance instanceof Map){
                //获取深度
                int depthLevel = msIoContainer.getDepthLevel(key);
                //获取映射信息
                LinkedHashMap<String, MsIoContainer.Information> mapping = msIoContainer.get(key);
                //编写标题
                mapComplexTitle(mapping,depthLevel,0,0,sheet);
            }else{

            }
        }

        /**
         *
         * @param mapping 标题数据源
         * @param depthLevel 最下层
         * @param maxLevel 当前层，用于统计当前指针层数，从0开始向下延伸，叶子层一律用最下层作为lastRowNum，双亲层一般为单层
         * @param index 当前列，作用为指针，在递归过程中能够记录
         * @param sheet 页面
         * @return 数据
         */
        private int mapComplexTitle(LinkedHashMap<String, MsIoContainer.Information> mapping, int depthLevel,int maxLevel,int index, Sheet sheet){
            if(null == mapping || mapping.isEmpty()){
                return 1;
            }
            int count = 0;
            for (String key : mapping.keySet()) {
                MsIoContainer.Information information = mapping.get(key);
                //单项最低
                int itemCount = mapComplexTitle(information.getChildren(), depthLevel, maxLevel + 1, index + 1, sheet);
                //无子项
                if(1 == itemCount){
                    //合并叶子单元格
                    MsUtils.mergeAndCenteredCell(sheet,information.getName(),maxLevel,depthLevel-1,index,index,true,true);
                    //指针下滑计数
                    index += 1;
                    count ++;
                }else{
                    //合并双亲单元格
                    MsUtils.mergeAndCenteredCell(sheet,information.getName(),maxLevel,maxLevel,index,index + itemCount,true,true);
                    index += itemCount;
                    count += itemCount;
                }
            }
            return count;
        }
    }
}
