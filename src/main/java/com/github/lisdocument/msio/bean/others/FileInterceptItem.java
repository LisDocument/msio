package com.github.lisdocument.msio.bean.others;

import org.apache.commons.fileupload.FileItem;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 文件过滤信息实体
 * @author bin
 */
@SuppressWarnings("unused")
public class FileInterceptItem {

    /**
     * 文件名称，使用正则表达式进行过滤
     */
    private String regexName;

    /**
     * 文件种类
     */
    private String fileType;

    /**
     * 文件能够接受的最大值
     */
    private Long fileMaxSize;

    /**
     * 文件能够接受的最小值
     */
    private Long fileMinSize;

    /**
     * 过滤链规则
     */
    private int orderNo = 0;

    /**
     * 如果基础属性不满足的情况下，用户可自定义的方法属性
     */
    private FileInterceptCustomize<FileItem> fileInterceptCustomize;

    private final static String EXCEL_OLD = "xls";

    private final static String EXCEL_NEW = "xlsx";

    private final static String WORD_OLD = "doc";

    private final static String WORD_NEW = "docx";

    private final static String PDF = "pdf";

    private final static String CSV = "csv";

    private final static String TXT = "txt";

    private final static String PIC = "jpg";

    public String getFileType() {
        return fileType;
    }

    public FileInterceptItem setFileType(String fileType) {
        this.fileType = fileType;
        return this;
    }

    public Long getFileMaxSize() {
        return fileMaxSize;
    }

    public FileInterceptItem setFileMaxSize(Long fileMaxSize) {
        this.fileMaxSize = fileMaxSize;
        return this;
    }

    public Long getFileMinSize() {
        return fileMinSize;
    }

    public FileInterceptItem setFileMinSize(Long fileMinSize) {
        this.fileMinSize = fileMinSize;
        return this;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public FileInterceptItem setOrderNo(int orderNo) {
        this.orderNo = orderNo;
        return this;
    }

    public FileInterceptCustomize<FileItem> getFileInterceptCustomize() {
        return fileInterceptCustomize;
    }

    public FileInterceptItem setFileInterceptCustomize(FileInterceptCustomize<FileItem> fileInterceptCustomize) {
        this.fileInterceptCustomize = fileInterceptCustomize;
        return this;
    }

    public String getRegexName() {
        return regexName;
    }

    public FileInterceptItem setRegexName(String regexName) {
        this.regexName = regexName;
        return this;
    }

    public boolean filter(FileItem file){
        String name = file.getName();
        if(StringUtils.isEmpty(regexName)){
            String fileName = name.substring(0, name.lastIndexOf("."));
            if(!Pattern.matches(regexName, fileName)){
                return false;
            }
        }
        if(StringUtils.isEmpty(fileType)){
            String type = name.substring(name.lastIndexOf(".") + 1);
            if(!fileType.equals(type)){
                return false;
            }
        }
        if(null != fileMaxSize){
            if(file.getSize() >= fileMaxSize){
                return false;
            }
        }
        if(null != fileMinSize){
            if(file.getSize() <= fileMinSize){
                return false;
            }
        }
        if(null != fileInterceptCustomize){
            return fileInterceptCustomize.filter(file);
        }
        return true;
    }

    @FunctionalInterface
    public interface FileInterceptCustomize<F>{
        /**
         * 自定义满足特定需求的过滤方法
         * @param from 文件
         * @return 过滤结果
         */
        boolean filter(F from);
    }
}
