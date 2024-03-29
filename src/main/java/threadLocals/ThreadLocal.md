## 什么是ThreadLocal
ThreadLocal的官方文档：
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


## ThreadLocal源码分析
本节，我们直接对ThreadLocal源码进行分析，来进一步了解线程本地变量的含义和实现原理。
在分析ThreadLocal源码之前，我们先了解两个类：ThreadLocalMap和Thread

**ThreadLocalMap**：它是ThreadLocal中一个静态内部类，ThreadLocal存储的变量值实际都存放在ThreadLocalMap中
（由于ThreadLocalMap原理较多，不在本文中详细介绍，这里我们先将它理解为一个HashMap<ThreadLocal, Object>，key是ThreadLocal对象，value是ThreadLocal存储的变量值）
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

**Thread**：在每个Thread内部都有一个ThreadLocalMap，用来存储当前Thread所有的ThreadLocal变量
```java
package java.lang;

public class Thread implements Runnable {
    /**
     * 此属性是包私有的
     */
    ThreadLocal.ThreadLocalMap threadLocals = null;
}
```
<br/>


接下来我们再看ThreadLocal，通过类图我们发现ThreadLocal提供了以下4个可供外部访问的接口：get()、set()、remove()、withInitial()

<img src="./threadLocal.png" width="240" />


get()、set()、remove()三个接口的源码：

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

总结以上内容，ThreadLocal中get()、set()、remove()操作的都是当前Thread中ThreadLocalMap。这也就是使用ThreadLocal实现线程本地变量的主要原理。
<br/>

再看一下withInitial()接口的源码：
```java
public class ThreadLocal<T> {
    /**
     * 创建带初始值的一个线程本地变量，初始值initialValue()返回值从传入的Supplier中获取
     */
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    /**
     * 实现了initialValue方法的ThreadLocal类，initialValue()返回值从初始化传入的Supplier中获取
     */
    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {

        private final Supplier<? extends T> supplier;

        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return supplier.get();
        }
    }
}
```
所以，以下两种方式都可以定义一个带初始值的ThreadLocal
```java
public class ThreadLocalDemo {
    private static final ThreadLocal<Object> CONTEXT1 = new ThreadLocal(){
        @Override
        protected Object initialValue() {
            return 11111;
        }
    };

    private static final ThreadLocal<Object> CONTEXT2 = ThreadLocal.withInitial(() -> 11111);
}
```


## ThreadLocal使用示例

### 示例1：登录用户信息上下文处理
在项目开发中，对于登录用户信息的处理经常会用到ThreadLocal，比如spring项目在拦截器中处理相应逻辑：
1. 在preHandle()方法中获取用户认证token，根据token获取到用户信息后，存储在ThreadLocal中，
2. 业务逻辑中就可以使用ThreadLocal.get()获取登录用户信息
3. 在afterCompletion()方法中调用ThreadLocal.remove()删除登录用户信息

```java
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


// 登录用户信息上下文
public class UserContext {

    private static final ThreadLocal<User> USER_CONTEXT = new ThreadLocal<>();

    public static User getUser() {
        return USER_CONTEXT.get();
    }

    public static void setUser(User user) {
        USER_CONTEXT.set(user);
    }

    public static void removeUser() {
        USER_CONTEXT.remove();
    }
}


public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("token");
//        User user = UserService.getUserByToken(token);
        User user = new User(1, "abc");
        UserContext.setUser(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.removeUser();
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }
}

// 这里我们模拟下拦截器
public class ThreadLocalDemo {

    public static void main(String[] args) {
        User user1 = new User(1, "user1");
        UserContext.setUser(user1);

        ThreadLocalDemo.printUser();
        ThreadLocalDemo.printUser2();

        UserContext.removeUser();
    }

    // 打印当前登录用户信息
    public static void printUser() {
        User currentUser = UserContext.getUser();
        System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + currentUser);
    }

    // 在子线程中获取ThreadLocal
    public static void printUser2() {
        Thread thread = new Thread(() -> {
            User currentUser = UserContext.getUser();
            System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + currentUser);
        });
        thread.start();
    }
}
```
运行结果：
```text
thread[main] user:User{id=1, name='user1'}
thread[Thread-0] user:null
```




 
### 示例2：线程上下文传递（一）
在开发场景中，我们总是需要将父线程中上下文信息传递到子线程中进行使用。比如示例1中，在main线程中开启一个子线程后，子线程同样需要当前用户登录信息处理一些逻辑。
jdk中提供了一种实现方式：将ThreadLocal替换为InheritableThreadLocal：

```java
public class InheritableUserContext {

    private static final ThreadLocal<User> USER_CONTEXT = new InheritableThreadLocal<>();

    public static User getUser() {
        return USER_CONTEXT.get();
    }

    public static void setUser(User user) {
        USER_CONTEXT.set(user);
    }

    public static void removeUser() {
        USER_CONTEXT.remove();
    }
}

public class ThreadLocalDemo {

    public static void main(String[] args) {
        User user1 = new User(1, "user1");
        InheritableUserContext.setUser(user1);

        ThreadLocalDemo.printUser();
        ThreadLocalDemo.printUser2();

        InheritableUserContext.removeUser();
    }

    // 打印当前登录用户信息
    public static void printUser() {
        User currentUser = InheritableUserContext.getUser();
        System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + currentUser);
    }

    // 在子线程中获取ThreadLocal
    public static void printUser2() {
        Thread thread = new Thread(() -> {
            User currentUser = InheritableUserContext.getUser();
            System.out.println("thread[" + Thread.currentThread().getName() + "] user:" + currentUser);
        });
        thread.start();
    }
}
```
执行结果：
```text
thread[main] user:User{id=1, name='user1'}
thread[Thread-0] user:User{id=1, name='user1'}
```

**InheritableThreadLocal源码分析**

InheritableThreadLocal继承自ThreadLocal，并重写了父类三个方法，InheritableThreadLocal变量存放在Thread.inheritableThreadLocals而不是Thread.threadLocals中
```java
public class InheritableThreadLocal<T> extends ThreadLocal<T> {
    protected T childValue(T parentValue) {
        return parentValue;
    }

    ThreadLocalMap getMap(Thread t) {
       return t.inheritableThreadLocals;
    }

    void createMap(Thread t, T firstValue) {
        t.inheritableThreadLocals = new ThreadLocalMap(this, firstValue);
    }
}
```

在new一个线程时，init()方法中会复制parent线程（当前调用线程）中的inheritableThreadLocals到新创建线程中
```java
public class Thread implements Runnable {
    
    ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;
    
    public Thread() {
        init(null, null, "Thread-" + nextThreadNum(), 0);
    }
    
    private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize) {
        init(g, target, name, stackSize, null, true);
    }
    
    
    private void init(ThreadGroup g, Runnable target, String name,
                          long stackSize, AccessControlContext acc,
                          boolean inheritThreadLocals) {
        // 继承父线程中的inheritableThreadLocals
        if (inheritThreadLocals && parent.inheritableThreadLocals != null)
            this.inheritableThreadLocals =
                ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
    }
}
```

同时，ThreadLocal也为Thread.inheritableThreadLocals的复制提供了相应的接口：
```java
public class ThreadLocal<T> {
    
    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }
    
    static class ThreadLocalMap {
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            // ...
        }
    }
}
```

### 示例3：线程上下文传递（二）
在实际项目中，一般都会使用线程池进行多线程编程，线程池中的线程会反复使用，应用需要的是把任务提交给线程池时的ThreadLocal传递给任务执行。
在线程池中运行一个Runnable实例并不会新建一个线程，而是把Runnable实例添加到任务队列中（在核心线程都处理任务的情况下），
让ThreadPoolExecutor的worker从队列里获取一个Runnable实例，然后运行Runnable实例的run()方法。
这时，父子线程的ThreadLocal传递已经没有意义，jdk提供的InheritableThreadLocal没办法使用。下面我们就看一下在线程池中怎么进行上下文传递：


假设我们的调用链通过TraceContext类来保存上下文信息
```java
public class TraceContext {

    private static final ThreadLocal<Object> CONTEXT = new ThreadLocal<>();

    public static Object getContext() {
        return CONTEXT.get();
    }
    public static void setContext(Object obj) {
        CONTEXT.set(obj);
    }
    public static void removeContext() {
        CONTEXT.remove();
    }
}
```

先定义2个类TraceRunnable和TraceCallable，分别继承自Runnable和Callable，目的在于初始化Runnable和Callable实例时保存调用线程的上下文信息，
在执行run()或者call()方法时，先把调用线程的上下文信息设置到当前执行的线程中，run()/call()方法执行后恢复执行线程的上下文

```java
public class TraceRunnable implements Runnable {

    //在初始化TraceRunnable时会获取调用线程的上下文
    private final Object context = TraceContext.getContext();

    private final Runnable runnable;

    public TraceRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        Object backup = TraceContextUtil.backupAndSet(this.context);

        try {
            this.runnable.run();
        } finally {
            TraceContextUtil.restoreBackup(backup);
        }
    }

    public Runnable getRunnable() {
        return this.runnable;
    }

    public static TraceRunnable get(Runnable runnable) {
        if (runnable == null) {
            return null;
        } else {
            return runnable instanceof TraceRunnable ? (TraceRunnable)runnable : new TraceRunnable(runnable);
        }
    }
}

public class TraceCallable<V> implements Callable<V> {

    //在初始化TraceCallable时会获取调用线程的上下文
    private final Object context = TraceContext.getContext();

    private final Callable<V> callable;

    public TraceCallable(Callable<V> callable) {
        this.callable = callable;
    }

    @Override
    public V call() throws Exception {
        Object backup = TraceContextUtil.backupAndSet(this.context);

        V result;
        try {
            result = this.callable.call();
        } finally {
            TraceContextUtil.restoreBackup(backup);
        }

        return result;
    }

    public Callable<V> getCallable() {
        return this.callable;
    }

    //返回TraceCallable实例
    public static <T> TraceCallable<T> get(Callable<T> callable) {
        if (callable == null) {
            return null;
        } else {
            return callable instanceof TraceCallable ? (TraceCallable)callable : new TraceCallable<>(callable);
        }
    }
}


public class TraceContextUtil {

    //设置调用线程的上下文到当前执行线程中,并返回执行线程之前的上下文
    public static Object backupAndSet(Object currentContext) {
        Object backupContext = TraceContext.getContext();
        TraceContext.setContext(currentContext);
        return backupContext;
    }

    //恢复执行线程的上下文
    public static void restoreBackup(Object backup) {
        TraceContext.setContext(backup);
    }
}
```

接下来，我们在线程池中执行TraceRunnble和TraceCallable实现上下文传递：
```java
public class ThreadLocalDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(4);

        TraceContext.setContext("0123456789");

        for (int i = 0; i < 5; i++) {
            threadPool.execute(TraceRunnable.get(() -> {
                printContext();
            }));

            Future<Integer> task = threadPool.submit(TraceCallable.get(() -> {
                printContext();
                return 1;
            }));
            task.get();
        }

        threadPool.shutdown();
    }


    public static void printContext() {
        System.out.println("thread[" + Thread.currentThread().getName() + "] context: " + TraceContext.getContext());
    }
}
```
执行结果：
```text
thread[pool-1-thread-1] context: 0123456789
thread[pool-1-thread-2] context: 0123456789
thread[pool-1-thread-3] context: 0123456789
thread[pool-1-thread-4] context: 0123456789
thread[pool-1-thread-1] context: 0123456789
thread[pool-1-thread-2] context: 0123456789
thread[pool-1-thread-3] context: 0123456789
thread[pool-1-thread-4] context: 0123456789
thread[pool-1-thread-1] context: 0123456789
thread[pool-1-thread-2] context: 0123456789
```
在分布式系统中，需要传递的信息一般包括traceID、 spanID以及部分请求参数等。就可以使用这种方式进行上下文传递。


## 总结
ThreadLocal称为线程本地变量。通过每个线程存储一份变量副本，实现线程之间隔离的效果，只有在线程内才能获取到相应的值。在遇到多线程间共享变量安全问题时，使用ThreadLocal也是一种解决方案。
<br/>
<br/>

参考文章：
* <https://blog.csdn.net/zhuzj12345/article/details/84333765>
* <https://www.ezlippi.com/blog/2019/05/trace-context-bwtween-threads.html>
