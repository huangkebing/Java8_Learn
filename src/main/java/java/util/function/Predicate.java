package java.util.function;

import java.util.Objects;

/**
 * 单个元素判断接口，stream 的 filter方法中使用
 */
@FunctionalInterface
public interface Predicate<T> {

    boolean test(T t);

    /**
     * 与方法
     */
    default Predicate<T> and(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) && other.test(t);
    }

    /**
     * 非方法
     */
    default Predicate<T> negate() {
        return (t) -> !test(t);
    }

    /**
     * 或方法
     */
    default Predicate<T> or(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) || other.test(t);
    }

    /**
     * 等于判断
     */
    static <T> Predicate<T> isEqual(Object targetRef) {
        return (null == targetRef)
                ? Objects::isNull
                : object -> targetRef.equals(object);
    }
}
