package java.util.function;

/**
 * function 入参double 返回值int
 */
@FunctionalInterface
public interface DoubleToIntFunction {

    int applyAsInt(double value);
}
