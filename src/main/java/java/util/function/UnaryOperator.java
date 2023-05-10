package java.util.function;

/**
 * 一元操作 继承了Function，区别是其入参和返回值为同一个类型
 */
@FunctionalInterface
public interface UnaryOperator<T> extends Function<T, T> {

    static <T> UnaryOperator<T> identity() {
        return t -> t;
    }
}
