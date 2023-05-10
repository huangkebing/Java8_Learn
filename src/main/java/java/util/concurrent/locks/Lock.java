package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * Lock接口
 */
public interface Lock {

    /**
     * 获得锁，如果当前被别的线程占有，就会阻塞
     */
    void lock();

    /**
     * 除非当前线程是interrupted，否则获取锁
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * 仅当调用时它是空闲的时才获取锁，如果当前被别的线程占有，那么返回false
     */
    boolean tryLock();

    /**
     * 带时间限制的tryLock()，若被别的线程占有，则等待至多unit时间，时间到了还没获取返回false
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 释放锁
     */
    void unlock();

    /**
     * 获得一个Condition实例
     */
    Condition newCondition();
}
