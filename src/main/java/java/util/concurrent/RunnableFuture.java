package java.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * 一个{@link Runnable}的{@link java.util.concurrent.Future}
 * 执行{@code run}方法来完成{@code Future}，并允许访问结果
 *
 * @param <V> Future的{@code get}方法返回的类型
 * @author Doug Lea
 * @see FutureTask
 * @see Executor
 * @since 1.6
 */
public interface RunnableFuture<V> extends Runnable, Future<V> {
    /**
     * 将future的状态修改为完成，若被取消了则不修改
     */
    void run();
}
