/*
 * Copyright (c) 2019 maoyan.com
 * All rights reserved.
 *
 */
package threadLocals;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 在这里编写类的功能描述
 *
 * @author kangkai
 * 2019/9/30
 */
public class Counter {

    private static final ThreadLocal<AtomicInteger> counter = ThreadLocal.withInitial(AtomicInteger::new);

    public static int get() {
        return counter.get().get();
    }

    public static int incr() {
        return counter.get().incrementAndGet();
    }

    public static int decr() {
        return counter.get().decrementAndGet();
    }
}

    