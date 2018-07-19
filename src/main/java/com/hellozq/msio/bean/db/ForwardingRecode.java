package com.hellozq.msio.bean.db;

import com.hellozq.msio.anno.MsItem;
import com.hellozq.msio.anno.MsOperator;
import com.hellozq.msio.bean.common.impl.DateOperator;

import java.util.Date;

/**
 * @author bin
 * 转发记录，用于转发数据的记录，相当于框架自带的存储方式
 */
@SuppressWarnings("unused")
@MsOperator(value = "forwardingRecode")
public class ForwardingRecode {

    /**
     * 当前用户名称，无法获取，需用户提交
     */
    @MsItem(value = "登录用户名")
    private String userName;

    /**
     * 文件来源url
     */
    @MsItem(value = "文件来源地址")
    private String sourceUrl;

    /**
     * 文件来源时间
     */
    @MsItem(value = "文件来源时间",transFormOperator = DateOperator.class)
    private Date uploadTime;

    /**
     * 文件名称
     */
    @MsItem(value = "文件名称")
    private String fileName;

    /**
     * 文件处理过程中产生的异常
     */
    private Exception exception;

    /**
     * 文件大小
     */
    private long fileSize;

    /**
     * 上传后文件保存的地址
     */
    private String cacheFilePath;

    /**
     * 是否进行了数据库存储操作
     */
    private boolean isStore;

    /**
     * 是否对前端有反馈操作
     */
    private boolean isReturn;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getCacheFilePath() {
        return cacheFilePath;
    }

    public void setCacheFilePath(String cacheFilePath) {
        this.cacheFilePath = cacheFilePath;
    }

    public boolean isStore() {
        return isStore;
    }

    public void setStore(boolean store) {
        isStore = store;
    }

    public boolean isReturn() {
        return isReturn;
    }

    public void setReturn(boolean aReturn) {
        isReturn = aReturn;
    }
}
