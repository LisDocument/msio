package com.hellozq.msio.anno;

import org.springframework.beans.factory.annotation.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: Libin
 * @Description: 用来处理相关格式
 * @Date: 11:01 2018/11/11
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MsReturnTranslator {

    /**
     * #表示返回的参数.表示调用方法，#.toString()就是调用返回参数的toString方法，点可连续使用
     * 作为EL表达式，将一些类型不正确的参数改正为正常List数据，例如Page变为List，或者返回是Map，仅需要部分数据
     * @return 注释在方法上，协同RequestMapping使用
     */
    String value();
}
