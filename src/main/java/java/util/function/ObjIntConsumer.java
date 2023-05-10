package java.util.function;

/**
 * 双元素消费者接口，第二个元素必须为int，无andThen方法
 */
@FunctionalInterface
public interface ObjIntConsumer<T> {

    void accept(T t, int value);
}
