package java.util.concurrent.locks;

import java.util.concurrent.locks.Lock;

/**
 * 读写锁
 */
public interface ReadWriteLock {
    /**
     * 获得读锁
     */
    java.util.concurrent.locks.Lock readLock();

    /**
     * 获得写锁
     */
    Lock writeLock();
}
