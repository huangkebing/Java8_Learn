package java.util.concurrent.locks;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Condition接口，定义了控制线程等待和唤醒的方法
 */
public interface Condition {

    /**
     * 无期限等待
     */
    void await() throws InterruptedException;

    /**
     * 等待直到被打断
     */
    void awaitUninterruptibly();

    /**
     * 等待给定的时长，单位纳秒
     */
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * 等待给定的时长
     */
    boolean await(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 等待直到给定的时间
     */
    boolean awaitUntil(Date deadline) throws InterruptedException;

    /**
     * 唤醒一个等待线程
     */
    void signal();

    /**
     * 唤醒所有等待线程
     */
    void signalAll();
}
