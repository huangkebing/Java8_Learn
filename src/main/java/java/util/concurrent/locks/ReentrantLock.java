package java.util.concurrent.locks;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * ReentrantLock，相比于synchronized有如下区别：
 * 1. 支持公平锁，通过构造器可以选择创建公平锁还是非公平锁
 * 2. 可以查询一些关于锁的信息
 * 3. 可以结合Condition做到线程精准唤醒
 * 4. 可中断获得锁
 */
public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;

    // 实现同步的实例
    private final Sync sync;

    abstract static class Sync extends java.util.concurrent.locks.AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        abstract void lock();

        /**
         * tryAcquire非公平版本，如果锁是空闲的，直接尝试CAS，不管阻塞队列中的情况
         */
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                // 如果当前锁是空闲的，直接尝试CAS获取锁
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }

        /**
         * 尝试释放锁
         */
        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            // 如果不是拥有线程，抛出异常
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            // 若state-releases==0，则返回true，否则返回false
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }

        // 查看当前线程是否是拥有线程
        protected final boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // 返回condition
        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // 返回拥有线程，没有则为null
        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }
        // 返回拥有数量，即state变量值。如果没有拥有线程，返回0
        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }
        // 返回锁是否空闲，true=不空闲
        final boolean isLocked() {
            return getState() != 0;
        }

        /**
         * 反序列化方法
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0);
        }
    }

    /**
     * 非公平锁实现类
     * 和公平锁最大的区别是：获取锁时不保证先后顺序，具体体现在以下几处：
     * 1. 执行lock方法是，非公平锁直接尝试CAS获取锁
     * 2. tryAcquire方法，公平锁需要阻塞队列为空，或者为第一个节点，而非公平锁没有此限制
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        /**
         * 尝试CAS state获得锁，失败则执行acquire方法
         */
        final void lock() {
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }

    /**
     * 公平锁实现类
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        // lock直接调用acquire方法
        final void lock() {
            acquire(1);
        }

        /**
         * tryAcquire 的公平版本。除非重入或没有等待线程或者是第一个，否则不获得锁。
         */
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            // 当前锁的状态
            int c = getState();
            if (c == 0) {
                // 当前锁空闲，且队列中没有等待线程或者当前线程为第一个，且CAS修改状态成功，则获得锁
                if (!hasQueuedPredecessors() && compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            // 如果是拥有者，修改state字段为c+acquires
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            // 否则，获取锁失败
            return false;
        }
    }

    /**
     * 无参构造器，默认创建非公平锁，性能更好
     */
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    /**
     * 有参构造，传入true则创建公平锁
     */
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * 获得锁方法，如果当前被别的线程占有，就会阻塞
     */
    public void lock() {
        sync.lock();
    }

    /**
     * 获得锁方法，如果有中断标记则不获得锁而是抛出异常
     */
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     * 仅当调用时没有被另一个线程持有时才获取锁，如果当前被别的线程占有，那么返回false
     */
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * 获得锁方法，线程有中断标记抛出异常，超过时间未获得锁则返回false
     */
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * 释放锁
     */
    public void unlock() {
        sync.release(1);
    }

    /**
     * 获得Condition对象
     */
    public java.util.concurrent.locks.Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * 查询当前线程持有该锁的次数
     */
    public int getHoldCount() {
        return sync.getHoldCount();
    }

    /**
     * 查询锁是否被当前线程占有
     */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * 查询锁是否是被占有状态
     */
    public boolean isLocked() {
        return sync.isLocked();
    }

    /**
     * 查询锁是否是公平锁，true=公平锁，false=非公平锁
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 返回当前锁的拥有线程
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * 阻塞队列中是否有线程正在等待
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 给定一个线程，判断是否在阻塞队列中等待
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * 获得阻塞队列的长度
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 获得阻塞队列中的线程list
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 给定condition，查询是否有线程正在等待
     */
    public boolean hasWaiters(java.util.concurrent.locks.Condition condition) {
        if (condition == null) throw new NullPointerException();
        if (!(condition instanceof java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 给定condition，查询等待线程的数量
     */
    public int getWaitQueueLength(java.util.concurrent.locks.Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 给定condition，获得等待的线程list
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                                   "[Unlocked]" :
                                   "[Locked by thread " + o.getName() + "]");
    }
}
