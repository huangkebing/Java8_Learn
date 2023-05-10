package java.util.function;

/**
 * long 双元操作符
 */
@FunctionalInterface
public interface LongBinaryOperator {

    long applyAsLong(long left, long right);
}
