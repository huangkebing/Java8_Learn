package java.util.function;

import java.util.Objects;

/**
 * 单元素Function接口
 */
@FunctionalInterface
public interface Function<T, R> {

    /**
     * 给定入参T 返回R
     */
    R apply(T t);

    /**
     * 在apply之前执行，给定入参为（入参为V返回为T的function类）
     */
    default <V> Function<V, R> compose(Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }

    /**
     * 在apply之后执行，给定入参为（入参为R返回为V的function类）
     */
    default <V> Function<T, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }

    /**
     * 返回本身
     */
    static <T> Function<T, T> identity() {
        return t -> t;
    }
}
