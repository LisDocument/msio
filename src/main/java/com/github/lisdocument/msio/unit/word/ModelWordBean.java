package com.github.lisdocument.msio.unit.word;

import com.github.lisdocument.msio.utils.ClassUtils;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Libin
 */
public class ModelWordBean extends BaseWordBeanReverse {

    /**
     * 构造方法，构造方法中调用
     * @see BaseWordBeanReverse#transport() 方法
     * @param fileName 模板文件名称
     * @param data 数据集
     * @throws IOException 包括文件未找到错误，创建流错误
     */
    public ModelWordBean(String fileName, List data) throws IOException{
        super(fileName, data);
    }

    @Override
    public void transport() {
        Iterator<XWPFParagraph> pit = document.getParagraphsIterator();
        while (pit.hasNext()){
            XWPFParagraph next = pit.next();

        }
    }

    public void replaceGraph(XWPFParagraph p, Object obj){
        for (XWPFRun run : p.getRuns()) {
            String content = run.getText(run.getTextPosition());
            //如果包含表达式的话->证明需要被替换
            //需要考虑一个run中携带多个变量的情况，需要一一修改,先根据$进行分割，分割后留下{}作为变量的获取方式
            String[] oldData = content.split("\\$");
            if(oldData.length == 1) {
                continue;
            }
            for (String oldItem : oldData) {
                int start = oldItem.indexOf("{");
                int end = oldItem.lastIndexOf("}");
                //只有两个同时不等于-1的情况下才会认定当前变量存在，且开始位置要小于结束位置
                if(start != -1 && end != -1 && start < end){
                    String key = oldItem.substring(start, end - start);
                    String value = ClassUtils.getValueByInvoke(key, obj);
                    //替换
                    content = content.replaceAll("\\$" + "\\{"+ key +"}", value);
                }
            }
            //给原始赋值
            run.setText(content, 0);
        }
    }
}
