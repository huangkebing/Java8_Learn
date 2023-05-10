package java.util.function;

import java.util.Objects;

/**
 * 两个元素的消费者接口，HashMap.foreach()中使用了此接口
 */
@FunctionalInterface
public interface BiConsumer<T, U> {

    void accept(T t, U u);

    default BiConsumer<T, U> andThen(BiConsumer<? super T, ? super U> after) {
        Objects.requireNonNull(after);

        return (l, r) -> {
            accept(l, r);
            after.accept(l, r);
        };
    }
}
