package java.util.function;

/**
 * 返回结果为Long的Function
 */
@FunctionalInterface
public interface ToLongFunction<T> {

    long applyAsLong(T value);
}
