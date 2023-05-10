package java.util.function;

/**
 * 双元素消费者接口，第二个元素必须为double，无andThen方法
 */
@FunctionalInterface
public interface ObjDoubleConsumer<T> {

    void accept(T t, double value);
}
