/*
 * Copyright (c) 2019 maoyan.com
 * All rights reserved.
 *
 */
package threadLocals.utils;

import threadLocals.utils.User;

public class UserContext {

    private static final ThreadLocal<User> USER_CONTEXT = new ThreadLocal<>();

    public static User getUser() {
        return USER_CONTEXT.get();
    }

    public static void setUser(User user) {
        USER_CONTEXT.set(user);
    }

    public static void remoteUser() {
        USER_CONTEXT.remove();
    }
}

    