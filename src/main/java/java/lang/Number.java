package java.lang;

public abstract class Number implements java.io.Serializable {
    /**
     * 获得int值
     */
    public abstract int intValue();

    /**
     * 获得long值
     */
    public abstract long longValue();

    /**
     * 获得float值
     */
    public abstract float floatValue();

    /**
     * 获得double值
     */
    public abstract double doubleValue();

    /**
     * 获得byte值
     */
    public byte byteValue() {
        return (byte)intValue();
    }

    /**
     * 获得short值
     */
    public short shortValue() {
        return (short)intValue();
    }

    private static final long serialVersionUID = -8742448824652078965L;
}
