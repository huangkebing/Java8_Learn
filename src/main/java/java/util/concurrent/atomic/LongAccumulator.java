package java.util.concurrent.atomic;

import java.io.Serializable;
import java.util.function.LongBinaryOperator;

/**
 * 和LongAdder原理基本一致，两者区别如下：
 * 1. LongAdder只支持加法计算，本类支持通过LongBinaryOperator自定义计算规则，不局限于加法
 * 2. LongAdder只支持默认值为0，本类支持通过identity给定默认值
 */
public class LongAccumulator extends java.util.concurrent.atomic.Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    private final LongBinaryOperator function;
    private final long identity;

    /**
     * 有参构造器
     */
    public LongAccumulator(LongBinaryOperator accumulatorFunction,
                           long identity) {
        this.function = accumulatorFunction;
        // 默认值，LongAdder里面初始值只能为0
        base = this.identity = identity;
    }

    /**
     * 计数器+X
     */
    public void accumulate(long x) {
        Cell[] as; long b, v, r; int m; Cell a;
        // 若cell不为null，base和x计算后!=base且cas base成功，或者base和x计算后==base，则直接结束
        if ((as = cells) != null || (r = function.applyAsLong(b = base, x)) != b && !casBase(b, r)) {
            boolean uncontended = true;
            // 此条件和LongAdder一致
            if (as == null || (m = as.length - 1) < 0 ||
                (a = as[getProbe() & m]) == null ||
                !(uncontended = (r = function.applyAsLong(v = a.value, x)) == v || a.cas(v, r)))
                longAccumulate(x, function, uncontended);
        }
    }

    /**
     * 返回值，若只有base，直接返回；否则按照给定的计算规则进行计算，返回计算结果
     */
    public long get() {
        Cell[] as = cells;
        Cell a;
        long result = base;
        // 若cell为null，直接返回base
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    // 若有cell，将result和cell按照给定的计算规则计算
                    result = function.applyAsLong(result, a.value);
            }
        }
        return result;
    }

    /**
     * 重置，将base和cell都置为给定的初始值
     */
    public void reset() {
        Cell[] as = cells; Cell a;
        base = identity;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    a.value = identity;
            }
        }
    }

    /**
     * 获得值并重置
     */
    public long getThenReset() {
        Cell[] as = cells; Cell a;
        long result = base;
        base = identity;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    long v = a.value;
                    a.value = identity;
                    result = function.applyAsLong(result, v);
                }
            }
        }
        return result;
    }

    public String toString() {
        return Long.toString(get());
    }

    public long longValue() {
        return get();
    }

    public int intValue() {
        return (int)get();
    }

    public float floatValue() {
        return (float)get();
    }

    public double doubleValue() {
        return (double)get();
    }

    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;

        private final long value;

        private final LongBinaryOperator function;

        private final long identity;

        SerializationProxy(LongAccumulator a) {
            function = a.function;
            identity = a.identity;
            value = a.get();
        }

        private Object readResolve() {
            LongAccumulator a = new LongAccumulator(function, identity);
            a.base = value;
            return a;
        }
    }

    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    private void readObject(java.io.ObjectInputStream s)
        throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Proxy required");
    }

}
