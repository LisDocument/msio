package com.hellozq.msio.anno;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author bin
 * 自动注入数据的方法
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@MsItem
@SuppressWarnings("unused")
public @interface MsAutomatic {

    @AliasFor(
            annotation = MsItem.class
    )
    String value() default "";
    /**
     *
     * @return 是否自动生成当前时间,该为true的情况只能修改Date类型的数据
     */
    boolean isUseNowDate() default false;

    /**
     *
     * @return 自动生成UUID,只能修饰String类型的数据，否则会报错
     */
    boolean isUseUUID() default false;

    /**
     * isUseNowDate/isUseUUID优先级更高
     * @return 自动赋值，自动进行格式转换，能接受时间或者数字类型等基本类型的转换，不支持复杂类型
     */
    String defaultValue() default "";

}
