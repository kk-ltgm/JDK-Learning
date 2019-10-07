## 什么是ThreadLocal
ThreadLocal官方文档：
```java
/**
 * This class provides thread-local variables.  These variables differ from
 * their normal counterparts in that each thread that accesses one (via its
 * {@code get} or {@code set} method) has its own, independently initialized
 * copy of the variable.  {@code ThreadLocal} instances are typically private
 * static fields in classes that wish to associate state with a thread (e.g.,
 * a user ID or Transaction ID).
 */
```
中文翻译：ThreadLocal称为线程本地变量。在每个访问此变量的Thread中都会创建一个变量副本。ThreadLocal变量通常需要private static修饰。



## ThreadLocal使用示例
1. 在Context类中定义一个线程本地变量NUM_CONTEXT
2. 构造5个线程，每个线程存储并获取当前的NUM_CONTEXT
```java
public class ThreadLocalDemo {

    public static class Context {

        private static ThreadLocal<Integer> NUM_CONTEXT = new ThreadLocal<>();

        public static Integer getNum() {
            return NUM_CONTEXT.get();
        }

        public static void setNum(Integer num) {
            NUM_CONTEXT.set(num);
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            int num = 1000 + i;
            Thread thread = new Thread(() -> {
                Context.setNum(num);
                print();
            });
            thread.start();
        }

//        运行结果：
//        Thread-0: 1000
//        Thread-3: 1003
//        Thread-2: 1002
//        Thread-4: 1004
//        Thread-1: 1001
    }

    public static void print() {
        System.out.println(Thread.currentThread().getName() + ": " + Context.getNum());
    }
}
```


## ThreadLocal实现原理
> 在每个Thread中，都有一个ThreadLocal.ThreadLocalMap（可暂时理解为HashMap），
> 存储当前Thread所有的ThreadLocal变量副本，key为当前ThreadLocal对象，value为变量副本。
> 初始化Thread时，ThreadLocalMap为空，当任意一个ThreadLocal变量调用get()方法或set()方法时，
> 会对ThreadLocalMap进行初始化。并且将当前ThreadLocal变量副本存储到ThreadLocalMap中。



Thread和ThreadLocal源码主要部分：
```java
public class Thread implements Runnable {
    ThreadLocal.ThreadLocalMap threadLocals = null;
}

public class ThreadLocal<T> {
    /**
     * 获取线程本地变量的值
     */
    public T get() {
        Thread t = Thread.currentThread();
        // 获取当前线程的ThreadLocalMap
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            // 如果ThreadLocalMap存在，返回ThreadLocalMap中的值
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        // 如果ThreadLocalMap不存在，则设置并返回initialValue
        return setInitialValue();
    }

    /**
     * 设置线程本地变量初始值
     */
    private T setInitialValue() {
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
        return value;
    }
    
    /**
     * 设置线程本地变量的值为value。通常情况下，可以覆盖initialValue方法设置初始值
     */
    public void set(T value) {
        Thread t = Thread.currentThread();
        // 获取当前线程的ThreadLocalMap
        ThreadLocalMap map = getMap(t);
        if (map != null)
            // 存在时，存放当前本地变量的值到ThreadLocalMap中
            map.set(this, value);
        else
            // 不存在时，创建当前线程的ThreadLocalMap，并设置初始值value
            createMap(t, value);
    }

    /**
     * 获取指定线程的ThreadLocalMap
     * 此方法是包私有的
     */
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }

    /**
     * 创建指定线程的ThreadLocalMap，并设置初始值firstValue
     * 此方法是包私有的
     */
    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    /**
     * ThreadLocalMap是为维护线程本地变量而自定义的散列表。只能在ThreadLocal类中进行操作。
     * 此类是包私有的
     */
    static class ThreadLocalMap {
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
        }
    }
}
```
