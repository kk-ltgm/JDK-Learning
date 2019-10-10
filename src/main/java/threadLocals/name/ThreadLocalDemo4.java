/*
 * Copyright (c) 2019 maoyan.com
 * All rights reserved.
 *
 */
package threadLocals.name;

import threadLocals.utils.TraceCallable;
import threadLocals.utils.TraceContext;
import threadLocals.utils.TraceRunnable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 在这里编写类的功能描述
 *
 * @author kangkai
 * 2019/10/10
 */
public class ThreadLocalDemo4 {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(4);

        TraceContext.setContext("0123456789");

        for (int i = 0; i < 5; i++) {
            threadPool.execute(TraceRunnable.get(() -> {
                printContext();
            }));

            Future<Integer> task = threadPool.submit(TraceCallable.get(() -> {
                printContext();
                return 1;
            }));
            task.get();
        }

        threadPool.shutdown();
    }


    public static void printContext() {
        System.out.println("thread[" + Thread.currentThread().getName() + "] context: " + TraceContext.getContext());
    }
}

    