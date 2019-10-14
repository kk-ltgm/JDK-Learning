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

```java
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





