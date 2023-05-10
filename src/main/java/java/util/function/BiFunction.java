package java.util.function;

import java.util.Objects;

/**
 * 双元素 Function接口
 */
@FunctionalInterface
public interface BiFunction<T, U, R> {

    /**
     * 给定入参T和U，返回值R
     */
    R apply(T t, U u);

    /**
     * 给定入参R，返回值V，没有compose方法，因为BiFunction.apply需要两个入参
     */
    default <V> BiFunction<T, U, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t, U u) -> after.apply(apply(t, u));
    }
}
