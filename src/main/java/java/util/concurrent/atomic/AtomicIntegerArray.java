package java.util.concurrent.atomic;

import sun.misc.Unsafe;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * 原子类 int数组（初始化有一些区别，后续使用AtomicInteger类似）
 */
public class AtomicIntegerArray implements java.io.Serializable {
    private static final long serialVersionUID = 2862133569453604235L;

    private static final Unsafe unsafe = Unsafe.getUnsafe();
    // 获得数组第一个元素的偏移量
    private static final int base = unsafe.arrayBaseOffset(int[].class);
    // 偏移量计算单位，值为log2(scale)，int 的scale=4，则shift=2
    private static final int shift;
    private final int[] array;

    static {
        // 获得数组的增量偏移量 int为4个字节，所以scale=4
        int scale = unsafe.arrayIndexScale(int[].class);
        // 校验scale是否为2的幂次
        if ((scale & (scale - 1)) != 0)
            throw new Error("data type scale not a power of two");
        // numberOfLeadingZeros 获得该数二进制高位0的个数，为29，shift=2，即计算scale是2的几次幂
        shift = 31 - Integer.numberOfLeadingZeros(scale);
    }

    private long checkedByteOffset(int i) {
        // 数组范围检查
        if (i < 0 || i >= array.length)
            throw new IndexOutOfBoundsException("index " + i);
        // 通过检查，则计算偏移量
        return byteOffset(i);
    }

    // 计算i为位置的元素在内存中的偏移量
    private static long byteOffset(int i) {
        return ((long) i << shift) + base;
    }

    /**
     * 有参构造器1，给定长度
     */
    public AtomicIntegerArray(int length) {
        array = new int[length];
    }

    /**
     * 有参构造器2，给定一个数组
     */
    public AtomicIntegerArray(int[] array) {
        this.array = array.clone();
    }

    /**
     * 获得长度
     */
    public final int length() {
        return array.length;
    }

    /**
     * 获得i位置的元素，调用checkedByteOffset，校验并获得偏移量
     */
    public final int get(int i) {
        return getRaw(checkedByteOffset(i));
    }

    /*
    * 根据偏移量，通过unsafe取值
    */
    private int getRaw(long offset) {
        return unsafe.getIntVolatile(array, offset);
    }

    /**
     * 根据偏移量，通过unsafe更新值
     */
    public final void set(int i, int newValue) {
        unsafe.putIntVolatile(array, checkedByteOffset(i), newValue);
    }

    /**
     * 普通方式设置，不保证其他线程可见
     */
    public final void lazySet(int i, int newValue) {
        unsafe.putOrderedInt(array, checkedByteOffset(i), newValue);
    }

    /**
     * 获得原值，并更新
     */
    public final int getAndSet(int i, int newValue) {
        return unsafe.getAndSetInt(array, checkedByteOffset(i), newValue);
    }

    /**
     * CAS
     */
    public final boolean compareAndSet(int i, int expect, int update) {
        return compareAndSetRaw(checkedByteOffset(i), expect, update);
    }

    // CAS调用内部方法，在计算出偏移量后进行CAS
    private boolean compareAndSetRaw(long offset, int expect, int update) {
        return unsafe.compareAndSwapInt(array, offset, expect, update);
    }

    public final boolean weakCompareAndSet(int i, int expect, int update) {
        return compareAndSet(i, expect, update);
    }

    /**
     * 获得原值，并更新原值+1
     */
    public final int getAndIncrement(int i) {
        return getAndAdd(i, 1);
    }

    /**
     * 获得原值，并更新原值-1
     */
    public final int getAndDecrement(int i) {
        return getAndAdd(i, -1);
    }

    /**
     * 获得原值，并更新原值+delta
     */
    public final int getAndAdd(int i, int delta) {
        return unsafe.getAndAddInt(array, checkedByteOffset(i), delta);
    }

    /**
     * 更新原值+1，并获得新值
     */
    public final int incrementAndGet(int i) {
        return getAndAdd(i, 1) + 1;
    }

    /**
     * 更新原值-1，并获得新值
     */
    public final int decrementAndGet(int i) {
        return getAndAdd(i, -1) - 1;
    }

    /**
     * 更新原值+delta，并获得新值
     */
    public final int addAndGet(int i, int delta) {
        return getAndAdd(i, delta) + delta;
    }


    /**
     * 给定IntUnaryOperator，原值按照该规则计算结果，更新结果，并返回原值
     */
    public final int getAndUpdate(int i, IntUnaryOperator updateFunction) {
        long offset = checkedByteOffset(i);
        int prev, next;
        do {
            prev = getRaw(offset);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSetRaw(offset, prev, next));
        return prev;
    }

    /**
     * 给定IntUnaryOperator，原值按照该规则计算结果，更新结果，并返回新值
     */
    public final int updateAndGet(int i, IntUnaryOperator updateFunction) {
        long offset = checkedByteOffset(i);
        int prev, next;
        do {
            prev = getRaw(offset);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSetRaw(offset, prev, next));
        return next;
    }

    /**
     * 给定x和accumulatorFunction，原值按照该规则计算结果，更新结果，并返回原值
     */
    public final int getAndAccumulate(int i, int x,
                                      IntBinaryOperator accumulatorFunction) {
        long offset = checkedByteOffset(i);
        int prev, next;
        do {
            prev = getRaw(offset);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSetRaw(offset, prev, next));
        return prev;
    }

    /**
     * 给定x和accumulatorFunction，原值按照该规则计算结果，更新结果，并返回新值
     */
    public final int accumulateAndGet(int i, int x,
                                      IntBinaryOperator accumulatorFunction) {
        long offset = checkedByteOffset(i);
        int prev, next;
        do {
            prev = getRaw(offset);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSetRaw(offset, prev, next));
        return next;
    }

    public String toString() {
        int iMax = array.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(getRaw(byteOffset(i)));
            if (i == iMax)
                return b.append(']').toString();
            b.append(',').append(' ');
        }
    }

}
