package com.hellozq.msio.utils;

import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;

public class MsELUtils {

    //方法调用
    public static final String FUNC_SIGN = ".";
    //本体调用
    public static final String FUNC_IDENTITY = "#";

    public static Object getValueByEL(Object obj,String el){
        Queue<String> methodStack = new LinkedBlockingQueue<>();
        String[] split = el.split(FUNC_SIGN);
        for (String s : split) {
            methodStack.add(s);
        }

    }

    public static Object valueCalculator(Object obj,String methodInfo){
        String[] split = methodInfo.split("\\(");
        if(split.length != 2){

        }
    }
}
