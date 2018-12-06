package com.hellozq.msio.test;

import com.hellozq.msio.anno.MsItem;
import com.hellozq.msio.anno.MsOperator;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * 测试复杂用户
 * @author ThisLi(Bin)
 * @date 2018/12/5
 * time: 10:11
 * To change this template use File | Settings | File Templates.
 */
@Data
@MsOperator(value = "flexUser",subClazz = User.class)
public class FlexUser {

    @MsItem("用户信息")
    private User user;

    @MsItem("电话号码")
    private String telPhone;

    @MsItem("性别")
    private String sex;
}
