package com.hellozq.msio.test;

import com.hellozq.msio.anno.MsItem;
import com.hellozq.msio.anno.MsOperator;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @author ThisLi(Bin)
 * @date 2018/12/5
 * time: 13:17
 * To change this template use File | Settings | File Templates.
 */
@Data
@MsOperator(value = "complexUser",subClazz = {User.class,FlexUser.class})
public class ComplexUser {

    @MsItem("简单测试用户")
    private User user;

    @MsItem("复杂则是用户")
    private FlexUser flexUser;

    @MsItem("复杂则是用户1")
    private FlexUser flexUser1;

    @MsItem("复杂则是用户2")
    private FlexUser flexUser2;
}
