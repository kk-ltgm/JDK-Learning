# ThreadLocal源码阅读

```java

package java.lang;

import java.lang.ref.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * This class provides thread-local variables.  These variables differ from
 * their normal counterparts in that each thread that accesses one (via its
 * {@code get} or {@code set} method) has its own, independently initialized
 * copy of the variable.  {@code ThreadLocal} instances are typically private
 * static fields in classes that wish to associate state with a thread (e.g.,
 * a user ID or Transaction ID).
 *
 * <p>For example, the class below generates unique identifiers local to each
 * thread.
 * A thread's id is assigned the first time it invokes {@code ThreadId.get()}
 * and remains unchanged on subsequent calls.
 * <pre>
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 * public class ThreadId {
 *     // Atomic integer containing the next thread ID to be assigned
 *     private static final AtomicInteger nextId = new AtomicInteger(0);
 *
 *     // Thread local variable containing each thread's ID
 *     private static final ThreadLocal&lt;Integer&gt; threadId =
 *         new ThreadLocal&lt;Integer&gt;() {
 *             &#64;Override protected Integer initialValue() {
 *                 return nextId.getAndIncrement();
 *         }
 *     };
 *
 *     // Returns the current thread's unique ID, assigning it if necessary
 *     public static int get() {
 *         return threadId.get();
 *     }
 * }
 * </pre>
 * <p>Each thread holds an implicit reference to its copy of a thread-local
 * variable as long as the thread is alive and the {@code ThreadLocal}
 * instance is accessible; after a thread goes away, all of its copies of
 * thread-local instances are subject to garbage collection (unless other
 * references to these copies exist).
 *
 * @author  Josh Bloch and Doug Lea
 * @since   1.2
 */
public class ThreadLocal<T> {
    /**
     * 当前线程本地变量的散列码，通过此散列码来找到ThreadLocalMap中的位置
     * 此散列码的值通过递加魔数0x61c88647生成，解决了散列值key的冲突问题
     * ThreadLocalMap通过线性探测方式解决散列冲突
     */
    private final int threadLocalHashCode = nextHashCode();

    /**
     * nextHashCode原子更新器，初始值从0开始
     */
    private static AtomicInteger nextHashCode =
            new AtomicInteger();

    /**
     * 斐波那契数列黄金分割数
     */
    private static final int HASH_INCREMENT = 0x61c88647;

    /**
     * 原子性获取nextHashCode，步长为HASH_INCREMENT
     */
    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    /**
     * 线程本地变量的初始值
     * 1.只有在第一次调用get()方法时，才会调用此方法并设置初始值
     * 2.如果直接调用set()方法，此方法不会被调用
     * 3.如果调用remove()之后，再次调用get()方法时，会再次调用该方法
     *
     * 如果需要初始值，一般情况下使用匿名子类重写此方法，建议使用{@link #withInitial(Supplier)}创建子类
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
     * 删除线程本地变量。
     * 下一次使用get()方法获取线程本地变量时，会进行重新初始化，
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

    /**
     * Factory method to create map of inherited thread locals.
     * Designed to be called only from Thread constructor.
     *
     * @param  parentMap the map associated with parent thread
     * @return a map containing the parent's inheritable bindings
     */
    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }

    /**
     * Method childValue is visibly defined in subclass
     * InheritableThreadLocal, but is internally defined here for the
     * sake of providing createInheritedMap factory method without
     * needing to subclass the map class in InheritableThreadLocal.
     * This technique is preferable to the alternative of embedding
     * instanceof tests in methods.
     */
    T childValue(T parentValue) {
        throw new UnsupportedOperationException();
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

    /**
     * ThreadLocalMap is a customized hash map suitable only for
     * maintaining thread local values. No operations are exported
     * outside of the ThreadLocal class. The class is package private to
     * allow declaration of fields in class Thread.  To help deal with
     * very large and long-lived usages, the hash table entries use
     * WeakReferences for keys. However, since reference queues are not
     * used, stale entries are guaranteed to be removed only when
     * the table starts running out of space.
     *
     *
     * ThreadLocalMap是为维护线程本地变量而自定义的散列表。只能在ThreadLocal类中进行操作。
     * ThreadLocalMap是包私有的，可以在Thread类中声明ThreadLocalMap字段。
     *
     */
    static class ThreadLocalMap {

        /**
         * The entries in this hash map extend WeakReference, using
         * its main ref field as the key (which is always a
         * ThreadLocal object).  Note that null keys (i.e. entry.get()
         * == null) mean that the key is no longer referenced, so the
         * entry can be expunged from table.  Such entries are referred to
         * as "stale entries" in the code that follows.
         */
        static class Entry extends WeakReference<ThreadLocal<?>> {
            /** The value associated with this ThreadLocal. */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

        /**
         * table初始容量，必须是2的幂次方
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * Entry表，根据需要调整大小，length必须是2的幂次方
         */
        private Entry[] table;

        /**
         * table中实际的Entry数量
         */
        private int size = 0;

        /**
         * table下一次需要扩容时的size
         */
        private int threshold; // Default to 0

        /**
         * 设置threshold为table.length的2/3处
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        /**
         * 获取下一个索引值（环形）
         */
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * 获取上一个索引值（环形）
         */
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        /**
         * 初始化ThreadLocalMap，需要有一个初始值
         * @param firstKey
         * @param firstValue
         */
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            // 设置table的初始length为16
            table = new Entry[INITIAL_CAPACITY];
            // 获取firstValue在table中的存放下标
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            table[i] = new Entry(firstKey, firstValue);
            size = 1;
            // 设置table下一次需要扩容的size位置
            setThreshold(INITIAL_CAPACITY);
        }

        /**
         * Construct a new map including all Inheritable ThreadLocals
         * from given parent map. Called only by createInheritedMap.
         *
         * @param parentMap the map associated with parent thread.
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * 获取ThreadLocal对应的Entry
         * @param key
         * @return
         */
        private Entry getEntry(ThreadLocal<?> key) {
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
            if (e != null && e.get() == key)
                // 如果key命中，直接返回
                return e;
            else
                return getEntryAfterMiss(key, i, e);
        }

        /**
         * 如果hash值发生冲突后，通过线性探测方法查找ThreadLocal对应的Entry
         */
        private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
            Entry[] tab = table;
            int len = tab.length;

            while (e != null) {
                ThreadLocal<?> k = e.get();
                if (k == key)
                    return e;
                if (k == null)
                    // 清除过期的Entry，并对i后面的Entry进行位置调整
                    expungeStaleEntry(i);
                else
                    i = nextIndex(i, len);
                // 下一个Entry
                e = tab[i];
            }
            return null;
        }

        /**
         * 设置和ThreadLocal关联的value
         */
        private void set(ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.

            // 创建一个table的地址引用
            Entry[] tab = table;
            int len = tab.length;
            // 计算key的hash值
            int i = key.threadLocalHashCode & (len-1);

            // 获取下标i位置的Entry，如果遇到hash冲突，就进行线性探测查找key应该存储的位置
            for (Entry e = tab[i]; e != null; e = tab[i = nextIndex(i, len)]) {
                // 获取当前Entry上存储的ThreadLocal，赋值为k
                ThreadLocal<?> k = e.get();

                // 如果当前Entry上存储的ThreadLocal和待存储的ThreadLocal对象地址相同，则直接替换Entry的value值
                if (k == key) {
                    e.value = value;
                    return;
                }

                // 如果当前Entry上存储的ThreadLocal为null，说明Entry已经过期
                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }

            // 如果没有遇到hash冲突，直接将ThreadLocal存放在下标i位置
            tab[i] = new Entry(key, value);
            // 已经存放的Entry总数量+1
            int sz = ++size;
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }

        /**
         * 删除指定的ThreadLocal
         */
        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            // 线性探测法查找ThreadLocal
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    e.clear();  // 清除Entry上的引用值
                    expungeStaleEntry(i);   // 清除当前索引上的Entry
                    return;
                }
            }
        }

        /**
         * Replace a stale entry encountered during a set operation
         * with an entry for the specified key.  The value passed in
         * the value parameter is stored in the entry, whether or not
         * an entry already exists for the specified key.
         *
         * As a side effect, this method expunges all stale entries in the
         * "run" containing the stale entry.  (A run is a sequence of entries
         * between two null slots.)
         *
         * @param  key the key
         * @param  value the value to be associated with key
         * @param  staleSlot index of the first stale entry encountered while
         *         searching for key.
         */
        private void replaceStaleEntry(ThreadLocal<?> key, Object value,
                                       int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            // Back up to check for prior stale entry in current run.
            // We clean out whole runs at a time to avoid continual
            // incremental rehashing due to garbage collector freeing
            // up refs in bunches (i.e., whenever the collector runs).
            // 复制staleSlot
            int slotToExpunge = staleSlot;
            // 找到上一个已过期的Entry
            for (int i = prevIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = prevIndex(i, len))
                if (e.get() == null)
                    slotToExpunge = i;

            // Find either the key or trailing null slot of run, whichever
            // occurs first
            for (int i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();

                // If we find key, then we need to swap it
                // with the stale entry to maintain hash table order.
                // The newly stale slot, or any other stale slot
                // encountered above it, can then be sent to expungeStaleEntry
                // to remove or rehash all of the other entries in run.
                if (k == key) {
                    e.value = value;

                    tab[i] = tab[staleSlot];
                    tab[staleSlot] = e;

                    // Start expunge at preceding stale entry if it exists
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
                    return;
                }

                // If we didn't find stale entry on backward scan, the
                // first stale entry seen while scanning for key is the
                // first still present in the run.
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            // If key not found, put new entry in stale slot
            tab[staleSlot].value = null;
            tab[staleSlot] = new Entry(key, value);

            // If there are any other stale entries in run, expunge them
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }

        /**
         * 清除索引staleSlot上的Entry，并对之后的Entry进行位置调整（rehash）
         * @return staleSlot之后的下一个值为null的索引
         */
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            // 将索引staleSlot位置的Entry值设置为null
            tab[staleSlot].value = null;
            tab[staleSlot] = null;
            size--;

            // 对staleSlot之后的值进行位置调整（rehash），直到遇到null
            Entry e;
            int i;
            for (i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                // 当前索引上的ThreadLocal
                ThreadLocal<?> k = e.get();
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {
                    // 如果ThreadLocal计算的hash值和实际存储的位置不一样（存储时遇到了冲突），进行位置调整（rehash）
                    int h = k.threadLocalHashCode & (len - 1);
                    if (h != i) {
                        tab[i] = null;

                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            return i;
        }

        /**
         * 扫描Entry表，清除过期的Entry（对数次扫描，O(logn)）
         * @param i 当前索引值
         * @param n Entry[]实际大小
         * @return 是否有过期的Entry被清除
         */
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            do {
                i = nextIndex(i, len);
                Entry e = tab[i];
                if (e != null && e.get() == null) {
                    n = len;
                    removed = true;
                    i = expungeStaleEntry(i);
                }
            } while ( (n >>>= 1) != 0);
            return removed;
        }

        /**
         * 重新打包或调整table大小。首先扫描整个表，删除过期的Entry。如果表大小未改变，对table进行加倍扩容
         */
        private void rehash() {
            expungeStaleEntries();

            // Use lower threshold for doubling to avoid hysteresis
            if (size >= threshold - threshold / 4)
                resize();
        }

        /**
         * 对table进行双倍扩容，并对已经存放的Entry重新hash
         */
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            int newLen = oldLen * 2;
            // 新的Entry[]
            Entry[] newTab = new Entry[newLen];
            int count = 0;

            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        int h = k.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            setThreshold(newLen);
            size = count;
            table = newTab;
        }

        /**
         * 删除所有已经过期的Entry
         */
        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                if (e != null && e.get() == null)
                    expungeStaleEntry(j);
            }
        }
    }
}



    

```