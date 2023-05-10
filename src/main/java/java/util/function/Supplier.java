package java.util.function;

/**
 * 单元素 生产者接口
 */
@FunctionalInterface
public interface Supplier<T> {

    T get();
}
