package java.util.concurrent;

/**
 * 线程实现类callable
 */
@FunctionalInterface
public interface Callable<V> {
    /**
     * 和Runnable接口比较：public abstract void run();
     * 看到Callable可以有返回值，可以抛出异常
     */
    V call() throws Exception;
}
