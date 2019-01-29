package com.github.lisdocument.msio.anno;

import com.github.lisdocument.msio.unit.excel.ExcelFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来处理相关格式
 * @author Libin
 * @version 1.0.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MsReturnTranslator {

    /**
     * #表示返回的参数.表示调用方法，#.toString()就是调用返回参数的toString方法，点可连续使用
     * 作为EL表达式，将一些类型不正确的参数改正为正常List数据，例如Page变为List，或者返回是Map，仅需要部分数据
     * @return 注释在方法上，协同RequestMapping使用
     */
    String value() default "";

    /**
     * 标识Map对应的映射id，标记后会自动使用get方法获取，加快效率
     * @return id集
     */
    String[] id() default "";

    /**
     * 是否启用复杂解析器解析为复杂excel
     * @return 默认为不启动
     */
    boolean isComplex() default false;

    /**
     * 文件名，不包括后缀，后缀会随着文件格式自动更改
     * @return 文件名
     */
    String fileName() default "download";

    /**
     * 文件格式
     * @return 文件格式
     */
    ExcelFactory.ExcelDealType type() default ExcelFactory.ExcelDealType.XLSX;
}
