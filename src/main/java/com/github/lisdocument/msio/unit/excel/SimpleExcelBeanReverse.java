package com.github.lisdocument.msio.unit.excel;

import com.github.lisdocument.msio.unit.func.OutExceptionHandler;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.github.lisdocument.msio.config.MsIoContainer;
import com.github.lisdocument.msio.exception.DataUnCatchException;
import com.github.lisdocument.msio.utils.ClassUtils;
import com.github.lisdocument.msio.utils.MsUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * 简单Excel导出功能
 * @author ThisLi(Bin)
 * time: 15:32
 * To change this template use File | Settings | File Templates.
 */
public final class SimpleExcelBeanReverse extends BaseExcelBeanReverse{

    private static final Logger log = LoggerFactory.getLogger(SimpleExcelBeanReverse.class);

    /**
     * 全参构造
     * @param data 代转换数据
     * @param asycSign 是否开启多线程共同输出
     * @param localCache 本地缓存
     * @param localCacheSize 本地缓存大小
     * @param type 导出种类
     * @param pageSize 每页显示记录数
     * @param handler 错误操作
     * @param mapKey 每页对应的映射id
     */
    SimpleExcelBeanReverse(Map<Integer,List> data, boolean asycSign, boolean localCache
            , int localCacheSize, ExcelFactory.ExcelDealType type, int pageSize,Map<Integer,String> mapKey, OutExceptionHandler handler){
        super(data, asycSign, localCache, handler, localCacheSize, pageSize, type, mapKey);
    }

    SimpleExcelBeanReverse(Map<Integer,List> data,OutExceptionHandler handler){
        this(data,false,true,500, ExcelFactory.ExcelDealType.XLSX,65536,Maps.newHashMapWithExpectedSize(64), handler);
    }

    SimpleExcelBeanReverse(List data,OutExceptionHandler handler){
        this(ImmutableMap.of(1,data),false,true,500, ExcelFactory.ExcelDealType.XLSX,65536,Maps.newHashMapWithExpectedSize(64), handler);
    }

    SimpleExcelBeanReverse(List data, ExcelFactory.ExcelDealType type,OutExceptionHandler handler){
        this(ImmutableMap.of(1,data),false,true,500, type,65536,Maps.newHashMapWithExpectedSize(64), handler);
    }

    /**
     * 私有导出到workbook
     */
    @Override
    void translator() throws DataUnCatchException {
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
    private void writeToSheet(List data, Sheet sheet, int pageNo) throws DataUnCatchException{
        if(data.isEmpty()) {
            return;
        }
        Object typeStandard = data.get(0);
        //map类型读取
        Row titleRow = sheet.createRow(0);
        //全局错误
        DataUnCatchException error = null;
        //全局总行列数
        int columnSize = 0;
        int rowSize = 0;
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
                            error = new DataUnCatchException("简单Excel导出，第" + pageNo + "组，第" + rowIndex + "行，第" + cellIndex + "列Map数据无法转换",e);
                            break out;
                        }
                        log.error("简单Excel导出，第" + pageNo + "组，第" + rowIndex + "组，第" + cellIndex + "列Map数据无法转换", e);
                        //执行错误处理程序
                        invoke = handler.handle(e,dataItem);
                    }
                    //解析完数据进入表中
                    cellTemp.setCellValue(String.valueOf(invoke));
                }
            }
            rowSize = rowIndex;
            columnSize = mapping.size();
            if(error != null){
                throw error;
            }
        }else{
            Class<?> clazz = data.get(0).getClass();
            LinkedHashMap<String, MsIoContainer.Information> mapping = msIoContainer.get(clazz);
            int rowIndex = 1;
            //excel列表顺序,且对标题行赋值
            List<String> keySet = Lists.newArrayList();
            int index = 0;
            for (String k : mapping.keySet()) {
                keySet.add(k);
                titleRow.createCell(index ++).setCellValue(mapping.get(k).getName());
            }
            out:
            for (Object obj : data) {
                Row dataRow = sheet.createRow(rowIndex ++);
                int cellIndex = 0;
                for (String keyTemp : keySet) {
                    Object value = ClassUtils.getFieldValue(keyTemp, obj, clazz);
                    MsIoContainer.Information action = mapping.get(keyTemp);
                    Cell cellTemp = dataRow.createCell(cellIndex ++);
                    Object invoke;
                    try {
                        if(null != action.getMethod()){
                            invoke = action.getMethod().invoke(action.getInvokeObject(), value);
                        }else {
                            invoke = action.getOperator().dataOperator(value);
                        }
                    }catch (IllegalAccessException | InvocationTargetException e){
                        //未找到错误处理程序，跳出循环并抛出异常，结束方法
                        if (handler == null) {
                            error = new DataUnCatchException("第" + pageNo + "组，第" + rowIndex + "行，第" + cellIndex +
                                    "列数据无法转换",e);
                            break out;
                        }
                        log.error("第" + pageNo + "组，第" + rowIndex + "行，第" + cellIndex +
                                "列类数据无法转换", e);
                        //执行错误处理程序
                        invoke = handler.handle(e,value);
                    }
                    cellTemp.setCellValue(String.valueOf(invoke));
                }
            }
            rowSize = rowIndex;
            columnSize = mapping.size();
            if(error != null){
                throw error;
            }
        }
        try {
            MsUtils.sheetFix(sheet,columnSize,rowSize);
        } catch (UnsupportedEncodingException e) {
            log.error("格式整理失败，按照原先格式进行导出",e);
        }
        pageIndex ++;
    }
}