## ThreadLocal内部属性

```java
public class ThreadLocal<T> {

    private final int threadLocalHashCode = nextHashCode();

    private static AtomicInteger nextHashCode =
        new AtomicInteger();

    private static final int HASH_INCREMENT = 0x61c88647;

    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }
}
```
threadLocalHashCode是一个final的属性，而原子计数器变量nextHashCode和生成下一个哈希魔数的方法nextHashCode()是静态变量和静态方法，静态变量只会初始化一次。换而言之，每新建一个ThreadLocal实例，它内部的threadLocalHashCode就会增加0x61c88647。举个栗子：

```text
//t1中的threadLocalHashCode变量为0x61c88647
ThreadLocal t1 = new ThreadLocal();
//t2中的threadLocalHashCode变量为0x61c88647 + 0x61c88647
ThreadLocal t2 = new ThreadLocal();
//t3中的threadLocalHashCode变量为0x61c88647 + 0x61c88647 + 0x61c88647
ThreadLocal t3 = new ThreadLocal();
```

threadLocalHashCode是下面的ThreadLocalMap结构中使用的哈希算法的核心变量，对于每个ThreadLocal实例，它的threadLocalHashCode是唯一的。


## ThreadLocalMap实现原理
ThreadLocalMap中维护了一个哈希表`Entry[]`，用来存放当前Thread中所有的ThreadLocal变量副本
ThreadLocalMap中getEntry()、set()、remove()三个方法对应的是ThreadLocal中get()、set()、remove()三个方法。

```java
public class ThreadLocal<T> {
    static class ThreadLocalMap {
        static class Entry extends WeakReference<ThreadLocal<?>> {
            /** The value associated with this ThreadLocal. */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

        private Entry[] table;

        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            table = new Entry[INITIAL_CAPACITY];
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            table[i] = new Entry(firstKey, firstValue);
            size = 1;
            setThreshold(INITIAL_CAPACITY);
        }

        private Entry getEntry(ThreadLocal<?> key) {
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
            if (e != null && e.get() == key)
                return e;
            else
                return getEntryAfterMiss(key, i, e);
        }

        private void set(ThreadLocal<?> key, Object value) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            // ...
            tab[i] = new Entry(key, value);
        }

        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    e.clear();
                    expungeStaleEntry(i);
                    return;
                }
            }
        }
    }
}
```


## 哈希
通过ThreadLocal的threadLocalHashCode计算数组的索引位置i，如果位置i已经存储了对象，那么就往后挪一个位置进行查找，直到找到空的位置存放对象


## Entry[]扩容
Entry[]最大容量默认为16，扩容因子为2/3处，每次初始化和扩容Entry[]时都会将下一次需要扩容的位置存储起来，如果超出了阈值，就需要进行扩容，并对所有对象进行重新hash


## 内存泄漏
Entry是一个弱引用对象，引用的是ThreadLocal实例，Entry中的value属性用来存储ThreadLocal变量值。也就是说，当ThreadLocal实例没有强引用时，在下次GC就会从内存中清理调ThreadLocal实例。

我们一般会将ThreadLocal实例定义在某个类的静态变量中，保证了强引用，所以GC时不会回收。如果直接将静态变量赋值为null，就没有了强引用，就会导致GC回收
