package com.github.lisdocument.msio.bean.common;

/**
 * @author bin
 * @see ITransFunctionContainer
 * 操作者，用于处理数据导出的时候发生的数据转换问题
 * 数据转换者
 * 不建议使用，建议使用公共操作类
 */
@SuppressWarnings("unused")
public interface Operator {

    /**
     * 操作方法，将数据导出的时候自动打入方法
     * @param obj 输出的原本的数据
     * @return 修正之后输出的数据
     */
    Object dataOperator(Object obj);
}
