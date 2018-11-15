package com.hellozq.msio.test;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author ThisLi(Bin)
 * @date 2018/11/15
 * time: 15:10
 * To change this template use File | Settings | File Templates.
 */
public class Page {

    private List list;

    public Page(List list) {
        this.list = list;
    }

    public List getList(int i,int j,String s,double d) {
        System.out.println(s + "字符串");
        System.out.println(d + "小数");
        return list.subList(i,j);
    }

    public Page setList(List list) {
        this.list = list;
        return this;
    }
}
