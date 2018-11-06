package com.hellozq.msio.exception;

/**
 * 为处理该有数据的地方没有数据抛出的异常
 * @author bin
 * @date 2018年10月29日21:04:51
 */
public class DataUnCatchException extends IllegalArgumentException {

    public DataUnCatchException(String tip){
        super(tip);
    }
}
