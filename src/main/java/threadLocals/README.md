[TOC]

## 什么是ThreadLocal
下面是ThreadLocal的官方文档：
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
    }

    public static void print() {
        System.out.println(Thread.currentThread().getName() + ": " + Context.getNum());
    }
}

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

// 用户信息
public class User {

    private Integer id;

    private String name;

    public User(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
```
运行结果：
```text
Thread-0: 1000
Thread-3: 1003
Thread-2: 1002
Thread-4: 1004
Thread-1: 1001
```


## ThreadLocal实现原理
**在每个Thread内部都有一个ThreadLocalMap，存储当前Thread所有的ThreadLocal变量副本**
```java
package java.lang;

public class Thread implements Runnable {
    /*
     * InheritableThreadLocal values pertaining to this thread. This map is
     * maintained by the InheritableThreadLocal class.
     */
    ThreadLocal.ThreadLocalMap threadLocals = null;
}
```
**ThreadLocalMap定义在ThreadLocal中，可暂时理解为它是一个HashMap，key是ThreadLocal对象，value是线程变量副本**
```java
package java.lang;

public class ThreadLocal<T> {
    static class ThreadLocalMap {
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            // ...
        }
    }
}
```
**ThreadLocal是Thread中ThreadLocalMap的管理者。对于ThreadLocal的set()、get()、remove()的操作结果，都是针对当前Thread中的ThreadLocalMap进行存储、获取、删除操作。**

具体分析看以下代码：
```java
package java.lang;

public class ThreadLocal<T> {
    /**
     * 线程本地变量的初始值
     * 1.只有在第一次调用get()方法时，才会调用此方法并设置初始值
     * 2.如果直接调用set()方法，此方法不会被调用
     * 3.如果调用remove()之后，再次调用get()方法时，会再次调用该方法
     *
     * 如果需要初始值，一般情况下使用匿名子类重写此方法，建议使用ThreadLocal.withInitial()创建子类
     */
    protected T initialValue() {
        return null;
    }

    /**
     * 创建带初始值的一个线程本地变量，初始值initialValue()返回值从传入的Supplier中获取
     */
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    /**
     * 创建一个线程本地变量
     */
    public ThreadLocal() {
    }

    /**
     * 获取当前线程的变量副本值
     */
    public T get() {
        // 获取当前线程的ThreadLocalMap
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        // 如果ThreadLocalMap已经初始化，并且ThreadLocalMap中存在变量副本，则返回变量副本
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        // 否则，设置并返回变量副本初始值
        return setInitialValue();
    }

    /**
     * 设置当前线程的变量副本初始值
     * 通过覆盖initialValue方法可设置初始值
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
     * 设置当前线程的变量副本值
     */
    public void set(T value) {
        // 获取当前线程的ThreadLocalMap
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            // 如果ThreadLocalMap已经初始化，设置变量副本值
            map.set(this, value);
        else
            // 否则，创建当前线程的ThreadLocalMap，并设置变量副本值为value
            createMap(t, value);
    }

    /**
     * 删除当前线程的变量副本值
     */
    public void remove() {
        ThreadLocalMap m = getMap(Thread.currentThread());
        if (m != null)
            m.remove(this);
    }

    /**
     * 获取指定线程的ThreadLocalMap
     * 此方法是包私有的
     *
     * @param  t the current thread
     * @return the map
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
}
```
> 注意：ThreadLocal中可以直接调用`t.threadLocals`是因为Thread与ThreadLocal在同一个包下，
> 同样Thread可以直接访问`ThreadLocal.ThreadLocalMap threadLocals = null;`来进行声明属性。




## ThreadLocalMap实现原理


















