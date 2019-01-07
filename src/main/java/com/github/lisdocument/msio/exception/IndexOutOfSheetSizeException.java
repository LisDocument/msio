package com.github.lisdocument.msio.exception;

/**
 * @author bin
 * 索引的长度超过最大长度报错
 */
public class IndexOutOfSheetSizeException extends Exception {

    public IndexOutOfSheetSizeException(String tip){
        super(tip);
    }
}
