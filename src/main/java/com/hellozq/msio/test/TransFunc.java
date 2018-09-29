package com.hellozq.msio.test;

import com.hellozq.msio.bean.common.ITransFunctionContainer;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA.
 *
 * @author ThisLi(Bin)
 * @date 2018/9/29
 * time: 14:05
 * To change this template use File | Settings | File Templates.
 */
@Component
public class TransFunc implements ITransFunctionContainer {

    public Object t1(Object o){
        return "ss";
    }
}
