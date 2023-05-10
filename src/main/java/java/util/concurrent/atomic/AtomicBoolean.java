package java.util.concurrent.atomic;
import sun.misc.Unsafe;

/**
 * 原子类 Boolean
 */
public class AtomicBoolean implements java.io.Serializable {
    private static final long serialVersionUID = 4654671469794556979L;
    // unsafe实例
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    // value所在内存偏移量
    private static final long valueOffset;

    static {
        try {
            // 获得value字段在对象中的内存偏移量
            valueOffset = unsafe.objectFieldOffset
                (AtomicBoolean.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }
    // 存储值 1=true，0=false
    private volatile int value;

    /**
     * 有参构造器
     */
    public AtomicBoolean(boolean initialValue) {
        value = initialValue ? 1 : 0;
    }

    /**
     * 无参构造器
     */
    public AtomicBoolean() {
    }

    /**
     * 返回当前值
     */
    public final boolean get() {
        return value != 0;
    }

    /**
     * CAS修改值，先转化为int，再调用unsafe类执行CAS
     */
    public final boolean compareAndSet(boolean expect, boolean update) {
        int e = expect ? 1 : 0;
        int u = update ? 1 : 0;
        return unsafe.compareAndSwapInt(this, valueOffset, e, u);
    }

    /**
     * 和compareAndSet一模一样，查资料得知Java8中两者没有区别
     */
    public boolean weakCompareAndSet(boolean expect, boolean update) {
        int e = expect ? 1 : 0;
        int u = update ? 1 : 0;
        return unsafe.compareAndSwapInt(this, valueOffset, e, u);
    }

    /**
     * value的set方法
     */
    public final void set(boolean newValue) {
        value = newValue ? 1 : 0;
    }

    /**
     * 以普通形式修改值，不保证修改被其他线程看到
     */
    public final void lazySet(boolean newValue) {
        int v = newValue ? 1 : 0;
        unsafe.putOrderedInt(this, valueOffset, v);
    }

    /**
     * 获得之前的值，并将值修改为新值，采用自旋CAS实现
     */
    public final boolean getAndSet(boolean newValue) {
        boolean prev;
        do {
            prev = get();
        } while (!compareAndSet(prev, newValue));
        return prev;
    }

    public String toString() {
        return Boolean.toString(get());
    }

}
