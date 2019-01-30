package com.github.lisdocument.msio.config;

import com.github.lisdocument.msio.bean.db.DownloadReword;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * 接收每次发来的消息的处理机制
 * @author Libin
 */
public abstract class AbstractStoreRecordConfigure<T> {

    protected T dataSource;
    /**
     * 定长保存阵列，
     */
    @SuppressWarnings("unchecked")
    private ExecutorService worker  = new ThreadPoolExecutor(1, 1, 100
            , TimeUnit.SECONDS, new LinkedBlockingQueue<>(15), new ThreadFactoryBuilder().setNameFormat("store").build(),
            ((r, executor) -> log.error(((StoreRunner)r).getInfo() + " 缓冲池积满，存储操作被拒绝")));

    protected final static Logger log = LoggerFactory.getLogger(AbstractStoreRecordConfigure.class);

    @Autowired
    AbstractStoreRecordConfigure(AbstractMsConfigure abstractMsConfigure){
        dataSource = abstractMsConfigure.configDataSource();
        if(null == dataSource){
            log.info("no datasource autowired，autoStore stop");
        }else{
            log.info("datasource autowired，autoStore start");
            init();
        }
    }

    /**
     * 对外抛出的保存接口，保证下载速度，保存异步执行
     * @param downloadReword 下载记录
     */
    public final void send(final DownloadReword downloadReword){
        if(null != dataSource) {
            worker.execute(new StoreRunner(downloadReword));
        }
    }


    /**
     * 保存的实际逻辑实现接口
     * @param downloadReword 下载记录
     */
    protected abstract void save(DownloadReword downloadReword);

    /**
     * 初始化表方法，钩子方法，由构造类调用
     */
    protected abstract void init();

    /**
     * 内部线程
     * @author Libin
     * @version 1.0.1
     */
    private class StoreRunner implements Runnable{

        private DownloadReword downloadReword;

        private StoreRunner(DownloadReword downloadReword){
            this.downloadReword = downloadReword;
        }

        @Override
        public void run() {
            save(downloadReword);
        }

        private String getInfo() {
            return downloadReword.toString();
        }
    }
}
