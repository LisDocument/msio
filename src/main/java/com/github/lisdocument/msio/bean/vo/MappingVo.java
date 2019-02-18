package com.github.lisdocument.msio.bean.vo;

/**
 * Created with IntelliJ IDEA.
 * 映射实例输出
 * @author Libin
 * @version 1.0.1
 */
public class MappingVo {
    /**
     * 是否可更改项，一般是由于与类绑定，因此无法修改，通过配置文件处理的可更改
     * 可更改为true
     */
    private boolean isChange;
    /**
     * 无法修改的主键id，与接口绑定
     */
    private String id;
    /**
     * 类全名，包括路径,和可更改标记有关
     */
    private String className;

}
