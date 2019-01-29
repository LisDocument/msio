package com.github.lisdocument.msio.unit.excel;

import com.github.lisdocument.msio.unit.func.OutExceptionHandler;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.github.lisdocument.msio.config.MsIoContainer;
import com.github.lisdocument.msio.exception.DataUnCatchException;
import com.github.lisdocument.msio.utils.ClassUtils;
import com.github.lisdocument.msio.utils.MsUtils;
import com.github.lisdocument.msio.utils.StringRegexUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Created with IntelliJ IDEA.
 * 复杂Excel导出
 * @author ThisLi(Bin)
 * time: 15:36
 * To change this template use File | Settings | File Templates.
 */
public final class ComplexExcelBeanReverse extends BaseExcelBeanReverse{

    private static final Logger log = LoggerFactory.getLogger(ComplexExcelBeanReverse.class);

    @Override
    public Workbook getWorkbook(){
        return this.workbook;
    }

    /**
     * 全参构造
     * @param data 代转换数据
     * @param asycSign 是否开启多线程共同输出
     * @param localCache 本地缓存
     * @param localCacheSize 本地缓存大小
     * @param type 导出种类
     * @param pageSize 每页显示记录数
     * @param handler 错误操作
     * @param mapKey 映射id与页码的对应关系
     */
    ComplexExcelBeanReverse(Map<Integer,List> data,boolean asycSign, boolean localCache, ExcelFactory.ExcelDealType type,
                                    int localCacheSize, int pageSize, Map<Integer, String> mapKey, OutExceptionHandler handler) {
        super(data, asycSign, localCache, handler, localCacheSize, pageSize, type, mapKey);
    }

    ComplexExcelBeanReverse(Map<Integer,String> ids,Map<Integer,List> data,OutExceptionHandler handler){
        this(data,false, true, ExcelFactory.ExcelDealType.XLSX, 500, 65536, ids,handler);
    }

    ComplexExcelBeanReverse(String id,List data,OutExceptionHandler handler){
        this(ImmutableMap.of(0,data),false, true, ExcelFactory.ExcelDealType.XLSX, 500, 65536, ImmutableMap.of(0,id),handler);
    }

    ComplexExcelBeanReverse(String id, List data, ExcelFactory.ExcelDealType type,OutExceptionHandler handler){
        this(ImmutableMap.of(0,data),false, true, type, 500, 65536, ImmutableMap.of(0,id),handler);
    }

    @Override
    void translator(){
        if(data == null || data.isEmpty()){
            throw new DataUnCatchException("未找到应有的数据集,若要制造模板，请传入一个空对象");
        }
        //如果标记为导出为XLS格式的，检查65536是否达到，达到默认翻页
        if(ExcelFactory.ExcelDealType.XLS.equals(type)){
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
                writeToSheet(list,workbook.createSheet(),1,mapKey.get(pageNo),pageNo);
                continue;
            }
            for (int i = 0; i < ceil; i++) {
                if(list.size() < (i + 1) * pageSize){
                    writeToSheet(list.subList(i * pageSize,list.size() - 1),workbook.createSheet(),ceil, mapKey.get(pageNo),pageNo);
                    continue;
                }
                writeToSheet(list.subList(i*pageSize,(i + 1) * pageSize),workbook.createSheet(),ceil, mapKey.get(pageNo),pageNo);
            }
        }
    }

    private void writeToSheet(List list, Sheet sheet, int ceil, String key, Integer pageNo){
        if(list.isEmpty()){
            return;
        }
        Object instance = list.get(0);
        //全局错误
        DataUnCatchException error = null;
        //总行数和总列数
        int columnSize = 0;
        int rowSize = 0;
        //解析行为
        if(instance instanceof Map){
            //获取深度
            int depthLevel = msIoContainer.getDepthLevel(key);
            //获取映射信息
            LinkedHashMap<String, MsIoContainer.Information> mapping = msIoContainer.get(key);
            //编写标题
            LinkedHashMap<String, MsIoContainer.Information> titles = Maps.newLinkedHashMapWithExpectedSize(32);
            mapComplexTitle(mapping,depthLevel,0,0,sheet,titles);
            //开始填入内容，map无层级关系，以最低级得叶子节点为主
            //行指针定义,行从0开始因此可以直接使用depthLevel
            int rowIndex = depthLevel;
            out:
            for (Object map : list) {
                Row rowTemp = sheet.createRow(rowIndex ++);
                int cellIndex = 0;
                for (String keyTemp : titles.keySet()) {
                    Object dataItem = ((Map) map).get(keyTemp);
                    MsIoContainer.Information action = titles.get(keyTemp);
                    Object invoke;
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
                            error = new DataUnCatchException("复杂Excel导出，第" + pageNo + "组，第" + rowIndex + "行，第" + cellIndex + "列Map数据无法转换",e);
                            break out;
                        }
                        log.error("复杂Excel导出，第" + pageNo + "组，第" + rowIndex + "组，第" + cellIndex + "列Map数据无法转换", e);
                        //执行错误处理程序
                        invoke = handler.handle(e,dataItem);
                    }
                    //解析完数据进入表中
                    cellTemp.setCellValue(StringRegexUtils.getOrDefault(invoke,""));
                }
            }
            rowSize = rowIndex;
            columnSize = titles.size();
            if(error != null){
                throw error;
            }
            //类的转换
        }else{
            Class<?> clazz = instance.getClass();
            LinkedHashMap<String, MsIoContainer.Information> mapping = msIoContainer.get(clazz);
            int depthLevel = msIoContainer.getDepthLevel(clazz);
            //这个title无效，pojo类产生的title，会因为英文相同导致被刷新
            LinkedHashMap<String, MsIoContainer.Information> titles = Maps.newLinkedHashMapWithExpectedSize(16);
            columnSize = mapComplexTitle(mapping,depthLevel,0,0,sheet,titles);
            //遍历存储数据
            for (Object obj : list) {
                Row dataRow = sheet.createRow(sheet.getLastRowNum() + 1);
                complexRowDataByPojo(obj,obj.getClass(),mapping,dataRow,0,pageIndex);
            }
            rowSize = depthLevel + list.size();
            //columnSize = titles.size();
        }
        try {
            MsUtils.sheetFix(sheet,columnSize,rowSize);
        } catch (UnsupportedEncodingException e) {
            log.error("格式整理失败，返回未整理前的模板",e);
        }
        pageIndex ++;
    }

    /**
     * 提取复杂pojo类中的数据
     * @param o pojo对象
     * @param clazz 当前解析类的字节码对象
     * @param mapping 映射关系
     * @param row 数据转储行
     * @param index 列偏移指针
     * @param pageNo 当前映射页标识
     * @return 列当前指针位置
     */
    private int complexRowDataByPojo(Object o,Class<?> clazz, LinkedHashMap<String, MsIoContainer.Information> mapping,Row row,int index,int pageNo){
        DataUnCatchException error = null;
        if(null == o){
            try {
                o = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        for (String key : mapping.keySet()) {
            Object fieldValue = ClassUtils.getFieldValue(key, o, clazz);
            MsIoContainer.Information v = mapping.get(key);
            if(null != v.getChildren() && !v.getChildren().isEmpty()){
                //子项
                index = complexRowDataByPojo(fieldValue,v.getFieldType(),v.getChildren(),row,index,pageNo);
            }else{
                //无子项直接打印
                Cell cell = row.createCell(index ++);
                try {
                    //反射的数据继续接受后期处理
                    if(null != v.getMethod()) {
                        fieldValue = v.getMethod().invoke(v.getInvokeObject(), fieldValue);
                    }else{
                        fieldValue = v.getOperator().dataOperator(fieldValue);
                    }
                } catch (IllegalAccessException|InvocationTargetException e) {
                    log.error("第" + pageNo + "组，第" + row.getRowNum() + "行，第" + cell.getColumnIndex() +
                            "列类数据无法转换", e);
                    error = new DataUnCatchException("第" + pageNo + "组，第" + row.getRowNum() + "行，第" + cell.getColumnIndex() +
                            "列类数据无法转换", e);
                    fieldValue = handler.handle(e,fieldValue);
                }
                cell.setCellValue(StringRegexUtils.getOrDefault(fieldValue,""));
            }
            if(null != error){
                throw error;
            }
        }
        return index;
    }

    /**
     *
     * @param mapping 标题数据源
     * @param depthLevel 最下层
     * @param maxLevel 当前层，用于统计当前指针层数，从0开始向下延伸，叶子层一律用最下层作为lastRowNum，双亲层一般为单层
     * @param index 当前列，作用为指针，在递归过程中能够记录
     * @param sheet 页面
     * @param titles 标题提取变量，使用地址传递的特性进行处理
     * @return 数据
     */
    private int mapComplexTitle(LinkedHashMap<String, MsIoContainer.Information> mapping, int depthLevel, int maxLevel, int index, Sheet sheet, LinkedHashMap<String, MsIoContainer.Information> titles){
        if(null == mapping || mapping.isEmpty()){
            return 1;
        }
        int count = 0;
        for (String key : mapping.keySet()) {
            MsIoContainer.Information information = mapping.get(key);
            //单项最低
            int itemCount = mapComplexTitle(information.getChildren(), depthLevel, maxLevel + 1, index, sheet,titles);
            //无子项
            if(1 == itemCount){
                //合并叶子单元格
                MsUtils.mergeAndCenteredCell(sheet,information.getName(),maxLevel,depthLevel-1,index,index,true,true);
                //指针下滑计数
                index += 1;
                count ++;
                titles.put(key,information);
            }else{
                //合并双亲单元格
                MsUtils.mergeAndCenteredCell(sheet,information.getName(),maxLevel,maxLevel,index,index + itemCount-1,true,true);
                index += itemCount;
                count += itemCount;
            }
        }
        return count;
    }
}