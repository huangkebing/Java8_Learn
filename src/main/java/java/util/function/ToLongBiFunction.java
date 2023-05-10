package java.util.function;

/**
 * 返回结果为long的biFunction
 */
@FunctionalInterface
public interface ToLongBiFunction<T, U> {

    long applyAsLong(T t, U u);
}
