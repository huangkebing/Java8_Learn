package java.util.function;

import java.util.Objects;

/**
 * int类型 判断型接口
 */
@FunctionalInterface
public interface IntPredicate {

    boolean test(int value);

    default IntPredicate and(IntPredicate other) {
        Objects.requireNonNull(other);
        return (value) -> test(value) && other.test(value);
    }

    default IntPredicate negate() {
        return (value) -> !test(value);
    }

    default IntPredicate or(IntPredicate other) {
        Objects.requireNonNull(other);
        return (value) -> test(value) || other.test(value);
    }
}
