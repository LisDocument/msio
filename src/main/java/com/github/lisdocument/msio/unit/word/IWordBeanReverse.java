package com.github.lisdocument.msio.unit.word;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created with IntelliJ IDEA.
 * word导出通用
 * @author Libin
 */
public interface IWordBeanReverse {

    /**
     * 获取word页面实体
     * @return 页面实体
     */
    XWPFDocument getDocument();

    /**
     * 文件写出方法
     * @param response 响应
     * @param fileName 文件名称
     * @throws IOException 写入时出错
     */
    void write(HttpServletResponse response, String fileName) throws IOException;

    /**
     * 文件写出方法
     * @param out 输出流
     * @throws IOException 写入时出错
     */
    void write(OutputStream out) throws IOException;
}
