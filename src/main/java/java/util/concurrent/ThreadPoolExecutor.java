package java.util.concurrent;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPoolExecutor extends AbstractExecutorService {
    /**
     * 主控制字段包含两个信息：
     *   工作线程数量, 表示有效线程数
     *   线程池运行状态, 指示线程池的运行状态
     *
     * runState 提供主要的线程池状态控制，取值：
     *
     *   RUNNING:  运行状态，接受新任务并处理排队的任务
     *   SHUTDOWN: 不接受新任务，但处理排队的任务
     *   STOP:     不接受新任务，不处理排队的任务，中断正在进行的任务
     *   TIDYING:  所有任务都已终止，workerCount 为零，转换到状态 TIDYING 的线程将运行 terminated() 钩子方法
     *   TERMINATED: terminated() 已执行完成
     *
     * 这5个值之间的数字，随着runState只会递增，而不会减，但不需要达到每个状态。变化时机是：
     *
     * RUNNING -> SHUTDOWN  调用了 shutdown(), 在finalize()方法中也会触发
     * (RUNNING or SHUTDOWN) -> STOP  调用了 shutdownNow()
     * SHUTDOWN -> TIDYING 当队列和池都为空时
     * STOP -> TIDYING 当池为空时
     * TIDYING -> TERMINATED 当 terminate() 钩子方法完成时
     */
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));

    /**
     * 29，{@code CAPACITY}的幂次
     */
    private static final int COUNT_BITS = Integer.SIZE - 3;
    /**
     * Integer共32位(其中1位符号位，31位存储)，在ThreadPoolExecutor中前3位存储运行状态，后29位存储任务数量
     * 0001 1111 1111 1111 1111 1111 1111 1111
     */
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;
    /**
     * 运行状态
     * 1110 0000 0000 0000 0000 0000 0000 0000
     */
    private static final int RUNNING    = -1 << COUNT_BITS;
    /**
     * 0000 0000 0000 0000 0000 0000 0000 0000
     */
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    /**
     * 0010 0000 0000 0000 0000 0000 0000 0000
     */
    private static final int STOP       =  1 << COUNT_BITS;
    /**
     * 0100 0000 0000 0000 0000 0000 0000 0000
     * tryTerminate()方法会将状态修改为TIDYING，执行terminated()方法后，修改为TERMINATED状态
     */
    private static final int TIDYING    =  2 << COUNT_BITS;
    /**
     * 0110 0000 0000 0000 0000 0000 0000 0000
     */
    private static final int TERMINATED =  3 << COUNT_BITS;

    /*-------------ctl字段操作方法，取相应信息、生成ctl-----------------*/
    /**
     * 获得运行状态，~CAPACITY=RUNNING,即取高3位，后29位置0
     */
    private static int runStateOf(int c)     { return c & ~CAPACITY; }
    /**
     * 获得目前的任务数量,即取低29位
     */
    private static int workerCountOf(int c)  { return c & CAPACITY; }
    /**
     * 将运行状态{@code rs} 和 工作线程数量{@code wc} 合到一个int中
     *
     * @param rs runState 运行状态
     * @param wc workerCount 工作线程数量
     * @return ctl的值
     */
    private static int ctlOf(int rs, int wc) { return rs | wc; }

    /*-------------------------状态判断方法----------------------------*/
    /**
     * 比较两个状态值大小，若c小于s 返回true
     * @param c 状态值1
     * @param s 状态值2
     * @return c小于s 返回true
     */
    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    /**
     * 比较两个状态值大小，若c>=s 返回true
     * @param c 状态值1
     * @param s 状态值2
     * @return c>=s 返回true
     */
    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    /**
     * 比较两个状态值c和SHUTDOWN的大小，若c小于SHUTDOWN(即为RUNNING状态)，则返回true
     *
     * @param c 状态值
     * @return c小于SHUTDOWN 返回true
     */
    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    /**
     * CAS ctl+1
     */
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    /**
     * 尝试对ctl的workerCount字段进行CAS操作，将值减1
     */
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    /**
     * Decrements the workerCount field of ctl. This is called only on
     * abrupt termination of a thread (see processWorkerExit). Other
     * decrements are performed within getTask.
     */
    private void decrementWorkerCount() {
        do {} while (! compareAndDecrementWorkerCount(ctl.get()));
    }

    /**
     * 等待执行的队列
     */
    private final BlockingQueue<Runnable> workQueue;


    private final ReentrantLock mainLock = new ReentrantLock();

    /**
     * 包含池中所有工作线程的集合, 仅在持有 mainLock 时访问
     */
    private final HashSet<Worker> workers = new HashSet<Worker>();

    /**
     * Wait condition to support awaitTermination
     */
    private final Condition termination = mainLock.newCondition();

    /**
     * 记录线程池最大的线程数量，即workers.size()的峰值
     * 读写均只能在获得mainLock后，才允许访问
     */
    private int largestPoolSize;

    /**
     * 已完成任务数量，仅在工作线程终止时将w.completedTasks加到变量中
     * 读写均只能在获得mainLock后，才允许访问
     */
    private long completedTaskCount;

    /*-------------------以下为构造器的基本入参------------------*/
    private volatile ThreadFactory threadFactory;
    private volatile java.util.concurrent.RejectedExecutionHandler handler;
    /**
     * 线程空闲时保留的最大时长，以纳秒为单位保存
     */
    private volatile long keepAliveTime;
    /**
     * 如果为true，则适用于非核心线程的相同保活策略也适用于核心线程(即空闲时间超过{@code keepAliveTime}后，被终止)
     * 当为 false（默认值）时，核心线程永远不会由于空闲而终止
     */
    private volatile boolean allowCoreThreadTimeOut;
    private volatile int corePoolSize;
    private volatile int maximumPoolSize;
    /**
     * 默认拒绝策略，为AbortPolicy
     */
    private static final java.util.concurrent.RejectedExecutionHandler defaultHandler = new AbortPolicy();

    /*-------------------线程认证使用，销毁时使用--------------------*/
    private static final RuntimePermission shutdownPerm =
        new RuntimePermission("modifyThread");

    /**
     * 执行终结器时要使用的上下文，可能null
     */
    private final AccessControlContext acc;

    /**
     * Class Worker mainly maintains interrupt control state for
     * threads running tasks, along with other minor bookkeeping.
     * This class opportunistically extends AbstractQueuedSynchronizer
     * to simplify acquiring and releasing a lock surrounding each
     * task execution.  This protects against interrupts that are
     * intended to wake up a worker thread waiting for a task from
     * instead interrupting a task being run.  We implement a simple
     * non-reentrant mutual exclusion lock rather than use
     * ReentrantLock because we do not want worker tasks to be able to
     * reacquire the lock when they invoke pool control methods like
     * setCorePoolSize.  Additionally, to suppress interrupts until
     * the thread actually starts running tasks, we initialize lock
     * state to a negative value, and clear it upon start (in
     * runWorker).
     */
    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
        private static final long serialVersionUID = 6138294804551838833L;

        /**
         * 执行此Worker任务的线程
         */
        final Thread thread;
        /** Initial task to run.  Possibly null. */
        Runnable firstTask;
        /** Per-thread task counter */
        volatile long completedTasks;

        /**
         * Creates with given first task and thread from ThreadFactory.
         * @param firstTask the first task (null if none)
         */
        Worker(Runnable firstTask) {
            // 尝试中断时，会执行tryLock方法，即通过CAS将state从0改为1，但永远不会成功，因此不会被中断
            setState(-1);
            this.firstTask = firstTask;
            // 通过线程工厂，创建线程，其中的Runnable为当前对象
            this.thread = getThreadFactory().newThread(this);
        }

        /**
         * 线程的run方法，主逻辑由{@link #runWorker(Worker)}执行
         */
        public void run() {
            runWorker(this);
        }

        // Lock methods
        //
        // The value 0 represents the unlocked state.
        // The value 1 represents the locked state.

        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void lock()        { acquire(1); }
        public boolean tryLock()  { return tryAcquire(1); }
        public void unlock()      { release(1); }
        public boolean isLocked() { return isHeldExclusively(); }

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }

    /*
     * Methods for setting control state
     */

    /**
     * shutdown和shutdownNow方法调用，用于将状态修改为SHUTDOWN 或者 STOP
     * 将 runState 转换为给定的目标，或者至少已经是给定的目标
     *
     * @param targetState 新的状态, SHUTDOWN 或者 STOP
     */
    private void advanceRunState(int targetState) {
        for (;;) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState) ||
                ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
                break;
        }
    }

    /**
     * Transitions to TERMINATED state if either (SHUTDOWN and pool
     * and queue empty) or (STOP and pool empty).  If otherwise
     * eligible to terminate but workerCount is nonzero, interrupts an
     * idle worker to ensure that shutdown signals propagate. This
     * method must be called following any action that might make
     * termination possible -- reducing worker count or removing tasks
     * from the queue during shutdown. The method is non-private to
     * allow access from ScheduledThreadPoolExecutor.
     */
    final void tryTerminate() {
        for (;;) {
            int c = ctl.get();
            // 如果线程池是运行状态，无需执行后面的逻辑
            // 或者是TIDYING、TERMINATED状态，说明线程池已经在终止了
            // 或者为SHUTDOWN状态但任务队列不为空，说明是shutdown方法，但仍有任务没执行完
            if (isRunning(c) ||
                runStateAtLeast(c, TIDYING) ||
                (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
                return;
            if (workerCountOf(c) != 0) { // Eligible to terminate
                interruptIdleWorkers(ONLY_ONE);
                return;
            }

            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();
                    } finally {
                        ctl.set(ctlOf(TERMINATED, 0));
                        termination.signalAll();
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }
            // else retry on failed CAS
        }
    }

    /*
     * Methods for controlling interrupts to worker threads.
     */

    /**
     * 校验线程所属权限
     * 如果有安全管理器，请确保调用者通常有权关闭线程（请参阅shutdownPerm）
     * 如果通过，另外确保调用者被允许中断每个工作线程
     * 如果 SecurityManager 对某些线程进行特殊处理，即使第一次检查通过，这也可能不是真的。
     */
    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                for (Worker w : workers)
                    security.checkAccess(w.thread);
            } finally {
                mainLock.unlock();
            }
        }
    }

    /**
     * Interrupts all threads, even if active. Ignores SecurityExceptions
     * (in which case some threads may remain uninterrupted).
     */
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Interrupts threads that might be waiting for tasks (as
     * indicated by not being locked) so they can check for
     * termination or configuration changes. Ignores
     * SecurityExceptions (in which case some threads may remain
     * uninterrupted).
     *
     * @param onlyOne If true, interrupt at most one worker. This is
     * called only from tryTerminate when termination is otherwise
     * enabled but there are still other workers.  In this case, at
     * most one waiting worker is interrupted to propagate shutdown
     * signals in case all threads are currently waiting.
     * Interrupting any arbitrary thread ensures that newly arriving
     * workers since shutdown began will also eventually exit.
     * To guarantee eventual termination, it suffices to always
     * interrupt only one idle worker, but shutdown() interrupts all
     * idle workers so that redundant workers exit promptly, not
     * waiting for a straggler task to finish.
     */
    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers) {
                Thread t = w.thread;
                // 中断没有中断过的，空闲的(即tryLock成功)工作线程
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne)
                    break;
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Common form of interruptIdleWorkers, to avoid having to
     * remember what the boolean argument means.
     */
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    private static final boolean ONLY_ONE = true;

    /*
     * Misc utilities, most of which are also exported to
     * ScheduledThreadPoolExecutor
     */

    /**
     * Invokes the rejected execution handler for the given command.
     * Package-protected for use by ScheduledThreadPoolExecutor.
     */
    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    /**
     * 在调用关闭时执行运行状态转换后的任何进一步清理
     */
    void onShutdown() {}

    /**
     * State check needed by ScheduledThreadPoolExecutor to
     * enable running tasks during shutdown.
     *
     * @param shutdownOK true if should return true if SHUTDOWN
     */
    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    /**
     * 将任务队列排到一个新列表中，通常使用 drainTo
     * 但是，如果是某些特殊的队列，drainTo移除失败，则使用循环移除
     */
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<Runnable>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r))
                    taskList.add(r);
            }
        }
        return taskList;
    }

    /*
     * Methods for creating, running and cleaning up after workers
     */

    /**
     * Checks if a new worker can be added with respect to current
     * pool state and the given bound (either core or maximum). If so,
     * the worker count is adjusted accordingly, and, if possible, a
     * new worker is created and started, running firstTask as its
     * first task. This method returns false if the pool is stopped or
     * eligible to shut down. It also returns false if the thread
     * factory fails to create a thread when asked.  If the thread
     * creation fails, either due to the thread factory returning
     * null, or due to an exception (typically OutOfMemoryError in
     * Thread.start()), we roll back cleanly.
     *
     * @param firstTask the task the new thread should run first (or
     * null if none). Workers are created with an initial first task
     * (in method execute()) to bypass queuing when there are fewer
     * than corePoolSize threads (in which case we always start one),
     * or when the queue is full (in which case we must bypass queue).
     * Initially idle threads are usually created via
     * prestartCoreThread or to replace other dying workers.
     *
     * @param core if true use corePoolSize as bound, else
     * maximumPoolSize. (A boolean indicator is used here rather than a
     * value to ensure reads of fresh values after checking other pool
     * state).
     * @return true if successful
     */
    private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        for (; ; ) {
            int c = ctl.get();
            // 获取运行状态
            int rs = runStateOf(c);

            /*
             * 这个if判断
             * 如果rs >= SHUTDOWN，则表示此时不再接收新任务；
             * 接着判断以下3个条件，只要有1个不满足，则返回false：
             * 1. rs == SHUTDOWN，这时表示关闭状态，不再接受新提交的任务，但却可以继续处理阻塞队列中已保存的任务
             * 2. firsTask为空
             * 3. 阻塞队列不为空
             *
             * 首先考虑rs == SHUTDOWN的情况
             * 这种情况下不会接受新提交的任务，所以在firstTask不为空的时候会返回false；
             * 然后，如果firstTask为空，并且workQueue也为空，则返回false，
             * 因为队列中已经没有任务了，不需要再添加线程了
             */
            // Check if queue empty only if necessary.
            if (rs >= SHUTDOWN &&
                    !(rs == SHUTDOWN && firstTask == null && !workQueue.isEmpty()))
                return false;
            for (; ; ) {
                // 获取线程数
                int wc = workerCountOf(c);
                // 如果wc超过CAPACITY，也就是ctl的低29位的最大值（二进制是29个1），返回false；
                // 这里的core是addWorker方法的第二个参数，如果为true表示根据corePoolSize来比较，
                // 如果为false则根据maximumPoolSize来比较。
                //
                if (wc >= CAPACITY ||
                        wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;
                // 尝试增加workerCount，如果成功，则跳出第一个for循环
                if (compareAndIncrementWorkerCount(c))
                    break retry;
                // 如果增加workerCount失败，则重新获取ctl的值
                c = ctl.get();  // Re-read ctl
                // 如果当前的运行状态不等于rs，说明状态已被改变，返回第一个for循环继续执行
                if (runStateOf(c) != rs)
                    continue retry;
                // else CAS failed due to workerCount change; retry inner loop
            }
        }
        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            // 根据firstTask来创建Worker对象
            w = new Worker(firstTask);
            // 每一个Worker对象都会创建一个线程
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    // Recheck while holding lock.
                    // Back out on ThreadFactory failure or if
                    // shut down before lock acquired.
                    int rs = runStateOf(ctl.get());
                    // rs < SHUTDOWN表示是RUNNING状态；
                    // 如果rs是RUNNING状态或者rs是SHUTDOWN状态并且firstTask为null，向线程池中添加线程。
                    // 因为在SHUTDOWN时不会在添加新的任务，但还是会执行workQueue中的任务
                    if (rs < SHUTDOWN ||
                            (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) // precheck that t is startable
                            throw new IllegalThreadStateException();
                        // workers是一个HashSet
                        workers.add(w);
                        int s = workers.size();
                        // largestPoolSize记录着线程池中出现过的最大线程数量
                        if (s > largestPoolSize)
                            largestPoolSize = s;
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    // 启动线程
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            if (!workerStarted)
                addWorkerFailed(w);
        }
        return workerStarted;
    }

    /**
     * Rolls back the worker thread creation.
     * - removes worker from workers, if present
     * - decrements worker count
     * - rechecks for termination, in case the existence of this
     *   worker was holding up termination
     */
    private void addWorkerFailed(Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null)
                workers.remove(w);
            decrementWorkerCount();
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Performs cleanup and bookkeeping for a dying worker. Called
     * only from worker threads. Unless completedAbruptly is set,
     * assumes that workerCount has already been adjusted to account
     * for exit.  This method removes thread from worker set, and
     * possibly terminates the pool or replaces the worker if either
     * it exited due to user task exception or if fewer than
     * corePoolSize workers are running or queue is non-empty but
     * there are no workers.
     *
     * @param w the worker
     * @param completedAbruptly if the worker died due to user exception
     */
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        // 如果completedAbruptly值为true，则说明线程执行时出现了异常，需要将workerCount减1；
        // 如果线程执行时没有出现异常，说明在getTask()方法中已经已经对workerCount进行了减1操作，这里就不必再减了。
        if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted
            decrementWorkerCount();
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            //统计完成的任务数
            completedTaskCount += w.completedTasks;
            // 从workers中移除，也就表示着从线程池中移除了一个工作线程
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }
        // 根据线程池状态进行判断是否结束线程池
        tryTerminate();
        int c = ctl.get();
        /*
         * 当线程池是RUNNING或SHUTDOWN状态时，如果worker是异常结束，那么会直接addWorker；
         * 如果allowCoreThreadTimeOut=true，并且等待队列有任务，至少保留一个worker；
         * 如果allowCoreThreadTimeOut=false，workerCount不少于corePoolSize。
         */
        if (runStateLessThan(c, STOP)) {
            if (!completedAbruptly) {
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                if (min == 0 && ! workQueue.isEmpty())
                    min = 1;
                if (workerCountOf(c) >= min)
                    return; // replacement not needed
            }
            addWorker(null, false);
        }
    }

    /**
     * Performs blocking or timed wait for a task, depending on
     * current configuration settings, or returns null if this worker
     * must exit because of any of:
     * 1. There are more than maximumPoolSize workers (due to
     *    a call to setMaximumPoolSize).
     * 2. The pool is stopped.
     * 3. The pool is shutdown and the queue is empty.
     * 4. This worker timed out waiting for a task, and timed-out
     *    workers are subject to termination (that is,
     *    {@code allowCoreThreadTimeOut || workerCount > corePoolSize})
     *    both before and after the timed wait, and if the queue is
     *    non-empty, this worker is not the last thread in the pool.
     *
     * @return task, or null if the worker must exit, in which case
     *         workerCount is decremented
     */
    private Runnable getTask() {
        // timeOut变量的值表示上次从阻塞队列中取任务时是否超时
        boolean timedOut = false; // Did the last poll() time out?
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);
            // Check if queue empty only if necessary.
            /*
             * 如果线程池状态rs >= SHUTDOWN，也就是非RUNNING状态，再进行以下判断：
             * 1. rs >= STOP，线程池是否正在stop；
             * 2. 阻塞队列是否为空。
             * 如果以上条件满足，则将workerCount减1并返回null。
             * 因为如果当前线程池状态的值是SHUTDOWN或以上时，不允许再向阻塞队列中添加任务。
             */
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }
            int wc = workerCountOf(c);
            // Are workers subject to culling?
            // timed变量用于判断是否需要进行超时控制。
            // allowCoreThreadTimeOut默认是false，也就是核心线程不允许进行超时；
            // wc > corePoolSize，表示当前线程池中的线程数量大于核心线程数量；
            // 对于超过核心线程数量的这些线程，需要进行超时控制
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            /*
             * wc > maximumPoolSize的情况是因为可能在此方法执行阶段同时执行了setMaximumPoolSize方法；
             * timed && timedOut 如果为true，表示当前操作需要进行超时控制，并且上次从阻塞队列中获取任务发生了超时
             * 接下来判断，如果有效线程数量大于1，或者阻塞队列是空的，那么尝试将workerCount减1；
             * 如果减1失败，则返回重试。
             * 如果wc == 1时，也就说明当前线程是线程池中唯一的一个线程了。
             */
            if ((wc > maximumPoolSize || (timed && timedOut))
                    && (wc > 1 || workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c))
                    return null;
                continue;
            }
            try {
                /*
                 * 根据timed来判断，如果为true，则通过阻塞队列的poll方法进行超时控制，如果在keepAliveTime时间内没有获取到任务，则返回null；
                 * 否则通过take方法，如果这时队列为空，则take方法会阻塞直到队列不为空。
                 *
                 */
                Runnable r = timed ?
                        workQueue.poll(keepAliveTime, java.util.concurrent.TimeUnit.NANOSECONDS) :
                        workQueue.take();
                if (r != null)
                    return r;
                // 如果 r == null，说明已经超时，timedOut设置为true
                timedOut = true;
            } catch (InterruptedException retry) {
                // 如果获取任务时当前线程发生了中断，则设置timedOut为false并返回循环重试
                timedOut = false;
            }
        }
    }

    /**
     * Main worker run loop.  Repeatedly gets tasks from queue and
     * executes them, while coping with a number of issues:
     *
     * 1. We may start out with an initial task, in which case we
     * don't need to get the first one. Otherwise, as long as pool is
     * running, we get tasks from getTask. If it returns null then the
     * worker exits due to changed pool state or configuration
     * parameters.  Other exits result from exception throws in
     * external code, in which case completedAbruptly holds, which
     * usually leads processWorkerExit to replace this thread.
     *
     * 2. Before running any task, the lock is acquired to prevent
     * other pool interrupts while the task is executing, and then we
     * ensure that unless pool is stopping, this thread does not have
     * its interrupt set.
     *
     * 3. Each task run is preceded by a call to beforeExecute, which
     * might throw an exception, in which case we cause thread to die
     * (breaking loop with completedAbruptly true) without processing
     * the task.
     *
     * 4. Assuming beforeExecute completes normally, we run the task,
     * gathering any of its thrown exceptions to send to afterExecute.
     * We separately handle RuntimeException, Error (both of which the
     * specs guarantee that we trap) and arbitrary Throwables.
     * Because we cannot rethrow Throwables within Runnable.run, we
     * wrap them within Errors on the way out (to the thread's
     * UncaughtExceptionHandler).  Any thrown exception also
     * conservatively causes thread to die.
     *
     * 5. After task.run completes, we call afterExecute, which may
     * also throw an exception, which will also cause thread to
     * die. According to JLS Sec 14.20, this exception is the one that
     * will be in effect even if task.run throws.
     *
     * The net effect of the exception mechanics is that afterExecute
     * and the thread's UncaughtExceptionHandler have as accurate
     * information as we can provide about any problems encountered by
     * user code.
     *
     * @param w the worker
     */
    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        // 将state从构造器中设置的-1修改为0，执行后可以进行interrupt
        w.unlock();
        boolean completedAbruptly = true;
        try {
            while (task != null || (task = getTask()) != null) {
                w.lock();
                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted.  This
                // requires a recheck in second case to deal with
                // shutdownNow race while clearing interrupt
                if ((runStateAtLeast(ctl.get(), STOP) ||
                     (Thread.interrupted() &&
                      runStateAtLeast(ctl.get(), STOP))) &&
                    !wt.isInterrupted())
                    wt.interrupt();
                try {
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    w.completedTasks++;
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);
        }
    }

    // ---------公共构造器和方法----------

    /**
     * 线程池构造1，使用默认的线程工厂和默认拒绝策略
     *
     * @param corePoolSize 保留在池中的线程数，即使它们是空闲的，除非设置了{@code allowCoreThreadTimeOut}
     * @param maximumPoolSize 池中允许最大的线程池
     * @param keepAliveTime 当线程池中的线程数大于{@code corePoolSize}时，线程等待的最大空闲等待时间
     * @param unit {@code keepAliveTime}的单位
     * @param workQueue 队列用于存储未执行的任务. 队列仅保存由{@code execute}方法提交的Runnable任务
     * @throws IllegalArgumentException 以下任意一点成立:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException 如果{@code workQueue}为null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              java.util.concurrent.TimeUnit unit,
                              BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), defaultHandler);
    }

    /**
     * 线程池构造2，使用指定的线程工厂和默认拒绝策略
     *
     * @param corePoolSize 保留在池中的线程数，即使它们是空闲的，除非设置了{@code allowCoreThreadTimeOut}
     * @param maximumPoolSize 池中允许最大的线程池
     * @param keepAliveTime 当线程池中的线程数大于{@code corePoolSize}时，线程等待的最大空闲等待时间
     * @param unit {@code keepAliveTime}的单位
     * @param workQueue 队列用于存储未执行的任务. 队列仅保存由{@code execute}方法提交的Runnable任务
     * @param threadFactory 线程池创建线程时使用的工厂
     * @throws IllegalArgumentException 以下任意一点成立:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException 如果{@code workQueue}为null或{@code threadFactory}为null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              java.util.concurrent.TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             threadFactory, defaultHandler);
    }

    /**
     * 线程池构造3，使用默认的线程工厂和指定拒绝策略
     *
     * @param corePoolSize 保留在池中的线程数，即使它们是空闲的，除非设置了{@code allowCoreThreadTimeOut}
     * @param maximumPoolSize 池中允许最大的线程池
     * @param keepAliveTime 当线程池中的线程数大于{@code corePoolSize}时，线程等待的最大空闲等待时间
     * @param unit {@code keepAliveTime}的单位
     * @param workQueue 队列用于存储未执行的任务. 队列仅保存由{@code execute}方法提交的Runnable任务
     * @param handler 当线程和阻塞队列都满时，执行的拒绝策略
     * @throws IllegalArgumentException 以下任意一点成立:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException 如果{@code workQueue}为null或{@code handler}为null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              java.util.concurrent.TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              java.util.concurrent.RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), handler);
    }

    /**
     * 线程池构造4，使用指定的线程工厂和指定拒绝策略(真正初始化逻辑)
     *
     * @param corePoolSize 保留在池中的线程数，即使它们是空闲的，除非设置了{@code allowCoreThreadTimeOut}
     * @param maximumPoolSize 池中允许最大的线程池
     * @param keepAliveTime 当线程池中的线程数大于{@code corePoolSize}时，线程等待的最大空闲等待时间
     * @param unit {@code keepAliveTime}的单位
     * @param workQueue 队列用于存储未执行的任务. 队列仅保存由{@code execute}方法提交的Runnable任务
     * @param threadFactory 线程池创建线程时使用的工厂
     * @param handler 当线程和阻塞队列都满时，执行的拒绝策略
     * @throws IllegalArgumentException 以下任意一点成立:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException 如果{@code workQueue}为null或{@code threadFactory}为null或{@code handler}为null
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              java.util.concurrent.TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              java.util.concurrent.RejectedExecutionHandler handler) {
        // 入参合法校验，与注释说明的异常抛出逻辑一致
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize || keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        this.acc = System.getSecurityManager() == null ? null : AccessController.getContext();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    /**
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     *
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current {@code RejectedExecutionHandler}.
     *
     * @param command the task to execute
     * @throws java.util.concurrent.RejectedExecutionException at discretion of
     *         {@code RejectedExecutionHandler}, if the task
     *         cannot be accepted for execution
     * @throws NullPointerException if {@code command} is null
     */
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
        /*
         * clt记录着runState和workerCount
         */
        int c = ctl.get();
        /*
         * workerCountOf方法取出低29位的值，表示当前活动的线程数；
         * 如果当前活动线程数小于corePoolSize，则新建一个线程放入线程池中；
         * 并把任务添加到该线程中。
         */
        if (workerCountOf(c) < corePoolSize) {
            /*
             * addWorker中的第二个参数表示限制添加线程的数量是根据corePoolSize来判断还是maximumPoolSize来判断；
             * 如果为true，根据corePoolSize来判断；
             * 如果为false，则根据maximumPoolSize来判断
             */
            if (addWorker(command, true))
                return;
            /*
             * 如果添加失败，则重新获取ctl值
             */
            c = ctl.get();
        }
        /*
         * 如果当前线程池是运行状态并且任务添加到队列成功
         */
        if (isRunning(c) && workQueue.offer(command)) {
            // 重新获取ctl值
            int recheck = ctl.get();
            // 再次判断线程池的运行状态，如果不是运行状态，由于之前已经把command添加到workQueue中了，
            // 这时需要移除该command
            // 执行过后通过handler使用拒绝策略对该任务进行处理，整个方法返回
            if (! isRunning(recheck) && remove(command))
                reject(command);
            /*
             * 获取线程池中的有效线程数，如果数量是0，则执行addWorker方法
             * 这里传入的参数表示：
             * 1. 第一个参数为null，表示在线程池中创建一个线程，直接去队列拉取任务；
             * 2. 第二个参数为false，将线程池的有限线程数量的上限设置为maximumPoolSize，添加线程时根据maximumPoolSize来判断；
             * 如果判断workerCount大于0，则直接返回，在workQueue中新增的command会在将来的某个时刻被执行。
             */
            else if (workerCountOf(recheck) == 0)
                addWorker(null, false);
        }
        /*
         * 如果执行到这里，有两种情况：
         * 1. 线程池已经不是RUNNING状态；
         * 2. 线程池是RUNNING状态，但workerCount >= corePoolSize并且workQueue已满。
         * 这时，再次调用addWorker方法，但第二个参数传入为false，将线程池的有限线程数量的上限设置为maximumPoolSize；
         * 如果失败则拒绝该任务
         */
        else if (!addWorker(command, false))
            reject(command);
    }

    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     *
     * @throws SecurityException {@inheritDoc}
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 校验
            checkShutdownAccess();
            // 修改线程池状态为SHUTDOWN
            advanceRunState(SHUTDOWN);
            // 将空闲且未被打断的工作线程，执行interrupt
            interruptIdleWorkers();
            // 空方法
            onShutdown();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
    }

    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution. These tasks are drained (removed)
     * from the task queue upon return from this method.
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  This implementation
     * cancels tasks via {@link Thread#interrupt}, so any task that
     * fails to respond to interrupts may never terminate.
     *
     * @throws SecurityException {@inheritDoc}
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(STOP);
            interruptWorkers();
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }

    /*-----------------以下3个方法用于查询线程池状态----------------*/
    /**
     * 判断线程池是否shutdown
     * 即当前状态的值大于等于SHUTDOWN,(非RUNNING状态)
     *
     * @return {@code true} 当线程池为非RUNNING状态, 反之返回{@code false}
     */
    public boolean isShutdown() {
        return ! isRunning(ctl.get());
    }

    /**
     * 如果此执行程序在 {@link #shutdown} 或 {@link #shutdownNow} 之后正在终止但尚未完全终止，则返回 true
     * 即为SHUTDOWN、STOP、TIDYING状态时返回{@code true}
     *
     * @return {@code true} 如果终止但尚未终止
     */
    public boolean isTerminating() {
        int c = ctl.get();
        return ! isRunning(c) && runStateLessThan(c, TERMINATED);
    }

    /**
     * 判断线程池是否为terminated状态
     * 即当前状态的值是否大于等于TERMINATED(根据定义，只有TERMINATED状态时，才会返回true)
     *
     * @return {@code true} 当线程池为TERMINATED状态, 反之返回{@code false}
     */
    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (;;) {
                if (runStateAtLeast(ctl.get(), TERMINATED))
                    return true;
                if (nanos <= 0)
                    return false;
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 调用shutdown，在线程池被回收前
     */
    protected void finalize() {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null || acc == null) {
            shutdown();
        } else {
            PrivilegedAction<Void> pa = () -> { shutdown(); return null; };
            AccessController.doPrivileged(pa, acc);
        }
    }

    /**
     * 为设置一个新的线程工厂
     *
     * @param threadFactory 新的线程工厂
     * @throws NullPointerException 如果新的线程工厂为null
     * @see #getThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null)
            throw new NullPointerException();
        this.threadFactory = threadFactory;
    }

    /**
     * 返回当前的线程工厂
     *
     * @return 当前的线程工厂
     * @see #setThreadFactory(ThreadFactory)
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * 为不执行的任务设置一个新的拒绝策略
     *
     * @param handler 新的拒绝策略
     * @throws NullPointerException 如果新的拒绝策略为null
     * @see #getRejectedExecutionHandler
     */
    public void setRejectedExecutionHandler(java.util.concurrent.RejectedExecutionHandler handler) {
        if (handler == null)
            throw new NullPointerException();
        this.handler = handler;
    }

    /**
     * 返回当前的不执行任务的拒绝策略
     *
     * @return 当前的拒绝策略
     * @see #setRejectedExecutionHandler(java.util.concurrent.RejectedExecutionHandler)
     */
    public java.util.concurrent.RejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }

    /**
     * 设置核心线程数
     * 如果新值小于当前值，多余的现有线程将在下次空闲时终止
     * 如果更大，则视任务队列情况，启动新线程以执行队列中的任务
     *
     * @param corePoolSize 新核心线程数量
     * @throws IllegalArgumentException 如果{@code corePoolSize < 0}
     * @see #getCorePoolSize
     */
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0)
            throw new IllegalArgumentException();
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        // 当前线程数大于新修改的线程
        if (workerCountOf(ctl.get()) > corePoolSize)
            interruptIdleWorkers();
        else if (delta > 0) {
            // 当前线程小于新修改的线程，创建尽可能少的核心线程
            // 1. 首先取差值和队列中任务数量的较小值k
            // 2. 先拟定创建k个核心线程，并逐一调用addWorker创建
            // 3. 每创建一个即检查任务队列是否为空，若为空说明线程已足够，停止创建；否则执行至循环结束
            int k = Math.min(delta, workQueue.size());
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty())
                    break;
            }
        }
    }

    /**
     * 返回核心线程数量
     *
     * @return 核心线程数量
     * @see #setCorePoolSize
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /*---------------------以下三个方法，可以实现预加载核心线程----------------------*/
    /**
     * 这会覆盖仅在执行新任务时启动核心线程的默认策略，这会覆盖仅在执行新任务时启动核心线程的默认策略
     * 如果核心线程已经到达上限，则会返回false
     *
     * @return {@code true} 为创建成功
     */
    public boolean prestartCoreThread() {
        // 如果线程数小于核心数，且创建核心线程成功，则返回true
        return workerCountOf(ctl.get()) < corePoolSize &&
            addWorker(null, true);
    }

    /**
     * 当线程数量小于核心线程数时，起一个核心线程，当线程数量为0时，起一个非核心线程
     */
    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize)
            addWorker(null, true);
        else if (wc == 0)
            addWorker(null, false);
    }

    /**
     * 启动所有核心线程，使它们空闲等待工作。这会覆盖仅在执行新任务时启动核心线程的默认策略
     *
     * @return 启动的核心线程数量
     */
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true))
            ++n;
        return n;
    }

    /**
     * 返回allowCoreThreadTimeOut
     *
     * @return allowCoreThreadTimeOut
     * @since 1.6
     */
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    /**
     * 修改{@code allowCoreThreadTimeOut}的值，为避免持续的线程替换，设置 {@code true} 时的 keep-alive 时间必须大于零
     * 通常应该在主动使用池之前调用此方法
     *
     * @param value {@code true} or {@code false}
     * @throws IllegalArgumentException 如果value为{@code true}且当前的{@code keepAliveTime}不大于0
     * @since 1.6
     */
    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0)
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        if (value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            // todo interruptIdleWorkers
            if (value)
                interruptIdleWorkers();
        }
    }

    /**
     * 设置允许的最大线程数。这会覆盖构造函数中设置的任何值。如果新值小于当前值，多余的现有线程将在下次空闲时终止(不会立即生效)
     *
     * @param maximumPoolSize 新的最大线程数
     * @throws IllegalArgumentException 如果新的最大线程数小于0, 或者小于{@linkplain #getCorePoolSize 核心线程数}
     * @see #getMaximumPoolSize
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
            throw new IllegalArgumentException();
        this.maximumPoolSize = maximumPoolSize;
        // todo interruptIdleWorkers
        if (workerCountOf(ctl.get()) > maximumPoolSize)
            interruptIdleWorkers();
    }

    /**
     * 返回允许的最大线程数
     *
     * @return 允许的最大线程数
     * @see #setMaximumPoolSize
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * 设置线程在终止之前可以保持空闲的时间限制，如果当前池中的线程数超过核心数{@code corePoolSize}
     * 则在等待此时间{@code getKeepAliveTime}后没有处理任务，多余的线程将被终止
     *
     * @param time 保持空闲的时间(时间值为零将导致多余的线程在执行任务后立即终止)
     * @param unit {@code time}的时间单位
     * @throws IllegalArgumentException 如果{@code time}小于0 或者{@code time}为0且{@code allowsCoreThreadTimeOut}为true
     * @see #getKeepAliveTime(java.util.concurrent.TimeUnit)
     */
    public void setKeepAliveTime(long time, java.util.concurrent.TimeUnit unit) {
        if (time < 0)
            throw new IllegalArgumentException();
        // allowCoreThreadTimeOut为true时，time不允许为0
        if (time == 0 && allowsCoreThreadTimeOut())
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        // todo interruptIdleWorkers
        if (delta < 0)
            interruptIdleWorkers();
    }

    /**
     * 返回线程保持活跃的最大空闲时间
     *
     * @param unit 返回结果的时间单位
     * @return 线程保持活跃的最大空闲时间
     * @see #setKeepAliveTime(long, java.util.concurrent.TimeUnit)
     */
    public long getKeepAliveTime(java.util.concurrent.TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    /*--------------用户级别的等待队列操作公用方法----------------*/
    /**
     * 返回此执行程序使用的任务队列。访问任务队列主要用于调试和监控，此队列可能正在使用中，检索任务队列不会阻止排队的任务执行
     *
     * @return 任务等待队列
     */
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    /**
     * 如果该任务存在，则从执行程序的内部队列中删除该任务
     * 如果是通过{@link #submit}方法提交的任务，则本方法无法移除，因为此时保存在workQueue中的是{@link FutureTask}而非Runnable
     * 但是，在这种情况下，可以使用方法 {@link #purge} 删除那些取消态的Future
     *
     * @param task 待移除的task
     * @return {@code true} 如果任务被移除
     */
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate();
        return removed;
    }

    /**
     * 尝试删除所有取消态的 {@link java.util.concurrent.Future}任务
     * 取消态的任务不会被执行，但是会一直存放在队列中，直到被工作线程主动移除，调用此方法替换为现在尝试移除他们
     * 此方法在其他线程的干扰(其他线程操作workQueue)下，可能失败
     */
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            // 通过迭代器遍历，如果是FutureTask，且已被取消，则移除
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof java.util.concurrent.Future<?> && ((java.util.concurrent.Future<?>)r).isCancelled())
                    it.remove();
            }
        } catch (ConcurrentModificationException fallThrough) {
            // 如果遇到异常，则先转化为数组后，再遍历
            for (Object r : q.toArray())
                if (r instanceof java.util.concurrent.Future<?> && ((Future<?>)r).isCancelled())
                    q.remove(r);
        }

        tryTerminate();
    }

    /*-------------Statistics，获取线程池的各项运行数据----------------*/
    /**
     * 返回池中的当前线程数
     *
     * @return 线程数量
     */
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 如果状态为TIDYING或TERMINATED 则返回0，否则返回worker的数量
            return runStateAtLeast(ctl.get(), TIDYING) ? 0 : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回正在执行任务的线程数量
     *
     * @return 正在执行任务的线程数量
     */
    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (Worker w : workers)
                if (w.isLocked())
                    ++n;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回池中同时存在的最大线程数
     *
     * @return 池中同时存在的最大线程数
     */
    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回已安排执行的任务的大致总数。因为任务和线程的状态在计算过程中可能会动态变化，所以返回的值只是一个近似值
     *
     * @return 已安排执行的任务的大致总数
     */
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 总任务数 = 计数器数量 + 各线程计数器数量 + 执行中线程数 + 等待队列数量
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked())
                    ++n;
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回已完成执行的任务总数(近似值)
     *
     * @return 已完成执行的大致任务总数
     */
    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 已完成数 = 计数器数量 + 各线程计数器数量
            long n = completedTaskCount;
            for (Worker w : workers)
                n += w.completedTasks;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 返回线程池的描述，如：
     * java.util.concurrent.ThreadPoolExecutor@73a28541[Running, pool size = 1, active threads = 1, queued tasks = 1,
     * completed tasks = 0]
     *
     * @return 标识此池的字符串及其状态
     */
    public String toString() {
        // 完成任务数
        long ncompleted;
        // 线程池线程数，活跃线程数(即任务执行中的线程数)
        int nworkers, nactive;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workers.size();
            for (Worker w : workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked())
                    ++nactive;
            }
        } finally {
            mainLock.unlock();
        }
        // 获取线程池运行状态
        int c = ctl.get();
        String rs = (runStateLessThan(c, SHUTDOWN) ? "Running" : (runStateAtLeast(c, TERMINATED) ? "Terminated" : "Shutting down"));
        // 拼接结果
        return super.toString() +
            "[" + rs +
            ", pool size = " + nworkers +
            ", active threads = " + nactive +
            ", queued tasks = " + workQueue.size() +
            ", completed tasks = " + ncompleted +
            "]";
    }

    /*------------扩展钩，通过extends来扩展以下3个方法------------*/
    /**
     * 任务{@code r} 执行前调用，此方法由执行任务 {@code r} 的线程 {@code t} 调用(runWorker方法)
     * 可用于重新初始化 ThreadLocals，或执行日志记录等等
     *
     * @param t 执行任务r的线程 {@code r}
     * @param r 被执行的任务
     */
    protected void beforeExecute(Thread t, Runnable r) { }

    /**
     * 任务{@code r} 执行完成后调用。此方法由执行任务{@code r}的线程调用。
     * {@code t} 如果非 null，则 Throwable 是未捕获的 {@code RuntimeException} 或 {@code Error} 导致执行突然终止。
     *
     * @param r 被执行的任务
     * @param t 导致执行终止的异常, 如果正常执行则为null
     */
    protected void afterExecute(Runnable r, Throwable t) { }

    /**
     * Executor 终止时调用的方法
     */
    protected void terminated() { }

    /*--------------预定义的4个拒绝策略-----------------*/
    public static class CallerRunsPolicy implements java.util.concurrent.RejectedExecutionHandler {
        public CallerRunsPolicy() { }

        /**
         * 在调用者的线程中执行任务r，如果线程池已经shutdown，则丢弃任务r
         *
         * @param r 需要执行的任务
         * @param e 执行任务r的执行者，线程池
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    public static class AbortPolicy implements java.util.concurrent.RejectedExecutionHandler {
        public AbortPolicy() { }

        /**
         * 直接抛出异常 {@code RejectedExecutionException}.
         *
         * @param r 需要执行的任务
         * @param e 执行任务r的执行者，线程池
         * @throws java.util.concurrent.RejectedExecutionException always
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + e.toString());
        }
    }

    public static class DiscardPolicy implements java.util.concurrent.RejectedExecutionHandler {
        public DiscardPolicy() { }

        /**
         * 什么都不做，即丢弃该任务
         *
         * @param r 需要执行的任务
         * @param e 执行任务r的执行者，线程池
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }
    }

    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        public DiscardOldestPolicy() { }

        /**
         * 移除等待队列中最早的任务(即队首元素)，并执行任务r，前提是线程池e不为shutdown状态
         *
         * @param r 需要执行的任务
         * @param e 执行任务r的执行者，线程池
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                e.getQueue().poll();
                e.execute(r);
            }
        }
    }
}
