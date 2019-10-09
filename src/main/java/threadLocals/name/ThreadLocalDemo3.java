/*
 * Copyright (c) 2019 maoyan.com
 * All rights reserved.
 *
 */
package threadLocals.name;

import threadLocals.utils.InheritableUserContext;
import threadLocals.utils.User;
import threadLocals.utils.UserContext;

/**
 * 通过InheritableThreadLocal实现ThreadLocal传递
 *
 * 1.InheritableThreadLocal变量存放在Thread.inheritableThreadLocals而不是Thread.threadLocals中
 * 2.new Thread()时，init()中会复制parent线程中的inheritableThreadLocals到当前线程
 * 3.InheritableThreadLocal不适合用在线程池中
 *
 */
public class ThreadLocalDemo3 {

    public static void main(String[] args) {
        // 在main线程中设置当前用户信息为user1
        User user1 = new User(1, "user1");
        InheritableUserContext.setUser(user1);
        // 打印main线程中当前用户信息，输出结果为user1
        printUser();

        Thread thread = new Thread(() -> {
            // 打印子线程中当前用户信息，输出结果为null
            printUser();

            Thread thread1 = new Thread(() -> {
                printUser();
            });
            thread1.start();
        });
        thread.start();
    }

    public static void printUser() {
        System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + InheritableUserContext.getUser());
    }
}

    