package java.util.function;

import java.util.Objects;

/**
 * int类型消费者接口，在IntStream中使用
 */
@FunctionalInterface
public interface IntConsumer {

    void accept(int value);

    default IntConsumer andThen(IntConsumer after) {
        Objects.requireNonNull(after);
        return (int t) -> { accept(t); after.accept(t); };
    }
}
