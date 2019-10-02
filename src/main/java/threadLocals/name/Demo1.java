/*
 * Copyright (c) 2019 maoyan.com
 * All rights reserved.
 *
 */
package threadLocals.name;

import threadLocals.utils.User;
import threadLocals.utils.UserContext;

/**
 * 演示线程本地变量特性、变量隔离
 *
 */
public class Demo1 {

    public static void main(String[] args) {
        UserContext.setUser(new User(1, "name1"));
        System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + UserContext.getUser());

        Thread thread = new Thread(() -> {
            System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + UserContext.getUser());
        });
        thread.start();
    }
}

    