package java.util.concurrent.locks;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 可重入的读写锁
 */
public class ReentrantReadWriteLock implements ReadWriteLock, java.io.Serializable {
    private static final long serialVersionUID = -6992448646407690164L;
    /** 读锁 */
    private final ReadLock readerLock;
    /** 写锁 */
    private final WriteLock writerLock;
    /** 同步对象实例 */
    final Sync sync;

    /**
     * 无参构造，创建非公平锁
     */
    public ReentrantReadWriteLock() {
        this(false);
    }

    /**
     * 创建一个读写锁，有参构造，入参为true，则创建公平锁
     */
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    public WriteLock writeLock() { return writerLock; }
    public ReadLock  readLock()  { return readerLock; }

    abstract static class Sync extends java.util.concurrent.locks.AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 6317671515068378041L;

        static final int SHARED_SHIFT   = 16;
        // 65536   1 0000 0000 0000 0000
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
        // 65535   1111 1111 1111 1111
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
        // 65535   1111 1111 1111 1111
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

        /** state高16位为读锁信息，取读锁数量 要右移16位  */
        static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
        /** 取state低16位，写锁信息  */
        static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }

        // 每个线程的读锁计数器
        static final class HoldCounter {
            int count = 0;
            final long tid = getThreadId(Thread.currentThread());//使用tid，避免垃圾保留
        }

        static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter> {
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

        /**
         * The number of reentrant read locks held by current thread.
         * Initialized only in constructor and readObject.
         * Removed whenever a thread's read hold count drops to 0.
         */
        private transient ThreadLocalHoldCounter readHolds;

        // 最后一个获得读锁的线程的计数器
        private transient HoldCounter cachedHoldCounter;

        /**
         * 第一个读锁线程，以及其holdCount
         */
        private transient Thread firstReader = null;
        private transient int firstReaderHoldCount;

        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState()); // ensures visibility of readHolds
        }

        /*
         * Acquires and releases use the same code for fair and
         * nonfair locks, but differ in whether/how they allow barging
         * when queues are non-empty.
         */

        /**
         * Returns true if the current thread, when trying to acquire
         * the read lock, and otherwise eligible to do so, should block
         * because of policy for overtaking other waiting threads.
         */
        abstract boolean readerShouldBlock();

        /**
         * 公平锁需要判断阻塞队列中是否还有排在前面的线程，非公平永远返回false
         */
        abstract boolean writerShouldBlock();

        /*
         * 尝试以独占模式释放锁
         */
        protected final boolean tryRelease(int releases) {
            // 如果当前锁非独占，抛出异常
            if (!isHeldExclusively()) throw new IllegalMonitorStateException();
            // 独占锁state在低16位，可以直接减
            int nextc = getState() - releases;
            // 如果减后的结果，独占锁count为0，则视为释放锁成功，返回true
            boolean free = exclusiveCount(nextc) == 0;
            if (free) setExclusiveOwnerThread(null);
            setState(nextc);
            return free;
        }

        /**
         * 以独占模式尝试获得锁，即获得写锁
         */
        protected final boolean tryAcquire(int acquires) {
            Thread current = Thread.currentThread();
            int c = getState();
            int w = exclusiveCount(c);
            if (c != 0) {
                // 如果state不为0，但独占锁次数为0，表示现在被读锁占用；或者写锁被其他线程占用，获取失败
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                // 如果当前线程占有写锁，获取锁后，次数大于MAX，抛出错误
                if (w + exclusiveCount(acquires) > MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                // 否则，添加次数，返回true
                setState(c + acquires);
                return true;
            }
            // 如果state为0，且获得写锁无需block，且CAS state成功，则设置拥有线程；任意一个条件不符合，则返回false
            if (writerShouldBlock() || !compareAndSetState(c, c + acquires))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        protected final boolean tryReleaseShared(int unused) {
            Thread current = Thread.currentThread();
            if (firstReader == current) {
                // assert firstReaderHoldCount > 0;
                if (firstReaderHoldCount == 1)
                    firstReader = null;
                else
                    firstReaderHoldCount--;
            } else {
                HoldCounter rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current))
                    rh = readHolds.get();
                int count = rh.count;
                if (count <= 1) {
                    readHolds.remove();
                    if (count <= 0)
                        throw unmatchedUnlockException();
                }
                --rh.count;
            }
            for (;;) {
                int c = getState();
                int nextc = c - SHARED_UNIT;
                if (compareAndSetState(c, nextc))
                    // Releasing the read lock has no effect on readers,
                    // but it may allow waiting writers to proceed if
                    // both read and write locks are now free.
                    return nextc == 0;
            }
        }

        private IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException(
                "attempt to unlock read lock, not locked by current thread");
        }

        protected final int tryAcquireShared(int unused) {
            Thread current = Thread.currentThread();
            int c = getState();
            // 如果写锁被占用，且占用线程不是当前线程，获取失败，返回-1
            if (exclusiveCount(c) != 0 && getExclusiveOwnerThread() != current)
                return -1;
            // 获得共享锁state
            int r = sharedCount(c);
            // 如果可以获得读锁，且读锁拥有次数小于最大值，且CAS交换state成功，视为获得读锁
            if (!readerShouldBlock() && r < MAX_COUNT && compareAndSetState(c, c + SHARED_UNIT)) {
                // 如果r为0，线程设为firstReader，holdCount为1
                if (r == 0) {
                    firstReader = current;
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {
                    // 如果firstReader是当前线程，但r不为0，holdCount+1
                    firstReaderHoldCount++;
                } else {
                    HoldCounter rh = cachedHoldCounter;
                    // 如果cache不属于当前线程
                    if (rh == null || rh.tid != getThreadId(current))
                        // 从ThreadLocal中获取，若不存在会初始化一个count为0的计数器
                        cachedHoldCounter = rh = readHolds.get();
                    else if (rh.count == 0)
                        readHolds.set(rh);
                    rh.count++; // count+1
                }
                return 1;
            }
            // 如果上面的简易版尝试失败，执行完整链路
            return fullTryAcquireShared(current);
        }

        /**
         * Full version of acquire for reads, that handles CAS misses
         * and reentrant reads not dealt with in tryAcquireShared.
         */
        final int fullTryAcquireShared(Thread current) {
            /*
             * This code is in part redundant with that in
             * tryAcquireShared but is simpler overall by not
             * complicating tryAcquireShared with interactions between
             * retries and lazily reading hold counts.
             */
            HoldCounter rh = null;
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0) {
                    // 独占锁被其他线程占用，返回-1
                    if (getExclusiveOwnerThread() != current)
                        return -1;
                } else if (readerShouldBlock()) {
                    // Make sure we're not acquiring read lock reentrantly
                    if (firstReader == current) {
                        // assert firstReaderHoldCount > 0;
                    } else {
                        if (rh == null) {
                            rh = cachedHoldCounter;
                            if (rh == null || rh.tid != getThreadId(current)) {
                                rh = readHolds.get();
                                if (rh.count == 0)
                                    readHolds.remove();
                            }
                        }
                        if (rh.count == 0)
                            return -1;
                    }
                }
                if (sharedCount(c) == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (sharedCount(c) == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        if (rh == null)
                            rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                        cachedHoldCounter = rh; // cache for release
                    }
                    return 1;
                }
            }
        }

        /**
         * 尝试获得写锁，和非公平版的tryAcquire一致
         */
        final boolean tryWriteLock() {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c != 0) {
                int w = exclusiveCount(c);
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                if (w == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
            }
            if (!compareAndSetState(c, c + 1))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        /**
         * Performs tryLock for read, enabling barging in both modes.
         * This is identical in effect to tryAcquireShared except for
         * lack of calls to readerShouldBlock.
         */
        final boolean tryReadLock() {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0 &&
                    getExclusiveOwnerThread() != current)
                    return false;
                int r = sharedCount(c);
                if (r == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (r == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        HoldCounter rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            cachedHoldCounter = rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                    }
                    return true;
                }
            }
        }

        /**
         * 判断当前线程是否拥有写锁
         */
        protected final boolean isHeldExclusively() {
            // true == 拥有
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // 获得写锁的拥有线程
        final Thread getOwner() {
            // state 低16位 是否为0，为0返回null，否则返回占有线程
            return ((exclusiveCount(getState()) == 0) ? null : getExclusiveOwnerThread());
        }

        // 取state 高16位
        final int getReadLockCount() {
            return sharedCount(getState());
        }

        // state 低16位 是否为0，为0表示写锁空闲
        final boolean isWriteLocked() {
            return exclusiveCount(getState()) != 0;
        }

        // 如果是当前拥有线程，获得独占锁拥有次数，否则返回0
        final int getWriteHoldCount() {
            return isHeldExclusively() ? exclusiveCount(getState()) : 0;
        }

        final int getReadHoldCount() {
            // 如果独占锁次数为0，返回0
            if (getReadLockCount() == 0) return 0;

            Thread current = Thread.currentThread();
            if (firstReader == current) return firstReaderHoldCount;

            HoldCounter rh = cachedHoldCounter;
            if (rh != null && rh.tid == getThreadId(current))
                return rh.count;

            int count = readHolds.get().count;
            if (count == 0) readHolds.remove();
            return count;
        }

        /**
         * 反序列化方法
         */
        private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            readHolds = new ThreadLocalHoldCounter();
            setState(0);
        }

        // 获得state字段
        final int getCount() { return getState(); }
    }

    /**
     * sync 非公平版本
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -8159625535654395037L;
        // 非公平锁，写锁可以插队
        final boolean writerShouldBlock() {
            return false;
        }
        // 如果第一个等待结点是以独占模式等待，则需要block，否则不需要
        final boolean readerShouldBlock() {
            return apparentlyFirstQueuedIsExclusive();
        }
    }

    /**
     * sync 公平版本
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -2274990926593161451L;
        final boolean writerShouldBlock() {
            return hasQueuedPredecessors();
        }
        final boolean readerShouldBlock() {
            return hasQueuedPredecessors();
        }
    }

    /**
     * 读锁，在ReentrantReadWriteLock类构造方法中实例化
     */
    public static class ReadLock implements java.util.concurrent.locks.Lock, java.io.Serializable {
        private static final long serialVersionUID = -5992448646407690164L;

        private final Sync sync;
        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * 以共享模式获得锁，即获得读锁
         */
        public void lock() {
            sync.acquireShared(1);
        }

        /**
         * 以共享模式可中断的获得锁
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        /**
         * 尝试获得读锁
         */
        public boolean tryLock() {
            return sync.tryReadLock();
        }

        /**
         * 给定时间内尝试获得锁，时间结束还没有获得锁，返回false
         */
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

        /**
         * 释放读锁
         */
        public void unlock() {
            sync.releaseShared(1);
        }

        /**
         * 读锁不支持condition
         */
        public java.util.concurrent.locks.Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        public String toString() {
            int r = sync.getReadLockCount();
            return super.toString() + "[Read locks = " + r + "]";
        }
    }

    /**
     * 写锁，在ReentrantReadWriteLock类构造方法中实例化
     */
    public static class WriteLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -4992448646407690164L;

        private final Sync sync;
        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * 以独占模式获得锁，即写锁
         */
        public void lock() {
            sync.acquire(1);
        }

        /**
         * 以独占模式可中断的获得锁
         */
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        /**
         * 尝试以独占模式获得锁，非公平
         */
        public boolean tryLock() {
            return sync.tryWriteLock();
        }

        /**
         * 在给定时间内，尝试以独占模式获得锁，时间到后还未获得锁，返回false
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

        public String toString() {
            Thread o = sync.getOwner();
            return super.toString() + ((o == null) ? "[Unlocked]" : "[Locked by thread " + o.getName() + "]");
        }

        /**
         * 查询写锁是否被当前线程拥有
         */
        public boolean isHeldByCurrentThread() {
            return sync.isHeldExclusively();
        }

        /**
         * 获得写锁拥有次数，若非拥有线程，则为0
         */
        public int getHoldCount() {
            return sync.getWriteHoldCount();
        }
    }

    /**
     * 查询是否公平锁，true=公平锁
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 获得写锁的拥有线程，没有则返回null
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * 查询读锁的拥有次数
     */
    public int getReadLockCount() {
        return sync.getReadLockCount();
    }

    /**
     * 查询读写锁，写锁是否空闲，true表示写锁不空闲
     */
    public boolean isWriteLocked() {
        return sync.isWriteLocked();
    }

    /**
     * 查询写锁是否被当前线程占用，true表示是
     */
    public boolean isWriteLockedByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * 查询当前线程，获得独占锁拥有次数
     */
    public int getWriteHoldCount() {
        return sync.getWriteHoldCount();
    }

    /**
     * 查询当前线程，获得读锁的拥有次数
     */
    public int getReadHoldCount() {
        return sync.getReadHoldCount();
    }

    /**
     * 返回阻塞队列中，等待写锁的线程集合
     */
    protected Collection<Thread> getQueuedWriterThreads() {
        return sync.getExclusiveQueuedThreads();
    }

    /**
     * 返回阻塞队列中，等待读锁的线程集合
     */
    protected Collection<Thread> getQueuedReaderThreads() {
        return sync.getSharedQueuedThreads();
    }

    /**
     * 查询是否有线程在等待获得写锁或读锁
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 给定thread，查询是否在等待获得写锁或者读锁
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * 获得等待读锁或写锁的队列长度
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 返回阻塞队列中，等待写锁或者读锁的线程集合
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 给定condition，返回是否有等待线程
     */
    public boolean hasWaiters(java.util.concurrent.locks.Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 给定condition，返回等待队列长度
     */
    public int getWaitQueueLength(java.util.concurrent.locks.Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * 给定condition，返回等待队列中的线程集合
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    public String toString() {
        int c = sync.getCount();
        int w = Sync.exclusiveCount(c);
        int r = Sync.sharedCount(c);
        return super.toString() + "[Write locks = " + w + ", Read locks = " + r + "]";
    }

    /**
     * 返回给定线程的线程id(tid字段)
     */
    static final long getThreadId(Thread thread) {
        return UNSAFE.getLongVolatile(thread, TID_OFFSET);
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long TID_OFFSET; //Thread类中tid变量的内存偏移量
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            TID_OFFSET = UNSAFE.objectFieldOffset(tk.getDeclaredField("tid"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}