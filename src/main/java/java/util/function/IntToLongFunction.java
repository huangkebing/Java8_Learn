package java.util.function;

/**
 * function 入参为int 返回值为long
 */
@FunctionalInterface
public interface IntToLongFunction {

    long applyAsLong(int value);
}
