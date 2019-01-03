package com.hellozq.msio.test;

import com.hellozq.msio.anno.MsItem;
import com.hellozq.msio.anno.MsOperator;

/**
 * Created with IntelliJ IDEA.
 *
 * @author ThisLi(Bin)
 * @date 2018/11/9
 * time: 13:50
 * To change this template use File | Settings | File Templates.
 */
@MsOperator("user")
public class User {
    @MsItem("名称")
    private String name;
    @MsItem("年龄")
    private Integer age;

    public String getName() {
        return name;
    }

    public User setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getAge() {
        return age;
    }

    public User setAge(Integer age) {
        this.age = age;
        return this;
    }
}
