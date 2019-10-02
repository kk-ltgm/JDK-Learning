/*
 * Copyright (c) 2019 maoyan.com
 * All rights reserved.
 *
 */
package threadLocals.utils;

/**
 * 在这里编写类的功能描述
 *
 * @author kangkai
 * 2019/10/2
 */
public class UserRunnable implements Runnable {

    private final User currentUser = UserContext.getUser();

    private final Runnable runnable;

    public UserRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        User backup = UserContext.getUser();
        UserContext.setUser(currentUser);
        try {
            this.runnable.run();
        } finally {
            UserContext.setUser(backup);
        }
    }

    public static UserRunnable get(Runnable runnable) {
        if (runnable == null) {
            return null;
        } else {
            return runnable instanceof UserRunnable ? (UserRunnable) runnable : new UserRunnable(runnable);
        }
    }
}

    