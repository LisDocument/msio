package com.hellozq.msio.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author bin
 * 方法操作对象，用于修饰pojo对象，pojo对象必须有getset
 * 仅能修饰领域模型，标识其为控制的数据结构
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@SuppressWarnings("unused")
public @interface MsOperator {

    /**
     * 表名
     * @return 表名
     */
    String tableName() default "";

    /**
     *
     * @return 系统中唯一标示项
     */
    String value();

}
