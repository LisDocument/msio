package com.github.lisdocument.msio.utils;

/**
 * @author bin
 * 用于部分数据处理方法的撰写
 */
@SuppressWarnings("unused")
public class StringRegexUtils {

    private static final String TRANSLATION_SIGN = "\\";
    /**
     * 用于监测配置文件中的方法配置
     * @param detected 待监测是否包含sign的String串
     * @param sign 监测字段
     * @return 该sign的开头位置，如果未找到则返回-1
     */
    public static int checkIsContain(String detected, String sign){
        String detectedModel = new String(detected);
        String tSign = TRANSLATION_SIGN + sign;
        //含转移字符的数据
        int i = detectedModel.lastIndexOf(tSign);
        if(i != -1) {
            StringBuilder sb = new StringBuilder();
            for (int i1 = 0; i1 < tSign.length(); i1++) {
                sb.append(" ");
            }
            while (detectedModel.contains(tSign)) {
                detectedModel = detectedModel.replace(tSign, sb.toString());
            }
        }
        return detectedModel.indexOf(sign);
    }

    /**
     * 提取默认值
     * @param obj 需要判断是否空的对象
     * @param defaultValue 如果是空返回的String类型的对象
     * @return 处理后的数据
     */
    public static String getOrDefault(Object obj, String defaultValue){
        if(null == obj || "".equals(obj)){
            return defaultValue;
        }
        return obj.toString();
    }

//    public static void main(String[] args){
//        System.out.println("ssss"+null);
//    }
}
