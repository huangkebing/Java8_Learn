package java.util.function;

/**
 * 返回结果为int的Function
 */
@FunctionalInterface
public interface ToIntFunction<T> {

    int applyAsInt(T value);
}
