package com.github.lisdocument.msio.unit.excel;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * 导出框架基本信息
 * @author ThisLi(Bin)
 * @date 2019/1/3
 * time: 13:29
 * To change this template use File | Settings | File Templates.
 */
public interface IExcelBean {

    /**
     * 查询该页的数据
     * @param pageNo 页码
     * @return 该页转义后的数组
     */
    List getData(Integer pageNo);
}
