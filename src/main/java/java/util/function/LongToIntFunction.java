package java.util.function;

/**
 * function 入参为long 返回值为int
 */
@FunctionalInterface
public interface LongToIntFunction {

    int applyAsInt(long value);
}
