package com.hellozq.msio.anno;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 扫描的包名，注解在创世类中
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface MsPackageScan {

    @AliasFor("value")
    String[] packageName() default {};

    @AliasFor("packageName")
    String[] value() default {};
}
