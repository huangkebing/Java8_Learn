package java.util.function;

/**
 * int 双元 操作符
 */
@FunctionalInterface
public interface IntBinaryOperator {

    int applyAsInt(int left, int right);
}
