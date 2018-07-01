package com.hellozq.msio.bean.common;

/**
 * @author bin
 * 操作者，用于处理数据导出的时候发生的数据转换问题
 * 数据转换者
 */
public interface Operator {

    /**
     * 操作方法，将数据导出的时候自动打入方法
     * @param obj 输出的原本的数据
     * @return 修正之后输出的数据
     */
    Object dataOperator(Object obj);
}
