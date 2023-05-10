package java.util.function;

import java.util.Objects;

/**
 * Long型 判断接口
 */
@FunctionalInterface
public interface LongPredicate {

    boolean test(long value);

    default LongPredicate and(LongPredicate other) {
        Objects.requireNonNull(other);
        return (value) -> test(value) && other.test(value);
    }

    default LongPredicate negate() {
        return (value) -> !test(value);
    }

    default LongPredicate or(LongPredicate other) {
        Objects.requireNonNull(other);
        return (value) -> test(value) || other.test(value);
    }
}
