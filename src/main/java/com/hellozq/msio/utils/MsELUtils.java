package com.hellozq.msio.utils;

import com.hellozq.msio.exception.MsELUnsupportException;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;

public class MsELUtils {

    //方法调用
    public static final String FUNC_SIGN = ".";
    //本体调用
    public static final String FUNC_IDENTITY = "#";

    public static Object getValueByEL(Object obj,String el) throws MsELUnsupportException{
        LinkedList<String> methodStack = new LinkedList<>();
        String[] split = el.split(FUNC_SIGN);
        for (String s : split) {
            methodStack.addLast(s);
        }
        Object result = obj;
        for (String methodName: methodStack) {
            result = valueCalculator(result,methodName);
        }

        return result;
    }

    public static Object valueCalculator(Object obj,String methodInfo) throws MsELUnsupportException{
        String[] split = methodInfo.split("\\(");
        if(split.length != 2){
            throw new MsELUnsupportException("EL表达式格式异常");
        }
        String args = split[1].substring(0,split[1].length() - 1);

        return ClassUtils.invokeMethod(obj,split[0],args.split(","));
    }
}
