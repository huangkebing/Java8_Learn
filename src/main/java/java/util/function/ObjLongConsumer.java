package java.util.function;

/**
 * 双元素消费者接口，第二个元素必须为long，无andThen方法
 */
@FunctionalInterface
public interface ObjLongConsumer<T> {

    void accept(T t, long value);
}
