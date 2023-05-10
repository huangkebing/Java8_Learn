package java.util.function;

/**
 * int类型Function接口
 */
@FunctionalInterface
public interface IntFunction<R> {

    R apply(int value);
}
