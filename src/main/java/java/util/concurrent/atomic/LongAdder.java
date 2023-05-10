package java.util.concurrent.atomic;

import java.io.Serializable;

/**
 * long类型的线程安全计数器，相比AtomicLong的计数器，本类的性能更好
 */
public class LongAdder extends java.util.concurrent.atomic.Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    public LongAdder() {
    }

    /**
     * 计数器+X
     */
    public void add(long x) {
        Cell[] as;
        long b, v;
        int m;
        Cell a;
        // 如果cell数组为null，且cas修改base字段成功，则表示已经add成功
        // 若cell数组不为null，则不修改base字段
        if ((as = cells) != null || !casBase(b = base, b + x)) {
            boolean uncontended = true;
            // 执行longAccumulate方法的条件如下，满足一个即执行：
            // 1.若cell数组为null
            // 2.若cell数组的长度为0
            // 3.若cell数组下标为getProbe() & m 的元素为null
            // 4.若CAS第三条中的元素失败
            if (as == null || (m = as.length - 1) < 0 || (a = as[getProbe() & m]) == null || !(uncontended = a.cas(v = a.value, v + x)))
                longAccumulate(x, null, uncontended);
        }
    }

    /**
     * 计数器+1
     */
    public void increment() {
        add(1L);
    }

    /**
     * 计数器-1
     */
    public void decrement() {
        add(-1L);
    }

    /**
     * cell数组和base所有值相加
     */
    public long sum() {
        Cell[] as = cells;
        Cell a;
        long sum = base;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
    }

    /**
     * 重置计数器，
     */
    public void reset() {
        Cell[] as = cells;
        Cell a;
        base = 0L;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    a.value = 0L;
            }
        }
    }

    /**
     * 获得sum值，然后重置
     */
    public long sumThenReset() {
        Cell[] as = cells; Cell a;
        long sum = base;
        base = 0L;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    sum += a.value;
                    a.value = 0L;
                }
            }
        }
        return sum;
    }

    public String toString() {
        return Long.toString(sum());
    }

    public long longValue() {
        return sum();
    }

    public int intValue() {
        return (int)sum();
    }

    public float floatValue() {
        return (float)sum();
    }

    public double doubleValue() {
        return (double)sum();
    }

    /**
     * 序列化代理类，专门用于序列化，避免在序列化中使用Striped64超类
     */
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;

        /**
         * 保存LongAdder的sum值，在构造中初始化
         */
        private final long value;

        SerializationProxy(LongAdder a) {
            value = a.sum();
        }

        /**
         * 反序列化方法
         */
        private Object readResolve() {
            LongAdder a = new LongAdder();
            a.base = value;
            return a;
        }
    }

    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    // 不能直接调用LongAdder的反序列化方法
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Proxy required");
    }

}
