package com.github.lisdocument.msio.bean.common.impl;

import com.github.lisdocument.msio.bean.common.Operator;

/**
 * @author bin
 * 默认的操作方法，没有意义，仅仅作为填补默认数据使用
 */
public class DefaultOperator implements Operator {

    @Override
    public Object dataOperator(Object obj) {
        return obj;
    }
}
