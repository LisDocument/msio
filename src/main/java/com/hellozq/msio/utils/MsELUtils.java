package com.hellozq.msio.utils;

import com.hellozq.msio.exception.MsELUnsupportException;

import java.util.LinkedList;

public class MsELUtils {

    //方法调用
    public static final String FUNC_SIGN = "#";

    public static final String TYPE_INTEGER = "$int$";

    public static final String TYPE_DOUBLE = "$double$";

    public static final String TYPE_STRING = "$string$";

    public static Object getValueByEL(Object obj,String el) throws MsELUnsupportException{
        LinkedList<String> methodStack = new LinkedList<>();
        String[] split = el.split(FUNC_SIGN);
        for (String s : split) {
            if(!"".equals(s)) {
                methodStack.addLast(s);
            }
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
        if("".equals(args)) {
            return ClassUtils.invokeMethod(obj, split[0]);
        }else{
            String[] ss = args.split(",");
            Object[] objArgs = new Object[ss.length];
            for (int i = 0; i < ss.length; i++) {
                if(!ss[i].contains("$")){
                    objArgs[i] = ss[i];
                }else{
                    if(ss[i].contains(TYPE_INTEGER)){
                        objArgs[i] = Integer.parseInt(ss[i].substring(5));
                    }else if(ss[i].contains(TYPE_DOUBLE)){
                        objArgs[i] = Double.parseDouble(ss[i].substring(8));
                    }else if(ss[i].contains(TYPE_STRING)){
                        objArgs[i] = ss[i].substring(8);
                    }
                }
            }
            return ClassUtils.invokeMethod(obj, split[0], objArgs);
        }
    }
}
