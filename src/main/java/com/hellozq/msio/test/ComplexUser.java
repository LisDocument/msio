package com.hellozq.msio.test;

import com.hellozq.msio.anno.MsItem;
import com.hellozq.msio.anno.MsOperator;

/**
 * Created with IntelliJ IDEA.
 *
 * @author ThisLi(Bin)
 * @date 2018/12/5
 * time: 13:17
 * To change this template use File | Settings | File Templates.
 */
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

    public User getUser() {
        return user;
    }

    public ComplexUser setUser(User user) {
        this.user = user;
        return this;
    }

    public FlexUser getFlexUser() {
        return flexUser;
    }

    public ComplexUser setFlexUser(FlexUser flexUser) {
        this.flexUser = flexUser;
        return this;
    }

    public FlexUser getFlexUser1() {
        return flexUser1;
    }

    public ComplexUser setFlexUser1(FlexUser flexUser1) {
        this.flexUser1 = flexUser1;
        return this;
    }

    public FlexUser getFlexUser2() {
        return flexUser2;
    }

    public ComplexUser setFlexUser2(FlexUser flexUser2) {
        this.flexUser2 = flexUser2;
        return this;
    }
}
