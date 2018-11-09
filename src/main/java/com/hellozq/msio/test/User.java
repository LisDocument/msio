package com.hellozq.msio.test;

import com.hellozq.msio.anno.MsItem;
import com.hellozq.msio.anno.MsOperator;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @author ThisLi(Bin)
 * @date 2018/11/9
 * time: 13:50
 * To change this template use File | Settings | File Templates.
 */
@MsOperator("user")
@Data
public class User {
    @MsItem("名称")
    private String name;
    @MsItem("年龄")
    private int age;
}
