package java.util.function;

/**
 * Double类型 Function接口
 */
@FunctionalInterface
public interface DoubleFunction<R> {
    
    R apply(double value);
}
