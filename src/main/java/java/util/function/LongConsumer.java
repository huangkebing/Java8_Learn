package java.util.function;

import java.util.Objects;

/**
 * long类型消费者接口，在LongStream中使用
 */
@FunctionalInterface
public interface LongConsumer {

    void accept(long value);

    default LongConsumer andThen(LongConsumer after) {
        Objects.requireNonNull(after);
        return (long t) -> { accept(t); after.accept(t); };
    }
}
