package com.github.lisdocument.msio.unit.excel;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * 导入框架基本信息
 * @author ThisLi(Bin)
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

    /**
     * 获取数据的总页数
     * @return 便于遍历
     */
    int getDataSize();
}
