
package threadLocals;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadLocalDemo {

    public static void main(String[] args) {
        for (int i = 1; i <= 100; i++) {
            Counter.incr();
        }

        System.out.println("thread[" + Thread.currentThread().getName() + "] count:" + Counter.get());

        Runnable runnable = () -> {
            for (int i = 1; i <= 1000; i++) {
                Counter.incr();
            }
            System.out.println("thread[" + Thread.currentThread().getName() + "] count:" + Counter.get());
        };

        Thread t1 = new Thread(runnable);
        Thread t2 = new Thread(runnable);
        t1.start();
        t2.start();

        System.out.println("thread[" + Thread.currentThread().getName() + "] count:" + Counter.get());
    }
}

    