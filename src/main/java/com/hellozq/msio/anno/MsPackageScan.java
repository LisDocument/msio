package com.hellozq.msio.anno;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@SuppressWarnings("unused")
public @interface MsPackageScan {

    @AliasFor("value")
    String[] packageName() default {};

    @AliasFor("packageName")
    String[] value() default {};
}
