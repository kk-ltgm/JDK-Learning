/*
 * Copyright (c) 2019 maoyan.com
 * All rights reserved.
 *
 */
package threadLocals;

/**
 * 在这里编写类的功能描述
 *
 * @author kangkai
 * 2019/10/2
 */
public class MultiThreadDemo {

    public static void main(String[] args) {
        User user = new User(1, "user1");
        UserContext.setUser(user);



        System.out.println(UserContext.getUser());
    }
}

    