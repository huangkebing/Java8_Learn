package java.util.function;

/**
 * 返回结果为double的Function
 */
@FunctionalInterface
public interface ToDoubleFunction<T> {

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    double applyAsDouble(T value);
}
