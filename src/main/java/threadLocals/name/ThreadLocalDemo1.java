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
public class ThreadLocalDemo1 {

    public static void main(String[] args) {
        User user1 = new User(1, "user1");
        UserContext.setUser(user1);
        printUser();

        Thread thread = new Thread(() -> {
            printUser();

            User user2 = new User(2, "user2");
            UserContext.setUser(user2);
            printUser();
        });
        thread.start();

        printUser();
    }

    public static void printUser() {
        System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + UserContext.getUser());
    }
}

    