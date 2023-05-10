package java.util.function;

/**
 * function 入参为long 返回值为double
 */
@FunctionalInterface
public interface LongToDoubleFunction {

    double applyAsDouble(long value);
}


