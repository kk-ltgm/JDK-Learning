/*
 * Copyright (c) 2019 maoyan.com
 * All rights reserved.
 *
 */
package threadLocals.name;

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
public class ThreadLocalDemo2 {

    public static void main(String[] args) {
        InheritableUserContext.setUser(new User(1, "name1"));
        System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + InheritableUserContext.getUser());

        Thread thread = new Thread(() -> {
            System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + InheritableUserContext.getUser());
        });
        thread.start();
    }
}

    