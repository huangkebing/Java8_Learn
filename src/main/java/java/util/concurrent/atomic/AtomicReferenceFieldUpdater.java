package java.util.concurrent.atomic;

import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * A reflection-based utility that enables atomic updates to
 * designated {@code volatile} reference fields of designated
 * classes.  This class is designed for use in atomic data structures
 * in which several reference fields of the same node are
 * independently subject to atomic updates. For example, a tree node
 * might be declared as
 *
 * <pre> {@code
 * class Node {
 *   private volatile Node left, right;
 *
 *   private static final AtomicReferenceFieldUpdater<Node, Node> leftUpdater =
 *     AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "left");
 *   private static AtomicReferenceFieldUpdater<Node, Node> rightUpdater =
 *     AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "right");
 *
 *   Node getLeft() { return left; }
 *   boolean compareAndSetLeft(Node expect, Node update) {
 *     return leftUpdater.compareAndSet(this, expect, update);
 *   }
 *   // ... and so on
 * }}</pre>
 *
 * <p>Note that the guarantees of the {@code compareAndSet}
 * method in this class are weaker than in other atomic classes.
 * Because this class cannot ensure that all uses of the field
 * are appropriate for purposes of atomic access, it can
 * guarantee atomicity only with respect to other invocations of
 * {@code compareAndSet} and {@code set} on the same updater.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <T> The type of the object holding the updatable field
 * @param <V> The type of the field
 */
public abstract class AtomicReferenceFieldUpdater<T,V> {

    /**
     * 获得修改器
     */
    @CallerSensitive
    public static <U,W> AtomicReferenceFieldUpdater<U,W> newUpdater(Class<U> tclass,
                                                                    Class<W> vclass,
                                                                    String fieldName) {
        return new AtomicReferenceFieldUpdaterImpl<U,W>
            (tclass, vclass, fieldName, Reflection.getCallerClass());
    }

    protected AtomicReferenceFieldUpdater() {
    }

    public abstract boolean compareAndSet(T obj, V expect, V update);

    public abstract boolean weakCompareAndSet(T obj, V expect, V update);

    public abstract void set(T obj, V newValue);

    public abstract void lazySet(T obj, V newValue);

    public abstract V get(T obj);

    public V getAndSet(T obj, V newValue) {
        V prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, newValue));
        return prev;
    }

    public final V getAndUpdate(T obj, UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = updateFunction.apply(prev);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    public final V updateAndGet(T obj, UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = updateFunction.apply(prev);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    public final V getAndAccumulate(T obj, V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    public final V accumulateAndGet(T obj, V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V prev, next;
        do {
            prev = get(obj);
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    private static final class AtomicReferenceFieldUpdaterImpl<T,V> extends AtomicReferenceFieldUpdater<T,V> {
        private static final sun.misc.Unsafe U = sun.misc.Unsafe.getUnsafe();
        private final long offset;
        // 调用者
        private final Class<?> cclass;
        // 修改字段拥有者
        private final Class<T> tclass;
        // 修改字段类型
        private final Class<V> vclass;

        AtomicReferenceFieldUpdaterImpl(final Class<T> tclass,
                                        final Class<V> vclass,
                                        final String fieldName,
                                        final Class<?> caller) {
            final Field field;
            final Class<?> fieldClass;
            final int modifiers;
            try {
                field = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Field>() {
                        public Field run() throws NoSuchFieldException {
                            return tclass.getDeclaredField(fieldName);
                        }
                    });
                modifiers = field.getModifiers();
                sun.reflect.misc.ReflectUtil.ensureMemberAccess(
                    caller, tclass, null, modifiers);
                ClassLoader cl = tclass.getClassLoader();
                ClassLoader ccl = caller.getClassLoader();
                if ((ccl != null) && (ccl != cl) &&
                    ((cl == null) || !isAncestor(cl, ccl))) {
                    sun.reflect.misc.ReflectUtil.checkPackageAccess(tclass);
                }
                fieldClass = field.getType();
            } catch (PrivilegedActionException pae) {
                throw new RuntimeException(pae.getException());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            if (vclass != fieldClass)
                throw new ClassCastException();
            if (vclass.isPrimitive())
                throw new IllegalArgumentException("Must be reference type");

            if (!Modifier.isVolatile(modifiers))
                throw new IllegalArgumentException("Must be volatile type");

            this.cclass = (Modifier.isProtected(modifiers) &&
                           tclass.isAssignableFrom(caller) &&
                           !isSamePackage(tclass, caller))
                          ? caller : tclass;
            this.tclass = tclass;
            this.vclass = vclass;
            this.offset = U.objectFieldOffset(field);
        }

        private static boolean isAncestor(ClassLoader first, ClassLoader second) {
            ClassLoader acl = first;
            do {
                acl = acl.getParent();
                if (second == acl) {
                    return true;
                }
            } while (acl != null);
            return false;
        }

        private static boolean isSamePackage(Class<?> class1, Class<?> class2) {
            return class1.getClassLoader() == class2.getClassLoader()
                   && Objects.equals(getPackageName(class1), getPackageName(class2));
        }

        private static String getPackageName(Class<?> cls) {
            String cn = cls.getName();
            int dot = cn.lastIndexOf('.');
            return (dot != -1) ? cn.substring(0, dot) : "";
        }

        private final void accessCheck(T obj) {
            if (!cclass.isInstance(obj))
                throwAccessCheckException(obj);
        }

        private final void throwAccessCheckException(T obj) {
            if (cclass == tclass)
                throw new ClassCastException();
            else
                throw new RuntimeException(
                    new IllegalAccessException(
                        "Class " +
                        cclass.getName() +
                        " can not access a protected member of class " +
                        tclass.getName() +
                        " using an instance of " +
                        obj.getClass().getName()));
        }

        private final void valueCheck(V v) {
            if (v != null && !(vclass.isInstance(v)))
                throwCCE();
        }

        static void throwCCE() {
            throw new ClassCastException();
        }

        public final boolean compareAndSet(T obj, V expect, V update) {
            accessCheck(obj);
            valueCheck(update);
            return U.compareAndSwapObject(obj, offset, expect, update);
        }

        public final boolean weakCompareAndSet(T obj, V expect, V update) {
            accessCheck(obj);
            valueCheck(update);
            return U.compareAndSwapObject(obj, offset, expect, update);
        }

        public final void set(T obj, V newValue) {
            accessCheck(obj);
            valueCheck(newValue);
            U.putObjectVolatile(obj, offset, newValue);
        }

        public final void lazySet(T obj, V newValue) {
            accessCheck(obj);
            valueCheck(newValue);
            U.putOrderedObject(obj, offset, newValue);
        }

        @SuppressWarnings("unchecked")
        public final V get(T obj) {
            accessCheck(obj);
            return (V)U.getObjectVolatile(obj, offset);
        }

        @SuppressWarnings("unchecked")
        public final V getAndSet(T obj, V newValue) {
            accessCheck(obj);
            valueCheck(newValue);
            return (V)U.getAndSetObject(obj, offset, newValue);
        }
    }
}
