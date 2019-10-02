/*
 * Copyright (c) 2019 maoyan.com
 * All rights reserved.
 *
 */
package threadLocals.name;

import threadLocals.utils.*;

import java.util.concurrent.*;

/**
 * 在这里编写类的功能描述
 *
 * @author kangkai
 * 2019/10/2
 */
public class Demo3 {

    public static ExecutorService getThreadPool() {
        int corePoolSize = 1;
        int maximumPoolSize = 1;
        long keepAliveTime = 10;
        TimeUnit unit = TimeUnit.MINUTES;
        LinkedBlockingDeque<Runnable> workQueue = new LinkedBlockingDeque<>(100);
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        ThreadPoolExecutor.CallerRunsPolicy rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory,
                rejectedExecutionHandler);
    }

    public static void main(String[] args) {
        UserContext.setUser(new User(1, "name1"));
        System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + UserContext.getUser());

        Runnable userRunnable = UserRunnable.get(() -> {
            System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + UserContext.getUser());
        });


        ExecutorService threadPool = getThreadPool();
        threadPool.execute(UserRunnable.get(() -> {
            System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + UserContext.getUser());
        }));
        UserContext.setUser(new User(2, "name2"));
        System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + UserContext.getUser());
        threadPool.execute(UserRunnable.get(() -> {
            System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + UserContext.getUser());
        }));

        System.out.println("aaa");

        threadPool.shutdown();
    }
}

    