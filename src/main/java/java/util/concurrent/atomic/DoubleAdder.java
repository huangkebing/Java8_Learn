package java.util.concurrent.atomic;

import java.io.Serializable;

/**
 * double类型计数器
 */
public class DoubleAdder extends java.util.concurrent.atomic.Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    public DoubleAdder() {
    }

    /**
     * 计数器累加x
     */
    public void add(double x) {
        Cell[] as; long b, v; int m; Cell a;
        // cell为null，cas base字段成功，直接结束
        if ((as = cells) != null || !casBase(b = base, Double.doubleToRawLongBits(Double.longBitsToDouble(b) + x))) {
            boolean uncontended = true;
            // 和longAdder的逻辑一致，cell为null或size为0，或者该节点为null，或cas失败，进度doubleAccumulate方法
            if (as == null || (m = as.length - 1) < 0 ||
                (a = as[getProbe() & m]) == null ||
                !(uncontended = a.cas(v = a.value, Double.doubleToRawLongBits(Double.longBitsToDouble(v) + x))))
                doubleAccumulate(x, null, uncontended);
        }
    }

    /**
     * base 和cell 先通过Double.longBitsToDouble()方法转化回double，再累加
     */
    public double sum() {
        Cell[] as = cells; Cell a;
        double sum = Double.longBitsToDouble(base);
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += Double.longBitsToDouble(a.value);
            }
        }
        return sum;
    }

    public void reset() {
        Cell[] as = cells; Cell a;
        base = 0L; // relies on fact that double 0 must have same rep as long
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    a.value = 0L;
            }
        }
    }

    public double sumThenReset() {
        Cell[] as = cells; Cell a;
        double sum = Double.longBitsToDouble(base);
        base = 0L;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    long v = a.value;
                    a.value = 0L;
                    sum += Double.longBitsToDouble(v);
                }
            }
        }
        return sum;
    }

    public String toString() {
        return Double.toString(sum());
    }

    public double doubleValue() {
        return sum();
    }

    public long longValue() {
        return (long)sum();
    }

    public int intValue() {
        return (int)sum();
    }

    public float floatValue() {
        return (float)sum();
    }

    /**
     * 序列化代理类
     */
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;

        private final double value;

        SerializationProxy(DoubleAdder a) {
            value = a.sum();
        }

        private Object readResolve() {
            DoubleAdder a = new DoubleAdder();
            a.base = Double.doubleToRawLongBits(value);
            return a;
        }
    }

    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    private void readObject(java.io.ObjectInputStream s) throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Proxy required");
    }
}
