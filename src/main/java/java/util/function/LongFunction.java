package java.util.function;

/**
 * Long类型 Function接口
 */
@FunctionalInterface
public interface LongFunction<R> {

    R apply(long value);
}
