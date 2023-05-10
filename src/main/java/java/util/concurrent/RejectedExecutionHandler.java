package java.util.concurrent;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 当任务不能被{@link java.util.concurrent.ThreadPoolExecutor}处理时的拒绝策略
 * 在{@link java.util.concurrent.ThreadPoolExecutor} 中定义了4中拒绝策略，当然也可以实现本接口实现自己的拒绝策略
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface RejectedExecutionHandler {

    /**
     * 线程池无法处理时，调用执行
     *
     * @param r 请求执行的可运行任务
     * @param executor 任务执行者(线程池)
     * @throws RejectedExecutionException 如果没有其他措施，抛出该异常
     */
    void rejectedExecution(Runnable r, ThreadPoolExecutor executor);
}
