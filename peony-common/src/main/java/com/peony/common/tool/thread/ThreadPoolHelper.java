package com.peony.common.tool.thread;

import java.util.concurrent.*;

/**
 * FIXME 这个类的参数设置，需要重新看一下
 */
public final class ThreadPoolHelper {
    /**
     * 创建一个线程池
     * @param namePrefix 线程名前缀
     * @param corePoolSize
     * @param maximumPoolSize
     * @param queueCapacity
     * @return
     */
    public static ThreadPoolExecutor newThreadPoolExecutor(String namePrefix,
                                                           int corePoolSize,
                                                           int maximumPoolSize,
                                                           int queueCapacity){

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,maximumPoolSize,20, TimeUnit.SECONDS,new LinkedBlockingDeque<Runnable>(queueCapacity),
                new PeonyThreadFactory(namePrefix),
                new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * 创建一个Scheduled线程池
     * @param namePrefix 线程名前缀
     * @param corePoolSize
     * @return
     */
    public static ScheduledThreadPoolExecutor newScheduledThreadPoolExecutor(String namePrefix, int corePoolSize){

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                corePoolSize,
                new PeonyThreadFactory(namePrefix),
                new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}
