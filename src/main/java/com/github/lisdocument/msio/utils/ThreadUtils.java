package com.github.lisdocument.msio.utils;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步处理工具类
 * @author bin
 */
public class ThreadUtils {

    private static ThreadPoolExecutor cacheThreadPool;

    static{
        cacheThreadPool = new ThreadPoolExecutor(10, 20, 30, TimeUnit.SECONDS
                , new LinkedBlockingQueue<>(50), new ThreadFactory() {
            AtomicInteger count = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread("异步处理工具单元-"+count.getAndAdd(1));
            }
        });
    }

    /**
     * 定长异步处理数据，确定长度进行数据处理，处理后反馈
     * @param method 需要被调用的方法
     * @param list 数据存储位置
     * @param params 调用时放置的参数
     * @param agent 媒介
     */
    public static void asynFixedLength(Method method, List list, Object agent,Object[]... params){

        int length = params.length;
        final CountDownLatch countDownLatch = new CountDownLatch(length);

        Object[] objs = new Object[length];

        for (int i = 0; i < length; i++) {
            final int index = i;
            cacheThreadPool.execute(()->{
                try {
                    objs[index] = method.invoke(agent,params[index]);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }finally {
                    countDownLatch.countDown();
                }
            });
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        list.addAll(Arrays.asList(objs));
    }
}
