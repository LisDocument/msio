package com.github.lisdocument.msio.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类转换List容器，为了防止多配置的情况
 * 特定在Pojo中使用，防止过多重复引起的重复工作
 * @author Libin
 * @version 1.0.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface MsTranslateOperator {

    /**
     * 简单的EL表达式实体
     * @return EL表达式
     */
    String value() default "";
}
