package com.github.lisdocument.msio.bean.db;

import com.github.lisdocument.msio.anno.MsItem;
import com.github.lisdocument.msio.anno.MsOperator;

import java.util.Date;

/**
 * 下載記錄實體
 * @author Libin
 * @version 1.0.1
 */
@MsOperator(value = "下载记录")
public class DownloadReword {
    /**
     * 下載用戶名，會從request中獲取username的參數加載
     */
    @MsItem("下载用户")
    private String username;
    /**
     * UUID
     */
    @MsItem("记录id")
    private String id;
    /**
     * 下載的當前ip
     */
    @MsItem("下载ip")
    private String ip;
    /**
     * 下載的時間戳，時間戳
     */
    @MsItem("下载时间")
    private Long time;
    /**
     * 訪問的方法路徑
     */
    @MsItem("访问的路径")
    private String url;
    /**
     * 携帶的參數，參數json格式存儲一個Map
     */
    @MsItem("携带的参数")
    private String params;
    /**
     * 本次翻譯下載花費時間
     */
    @MsItem("下载操作花费时间")
    private Integer costTime;
    /**
     * 本地下載使用的方法
     */
    @MsItem("下载方法")
    private String method;

    public String getUsername() {
        return username;
    }

    public DownloadReword setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getId() {
        return id;
    }

    public DownloadReword setId(String id) {
        this.id = id;
        return this;
    }

    public String getIp() {
        return ip;
    }

    public DownloadReword setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public Long getTime() {
        return time;
    }

    public DownloadReword setTime(Long time) {
        this.time = time;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public DownloadReword setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getParams() {
        return params;
    }

    public DownloadReword setParams(String params) {
        this.params = params;
        return this;
    }

    public Integer getCostTime() {
        return costTime;
    }

    public DownloadReword setCostTime(Integer costTime) {
        this.costTime = costTime;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public DownloadReword setMethod(String method) {
        this.method = method;
        return this;
    }

    @Override
    public String toString() {
        return "DownloadReword{" +
                "username='" + username + '\'' +
                ", id='" + id + '\'' +
                ", ip='" + ip + '\'' +
                ", time=" + time +
                ", url='" + url + '\'' +
                ", params='" + params + '\'' +
                ", costTime=" + costTime +
                ", method='" + method + '\'' +
                '}';
    }
}
