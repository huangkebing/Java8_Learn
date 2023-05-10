package java.util.function;

/**
 * boolean类型 生产者接口
 */
@FunctionalInterface
public interface BooleanSupplier {

    boolean getAsBoolean();
}
