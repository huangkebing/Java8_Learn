package java.util.function;

import java.util.Objects;

/**
 * 单个元素，消费方法
 * 如List.foreach中使用了此接口
 */
@FunctionalInterface
public interface Consumer<T> {

    void accept(T t);

    default Consumer<T> andThen(Consumer<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> { accept(t); after.accept(t); };
    }
}
