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
@Target({ElementType.FIELD,ElementType.ANNOTATION_TYPE})
public @interface MsItem {

    /**
     *
     * @return 对应的中文名称映射
     */
    String value() default "";

    /**
     *
     * @return 方法名，后面描述的导出方法容器中相应的方法名
     */
    String methodName() default "";

    /**
     *
     * @return 操作类，拾取方法的缓存方法池
     */
    Class<? extends Operator> transFormOperator() default DefaultOperator.class;
}
