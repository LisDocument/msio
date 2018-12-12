package com.hellozq.msio.exception;

/**
 * @author bin
 * 格式不支持抛出的异常
 */
public class UnsupportFormatException extends Exception{

    public UnsupportFormatException(String tip){
        super(tip);
    }
}
