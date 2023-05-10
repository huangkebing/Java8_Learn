package java.util.function;

/**
 * long类型 生产者接口
 */
@FunctionalInterface
public interface LongSupplier {

    long getAsLong();
}
