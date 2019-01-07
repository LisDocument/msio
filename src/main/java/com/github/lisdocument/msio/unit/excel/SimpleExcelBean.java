package com.github.lisdocument.msio.unit.excel;

/**
 * Created with IntelliJ IDEA.
 *
 * @author ThisLi(Bin)
 * @date 2019/1/3
 * time: 14:41
 * To change this template use File | Settings | File Templates.
 */

import com.github.lisdocument.msio.config.MsIoContainer;
import com.github.lisdocument.msio.exception.IndexOutOfSheetSizeException;
import com.github.lisdocument.msio.exception.UnsupportFormatException;
import com.github.lisdocument.msio.utils.ClassUtils;
import com.github.lisdocument.msio.utils.MsUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * 简单Excel导入功能
 * @author ThisLi(Bin)
 * time: 15:32
 * To change this template use File | Settings | File Templates.
 */
public final class SimpleExcelBean extends BaseExcelBean {

    private static final Logger log = LoggerFactory.getLogger(SimpleExcelBean.class);

    /**
     * 是否根据自动更新Class
     */
    private boolean isChangeClass = true;

    private List<String> idPool;

    private Class<?> clazz;

    private String id;

    private SimpleExcelBean(@NotNull MultipartFile file){
        super(file,true);
    }

    private SimpleExcelBean(@NotNull File file){
        super(file,true);
    }

    /**
     * 单页初始化
     * @param id 指派导出类型，为null则自行查询
     * @param file 文件流
     * @param pageIndex 单页码，页码
     */
    SimpleExcelBean(String id, @NotNull MultipartFile file,@NotNull Integer pageIndex){
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
     * 单页初始化
     * @param id 指派导出类型，为null则自行查询
     * @param file 文件流
     * @param pageIndex 单页码，页码
     */
    SimpleExcelBean(String id,@NotNull File file,@NotNull Integer pageIndex){
        this(file);
        this.isTuring = false;
        if(StringUtils.isEmpty(id)){
            this.isChangeClass = true;
        }else {
            this.id = id;
            this.clazz = msIoContainer.getClazz(id);
            this.isChangeClass = false;
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
    SimpleExcelBean(@NotNull List<String> idPool,@NotNull MultipartFile file,boolean isChangeClass){
        this(file);
        this.isTuring = true;
        this.isChangeClass = isChangeClass;
        this.idPool = idPool;
        automaticPageTurningWithMapping();
    }

    /**
     * 多页指定每页的id并初始化
     * @param idPool 每页的id池，会根据页码去索引，请根据顺序给定
     * @param file 文件流
     * @param isChangeClass 是否自动去寻找类，若设置为false则会省略当前无映射的页
     */
    SimpleExcelBean(@NotNull List<String> idPool,@NotNull File file,boolean isChangeClass){
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
    SimpleExcelBean(@NotNull MultipartFile file,@NotNull boolean isChangeClass){
        this(file);
        this.isTuring = true;
        this.isChangeClass = true;
        automaticPageTurningWithoutMapping();
    }

    /**
     * 多页不指定每页的id初始化
     * @param file 文件流
     * @param isChangeClass 若没有指定的class对象是否进行自动寻找
     */
    SimpleExcelBean(@NotNull File file,@NotNull boolean isChangeClass){
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
        List<String> titles = MsUtils.getRowDataInString(rowIndex ++, 0, 0, sheetNow);
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
     * @throws NoSuchMethodException 找不到对应的方法
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
            MsIoContainer.Information information = auto.get(egTitle);
            if(information.getFieldType() == String.class){
                ClassUtils.setFieldValue(value, egTitle, obj, clazz);
                //倘若导入目标为集合的情况
            }else if(List.class.isAssignableFrom(information.getFieldType())){
                String simpleName = "fromStringtoListBy" + information.getFieldType().getSimpleName();
                Object invoke;
                try {
                    invoke = ClassUtils.invokeMethod(formatConversion, simpleName, value);
                }catch (IllegalArgumentException e){
                    log.error("尝试使用" + simpleName + "获取方法失败，正在尝试使用全名获取");
                    String flexName = "fromStringtoSetBy" + information.getFieldType().getName().replaceAll(".", "");
                    try {
                        invoke = ClassUtils.invokeMethod(formatConversion, flexName, value);
                    }catch (IllegalArgumentException e1){
                        log.error("尝试使用" + flexName + "获取方法失败，抛出异常，请检查是否存在方法或者方法是否设置为non-private");
                        throw new NoSuchMethodException("无法找到方法" + simpleName + "、" + flexName);
                    }
                }
                ClassUtils.setFieldValue(value, egTitle, obj, clazz);
            }else if(Set.class.isAssignableFrom(information.getFieldType())){
                String simpleName = "fromStringtoSetBy" + information.getFieldType().getSimpleName();
                Object invoke;
                try {
                    invoke = ClassUtils.invokeMethod(formatConversion, simpleName, value);
                }catch (IllegalArgumentException e){
                    log.error("尝试使用" + simpleName + "获取方法失败，正在尝试使用全名获取");
                    String flexName = "fromStringtoSetBy" + information.getFieldType().getName().replaceAll(".", "");
                    try {
                        invoke = ClassUtils.invokeMethod(formatConversion, flexName, value);
                    }catch (IllegalArgumentException e1){
                        log.error("尝试使用" + flexName + "获取方法失败，抛出异常，请检查是否存在方法或者方法是否设置为non-private");
                        throw new NoSuchMethodException("无法找到方法" + simpleName + "、" + flexName);
                    }
                }
                ClassUtils.setFieldValue(value, egTitle, obj, clazz);
            }else{
                String simpleName = "fromStringto" + information.getFieldType().getSimpleName();
                Object invoke;
                try {
                    invoke = ClassUtils.invokeMethod(formatConversion, simpleName, value);
                }catch (IllegalArgumentException e){
                    log.error("尝试使用" + simpleName + "获取方法失败，正在尝试使用全名获取");
                    String flexName = "fromStringto" + information.getFieldType().getName().replaceAll(".", "");
                    try {
                        invoke = ClassUtils.invokeMethod(formatConversion, flexName, value);
                    }catch (IllegalArgumentException e1){
                        throw new NoSuchMethodException("尝试使用" + flexName + "获取方法失败，抛出异常，请检查是否存在方法或者方法是否设置为non-private");
                    }
                }
                ClassUtils.setFieldValue(invoke, egTitle, obj, clazz);
            }
        }
        return obj;
    }
}
