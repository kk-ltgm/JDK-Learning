/*
 * Copyright (c) 2019 maoyan.com
 * All rights reserved.
 *
 */
package threadLocals.demo;

import threadLocals.utils.InheritableUserContext;
import threadLocals.utils.User;

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
        User user1 = new User(1, "user1");
        InheritableUserContext.setUser(user1);

        ThreadLocalDemo3.printUser();
        ThreadLocalDemo3.printUser2();

        InheritableUserContext.removeUser();
    }

    /**
     * 打印当前登录用户信息
     */
    public static void printUser() {
        User currentUser = InheritableUserContext.getUser();
        System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + currentUser);
    }

    /**
     * 在子线程中获取ThreadLocal
     */
    public static void printUser2() {
        Thread thread = new Thread(() -> {
            User currentUser = InheritableUserContext.getUser();
            System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + currentUser);
        });
        thread.start();
    }
}

    