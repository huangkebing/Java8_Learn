package java.util.function;

/**
 * 返回结果为Double的BiFunction
 */
@FunctionalInterface
public interface ToDoubleBiFunction<T, U> {

    double applyAsDouble(T t, U u);
}
