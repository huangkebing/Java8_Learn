package java.util.concurrent.atomic;

/**
 * 拥有一个对象实例和标记字段的原子引用更新类
 */
public class AtomicMarkableReference<V> {

    private static class Pair<T> {
        final T reference;
        final boolean mark;
        private Pair(T reference, boolean mark) {
            this.reference = reference;
            this.mark = mark;
        }
        // 使用此方法获得Pair实例
        static <T> Pair<T> of(T reference, boolean mark) {
            return new Pair<T>(reference, mark);
        }
    }

    private volatile Pair<V> pair;

    /**
     * 构造器，给定一个实例，和一个标记，创建实例
     */
    public AtomicMarkableReference(V initialRef, boolean initialMark) {
        pair = Pair.of(initialRef, initialMark);
    }

    /**
     * 获得实例对象
     */
    public V getReference() {
        return pair.reference;
    }

    /**
     * 获得标记
     */
    public boolean isMarked() {
        return pair.mark;
    }

    /**
     * 获得实例对象，并且将标记保存在markHolder数组中
     */
    public V get(boolean[] markHolder) {
        Pair<V> pair = this.pair;
        markHolder[0] = pair.mark;
        return pair.reference;
    }

    public boolean weakCompareAndSet(V expectedReference,
                                     V newReference,
                                     boolean expectedMark,
                                     boolean newMark) {
        return compareAndSet(expectedReference, newReference, expectedMark, newMark);
    }

    /**
     * 交换对象实例，和标记
     */
    public boolean compareAndSet(V expectedReference,
                                 V newReference,
                                 boolean expectedMark,
                                 boolean newMark) {
        Pair<V> current = pair;
        // 先判断预期值和实际值是否一致，不一致直接返回false
        // 然后比较新值和实际值是否一致，一致直接返回true，不一致调用casPair进行CAS
        return expectedReference == current.reference &&
                        expectedMark == current.mark &&
                        ((newReference == current.reference &&
                                newMark == current.mark) ||
                                casPair(current, Pair.of(newReference, newMark)));
    }

    /**
     * set方法修改
     */
    public void set(V newReference, boolean newMark) {
        Pair<V> current = pair;
        if (newReference != current.reference || newMark != current.mark)
            this.pair = Pair.of(newReference, newMark);
    }

    /**
     * 修改标记方法
     */
    public boolean attemptMark(V expectedReference, boolean newMark) {
        Pair<V> current = pair;
        return expectedReference == current.reference &&
            (newMark == current.mark ||
             casPair(current, Pair.of(expectedReference, newMark)));
    }

    private static final sun.misc.Unsafe UNSAFE = sun.misc.Unsafe.getUnsafe();
    // 获得pair字段的偏移量
    private static final long pairOffset = objectFieldOffset(UNSAFE, "pair", AtomicMarkableReference.class);

    // 调用CAS执行方法
    private boolean casPair(Pair<V> cmp, Pair<V> val) {
        return UNSAFE.compareAndSwapObject(this, pairOffset, cmp, val);
    }

    static long objectFieldOffset(sun.misc.Unsafe UNSAFE, String field, Class<?> klazz) {
        try {
            return UNSAFE.objectFieldOffset(klazz.getDeclaredField(field));
        } catch (NoSuchFieldException e) {
            NoSuchFieldError error = new NoSuchFieldError(field);
            error.initCause(e);
            throw error;
        }
    }
}
