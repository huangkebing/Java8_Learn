package java.util.concurrent.locks;

public abstract class AbstractOwnableSynchronizer implements java.io.Serializable {
    private static final long serialVersionUID = 3737899427754241961L;

    // protected 空构造器
    protected AbstractOwnableSynchronizer() { }

    // 锁的拥有线程
    private transient Thread exclusiveOwnerThread;

    // set方法
    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }

    // get方法
    protected final Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }
}
