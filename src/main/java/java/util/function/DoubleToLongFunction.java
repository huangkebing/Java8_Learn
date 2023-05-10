package java.util.function;

/**
 * function 入参double 返回值long
 */
@FunctionalInterface
public interface DoubleToLongFunction {

    long applyAsLong(double value);
}
