package java.util.function;

/**
 * function 入参为int 返回值为double
 */
@FunctionalInterface
public interface IntToDoubleFunction {

    double applyAsDouble(int value);
}
