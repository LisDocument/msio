package com.github.lisdocument.msio.unit.word;

import com.github.lisdocument.msio.utils.MsUtils;
import com.google.common.reflect.ClassPath;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Libin
 */
public abstract class BaseWordBeanReverse implements IWordBeanReverse {

    /**
     * 工作内容实体
     */
    protected XWPFDocument document;
    /**
     * 映射文件名
     */
    protected String fileName;
    /**
     * 外来数据源映射
     */
    protected Object data;

    /**
     * 构造方法，构造方法中调用
     * @see BaseWordBeanReverse#transport() 方法
     * @param fileName 模板文件名称
     * @param data 数据集
     * @throws IOException 包括文件未找到错误，创建流错误
     */
    public BaseWordBeanReverse(String fileName, Object data) throws IOException{
        this.data = data;
        this.fileName = fileName;
        InputStream ins = new  ClassPathResource("model/" + fileName).getInputStream();
        document = new XWPFDocument(ins);
        transport();
    }

    @Override
    public XWPFDocument getDocument() {
        return this.document;
    }

    @Override
    public void write(HttpServletResponse response, String fileName) throws IOException {
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + MsUtils.toUtf8String(fileName));
        document.write(response.getOutputStream());
    }

    @Override
    public void write(OutputStream out) throws IOException {
        document.write(out);
    }

    /**
     * 钩子-->
     * 用于在初始化时执行
     */
    public abstract void transport();
}
