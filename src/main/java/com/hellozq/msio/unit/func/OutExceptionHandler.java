package com.hellozq.msio.unit.func;
/**
 * @Author: Libin
 * @Description: 导出时错误处理程序，一旦加入错误处理程序，所有错误将会被按照指定方法执行，发生转化错误时仅仅会打印错误信息，不会中断
 * @Date: 20:35 2018/11/8
 */
@FunctionalInterface
public interface OutExceptionHandler {

    /**
     * 处理方法，true和false为是否接下去执行
     * @param e 转录过程中出现的错误信息
     * @param obj 转录前的数据，作为模板会传入
     * @return 修改后的修正对象，用于替代错误信息填入错误格
     */
    Object handle(Exception e,Object obj);
}
