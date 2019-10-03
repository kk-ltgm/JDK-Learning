/*
 * Copyright (c) 2019 maoyan.com
 * All rights reserved.
 *
 */
package threadLocals.name;

/**
 * 在这里编写类的功能描述
 *
 * @author kangkai
 * 2019/10/3
 */
public class ThreadLocalDemo {

    public static class Context {

        // 线程本地变量NUM_CONTEXT
        private static final ThreadLocal<Integer> NUM_CONTEXT = new ThreadLocal<>();

        public static Integer getNum() {
            return NUM_CONTEXT.get();
        }

        public static void setNum(Integer num) {
            NUM_CONTEXT.set(num);
        }

        public static void remoteNum() {
            NUM_CONTEXT.remove();
        }
    }

    public static void main(String[] args) {
        // 构造5个线程，在每个线程内调用Context.setNum(num)，再打印Context.getNum()
        for (int i = 0; i < 5; i++) {
            int num = 1000 + i;
            Thread thread = new Thread(() -> {
                Context.setNum(num);
                System.out.println(Thread.currentThread().getName() + ": " + Context.getNum());
            });
            thread.start();
        }
    }
}

    