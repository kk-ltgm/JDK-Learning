/*
 * Copyright (c) 2019 maoyan.com
 * All rights reserved.
 *
 */
package threadLocals.name;

import threadLocals.utils.User;
import threadLocals.utils.UserContext;

/**
 * 模拟登录用户信息上下文设置场景，来测试ThreadLocal变量副本作用。
 */
public class ThreadLocalDemo1 {

    public static void main(String[] args) {
        // 在main线程中设置当前用户信息为user1
        User user1 = new User(1, "user1");
        UserContext.setUser(user1);
        // 打印main线程中当前用户信息，输出结果为user1
        printUser();

        Thread thread = new Thread(() -> {
            // 打印子线程中当前用户信息，输出结果为null
            printUser();

            // 在子线程中设置当前用户信息为user2
            User user2 = new User(2, "user2");
            UserContext.setUser(user2);
            // 打印子线程中当前用户信息，输出结果为user2
            printUser();
        });
        thread.start();

        // 再打印main线程中当前用户信息，输出结果仍是user1
        printUser();
    }

    public static void printUser() {
        System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + UserContext.getUser());
    }
}

    