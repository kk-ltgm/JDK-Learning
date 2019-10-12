/*
 * Copyright (c) 2019 maoyan.com
 * All rights reserved.
 *
 */
package threadLocals.demo;

import threadLocals.utils.User;
import threadLocals.utils.UserContext;

/**
 * 模拟登录用户信息上下文设置场景，来测试ThreadLocal变量副本作用。
 */
public class ThreadLocalDemo1 {

    public static void main(String[] args) {
        User user1 = new User(1, "user1");
        UserContext.setUser(user1);

        ThreadLocalDemo1.printUser();
        ThreadLocalDemo1.printUser2();

        UserContext.removeUser();
    }

    /**
     * 打印当前登录用户信息
     */
    public static void printUser() {
        User currentUser = UserContext.getUser();
        System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + currentUser);
    }

    /**
     * 在子线程中获取ThreadLocal
     */
    public static void printUser2() {
        Thread thread = new Thread(() -> {
            User currentUser = UserContext.getUser();
            System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + currentUser);
        });
        thread.start();
    }
}

    