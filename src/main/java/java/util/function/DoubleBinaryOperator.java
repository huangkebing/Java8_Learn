package java.util.function;

/**
 * double 二元操作类，给定两个double入参，返回一个double值
 */
@FunctionalInterface
public interface DoubleBinaryOperator {

    double applyAsDouble(double left, double right);
}
