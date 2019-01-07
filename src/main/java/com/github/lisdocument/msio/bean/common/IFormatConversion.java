package com.github.lisdocument.msio.bean.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author bin
 * 格式转换方式，默认提供部分数据方式
 * 导入时使用
 */
@SuppressWarnings("unused")
public interface IFormatConversion {

    Log log = LogFactory.getLog(IFormatConversion.class);

    /**
     * 提供String类型与Date类型的格式化切换
     * @param data 待转换的数据格式
     * @return 返回数据类型
     */
    default Date fromStringtoDate(String data){
        if(StringUtils.isEmpty(data)){
            return null;
        }
        //备选格式
        SimpleDateFormat[] alternative = new SimpleDateFormat[]{
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                new SimpleDateFormat("yyyy-MM-dd"),
                new SimpleDateFormat("yyyy-MM-dd HH:mm"),
                new SimpleDateFormat("yyyy-MM-dd HH"),
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"),
                new SimpleDateFormat("yyyy/MM/dd"),
                new SimpleDateFormat("yyyy/MM/dd HH:mm"),
                new SimpleDateFormat("yyyy/MM/dd HH"),
                new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒"),
                new SimpleDateFormat("yyyy年MM月dd日 HH时"),
                new SimpleDateFormat("yyyy年MM月dd日"),
                new SimpleDateFormat("yyyy年MM月")
        };
        for (SimpleDateFormat sm : alternative) {
            try {
                return sm.parse(data);
            } catch (ParseException e) {
                log.error("尝试匹配日期格式，匹配失败");
            }
        }
        return new Date(0L);
    }

    /**
     * 提供String类型转换为Long类型的方法
     * @param data 待转化的数据
     * @return 转换后的数据
     */
    default Long fromStringtoLong(String data){
        if(StringUtils.isEmpty(data)){
            return null;
        }
        try {
            return Long.parseLong(data);
        }catch (NumberFormatException e){
            log.error("该字符串非数字无法转换");
            e.printStackTrace();
        }
        return 0L;
    }

    /**
     * 提供String类型转换为Integer类型的方法
     * @param data 待转化的数据
     * @return 转换后的数据
     */
    default Integer fromStringtoInteger(String data){
        if(StringUtils.isEmpty(data)){
            return null;
        }
        try {
            return Integer.parseInt(data);
        }catch (NumberFormatException e){
            log.error("该字符串非数字无法转换");
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 提供String类型转换为Double类型的方法
     * @param data 待转换的数据
     * @return 转换后的数据
     */
    default Double fromStringtoDouble(String data){
        if(StringUtils.isEmpty(data)){
            return null;
        }
        try{
            return Double.parseDouble(data);
        }catch (NumberFormatException e){
            log.error("该字符串非数字无法转换");
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * 提供String类型转换为Float类型的方法
     * @param data 待转换的数据
     * @return 转换后的数据
     */
    default Float fromStringtoFloat(String data){
        if(StringUtils.isEmpty(data)){
            return null;
        }
        try{
            return Float.parseFloat(data);
        }catch (NumberFormatException e){
            log.error("该字符串非数字无法转换");
            e.printStackTrace();
        }
        return 0.0f;
    }

    /**
     * 提供String类型转换为Float类型的方法
     * @param data 待转换的数据
     * @return 转换后的数据
     */
    default Boolean fromStringtoBoolean(String data){
        if(StringUtils.isEmpty(data)){
            return null;
        }
        try{
            return Boolean.valueOf(data);
        }catch (NumberFormatException e){
            log.error("该字符串非数字无法转换");
            e.printStackTrace();
        }
        return Boolean.TRUE;
    }
}
