package com.hellozq.msio.exception;

/**
 * 为处理该有数据的地方没有数据抛出的异常
 * @author bin
 * @date 2018年10月29日21:04:51
 */
public class DataUnCatchException extends IllegalArgumentException {


    private Exception e;

    public DataUnCatchException(String tip,Exception e){
        super(tip);
        this.e = e;
    }

    public DataUnCatchException(String tip){
        super(tip);
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
        if(e != null) {
            e.printStackTrace();
        }
    }
}
