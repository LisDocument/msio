package com.hellozq.msio.anno;

import com.hellozq.msio.bean.common.Operator;
import com.hellozq.msio.bean.common.impl.DefaultOperator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author bin
 * 方法操作单体，用于操作某个数据值
 * @see MsOperator
 * 仅用于修饰被MsOperator修饰的类
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface MsItem {

    String value();

    String methodName() default "";

    Class<? extends Operator> transFormOperator() default DefaultOperator.class;
}
