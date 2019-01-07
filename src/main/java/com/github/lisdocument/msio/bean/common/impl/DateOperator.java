package com.github.lisdocument.msio.bean.common.impl;

import com.github.lisdocument.msio.bean.common.Operator;
import com.github.lisdocument.msio.bean.common.CommonBean;

import java.util.Date;

/**
 * @author bin
 * 日期转换默认方式，按照普通格式通用格式输出，舍弃毫秒属性
 */
public class DateOperator implements Operator {

    @Override
    public Object dataOperator(Object obj) {
        if(obj instanceof Date){
            return CommonBean.SIMPLE_DATE_FORMAT.format(obj);
        }
        return "DATA INITIAL FAILD";
    }
}
