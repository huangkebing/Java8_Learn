package java.util.function;

/**
 * 返回结果为int的biFunction
 */
@FunctionalInterface
public interface ToIntBiFunction<T, U> {

    int applyAsInt(T t, U u);
}
