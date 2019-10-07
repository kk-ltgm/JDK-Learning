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

        private static ThreadLocal<Integer> NUM_CONTEXT = new ThreadLocal<>();

        public static Integer getNum() {
            return NUM_CONTEXT.get();
        }

        public static void setNum(Integer num) {
            NUM_CONTEXT.set(num);
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 1; i++) {
            int num = 1000 + i;
            Thread thread = new Thread(() -> {
                Context.setNum(num);
                print();
            });
            thread.start();
        }

//        运行结果：
//        Thread-0: 1000
//        Thread-3: 1003
//        Thread-2: 1002
//        Thread-4: 1004
//        Thread-1: 1001
    }

    public static void print() {
        Integer num = Context.getNum();
        System.out.println(Thread.currentThread().getName() + ": " + num);
    }
}

    