package java.util.function;

import java.util.Objects;

/**
 * double类型消费者接口，在DoubleStream中使用
 */
@FunctionalInterface
public interface DoubleConsumer {

    void accept(double value);

    default DoubleConsumer andThen(DoubleConsumer after) {
        Objects.requireNonNull(after);
        return (double t) -> { accept(t); after.accept(t); };
    }
}
