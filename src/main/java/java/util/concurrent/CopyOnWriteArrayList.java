package java.util.concurrent;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A thread-safe variant of {@link java.util.ArrayList} in which all mutative
 * operations ({@code add}, {@code set}, and so on) are implemented by
 * making a fresh copy of the underlying array.
 *
 * <p>This is ordinarily too costly, but may be <em>more</em> efficient
 * than alternatives when traversal operations vastly outnumber
 * mutations, and is useful when you cannot or don't want to
 * synchronize traversals, yet need to preclude interference among
 * concurrent threads.  The "snapshot" style iterator method uses a
 * reference to the state of the array at the point that the iterator
 * was created. This array never changes during the lifetime of the
 * iterator, so interference is impossible and the iterator is
 * guaranteed not to throw {@code ConcurrentModificationException}.
 * The iterator will not reflect additions, removals, or changes to
 * the list since the iterator was created.  Element-changing
 * operations on iterators themselves ({@code remove}, {@code set}, and
 * {@code add}) are not supported. These methods throw
 * {@code UnsupportedOperationException}.
 *
 * <p>All elements are permitted, including {@code null}.
 *
 * <p>Memory consistency effects: As with other concurrent
 * collections, actions in a thread prior to placing an object into a
 * {@code CopyOnWriteArrayList}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that element from
 * the {@code CopyOnWriteArrayList} in another thread.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public class CopyOnWriteArrayList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 8673264195747942595L;

    final transient ReentrantLock lock = new ReentrantLock();

    /**
     * 内存存储数据，只能通过getArray/setArray方法访问
     */
    private transient volatile Object[] array;

    /**
     * Gets the array
     * default类型方法，可以被同包下的其他类访问，如CopyOnWriteArraySet
     */
    final Object[] getArray() {
        return array;
    }

    /**
     * Sets the array
     */
    final void setArray(Object[] a) {
        array = a;
    }

    /**
     * 默认空参构造，创建一个size为0的空数组
     */
    public CopyOnWriteArrayList() {
        setArray(new Object[0]);
    }

    /**
     * 构造器，入参为一个集合
     * @throws NullPointerException if the specified collection is null
     */
    public CopyOnWriteArrayList(Collection<? extends E> c) {
        Object[] elements;
        if (c.getClass() == CopyOnWriteArrayList.class)
            elements = ((CopyOnWriteArrayList<?>)c).getArray();
        else {
            elements = c.toArray();
            /*
             * c.toArray might (incorrectly) not return Object[] (see 6260652)
             * 见ListTest.toArrayTest()
             */
            if (elements.getClass() != Object[].class)
                elements = Arrays.copyOf(elements, elements.length, Object[].class);
        }
        setArray(elements);
    }

    /**
     * 构造器，入参为数组，通过Arrays.copyOf复制一份传入
     * @throws NullPointerException if the specified array is null
     */
    public CopyOnWriteArrayList(E[] toCopyIn) {
        setArray(Arrays.copyOf(toCopyIn, toCopyIn.length, Object[].class));
    }

    //-------------List通用方法-------------
    public int size() {
        return getArray().length;
    }

    public boolean isEmpty() {
        return size() == 0;
    }
    private static boolean eq(Object o1, Object o2) {
        return (o1 == null) ? o2 == null : o1.equals(o2);
    }

    /**
     * 对象o 是否在数组elements的下标[index,fence)中
     * 若存在返回下标i，否则返回-1
     */
    private static int indexOf(Object o, Object[] elements,
                               int index, int fence) {
        if (o == null) {
            for (int i = index; i < fence; i++)
                if (elements[i] == null)
                    return i;
        } else {
            for (int i = index; i < fence; i++)
                if (o.equals(elements[i]))
                    return i;
        }
        return -1;
    }

    /**
     * 查找o在elements的下标[0,index]中 最后一次出现的下标i
     * 不存在返回-1
     */
    private static int lastIndexOf(Object o, Object[] elements, int index) {
        if (o == null) {
            for (int i = index; i >= 0; i--)
                if (elements[i] == null)
                    return i;
        } else {
            for (int i = index; i >= 0; i--)
                if (o.equals(elements[i]))
                    return i;
        }
        return -1;
    }
    public boolean contains(Object o) {
        Object[] elements = getArray();
        return indexOf(o, elements, 0, elements.length) >= 0;
    }
    public int indexOf(Object o) {
        Object[] elements = getArray();
        return indexOf(o, elements, 0, elements.length);
    }

    /**
     * 查找元素e，在list中[index,size)中第一次出现的位置，不存在返回-1
     */
    public int indexOf(E e, int index) {
        Object[] elements = getArray();
        return indexOf(e, elements, index, elements.length);
    }
    public int lastIndexOf(Object o) {
        Object[] elements = getArray();
        return lastIndexOf(o, elements, elements.length - 1);
    }
    public int lastIndexOf(E e, int index) {
        Object[] elements = getArray();
        return lastIndexOf(e, elements, index);
    }

    /**
     * 浅拷贝
     */
    public Object clone() {
        try {
            @SuppressWarnings("unchecked")
            CopyOnWriteArrayList<E> clone =
                (CopyOnWriteArrayList<E>) super.clone();
            clone.resetLock();
            return clone;
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }
    public Object[] toArray() {
        Object[] elements = getArray();
        return Arrays.copyOf(elements, elements.length);
    }

    /**
     * 将list array中的元素 复制到a中，若a.length > elements.length，将下标=size元素赋null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T a[]) {
        Object[] elements = getArray();
        int len = elements.length;
        if (a.length < len)
            return (T[]) Arrays.copyOf(elements, len, a.getClass());
        else {
            System.arraycopy(elements, 0, a, 0, len);
            if (a.length > len)
                a[len] = null;
            return a;
        }
    }

    // 元素操作方法，读方法直接执行

    @SuppressWarnings("unchecked")
    private E get(Object[] a, int index) {
        return (E) a[index];
    }

    public E get(int index) {
        return get(getArray(), index);
    }

    public E set(int index, E element) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            E oldValue = get(elements, index);
            // 复制一个新数组，将元素替换在新数组中，set回去
            if (oldValue != element) {
                int len = elements.length;
                Object[] newElements = Arrays.copyOf(elements, len);
                newElements[index] = element;
                setArray(newElements);
            } else {
                setArray(elements);
            }
            return oldValue;
        } finally {
            lock.unlock();
        }
    }

    public boolean add(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            // 创建新数组，长度为原长度 + 1
            Object[] newElements = Arrays.copyOf(elements, len + 1);
            newElements[len] = e;
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void add(int index, E element) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (index > len || index < 0)
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + len);
            Object[] newElements;
            int numMoved = len - index;
            if (numMoved == 0)
                // index在elements末尾，可以直接拷贝数组
                newElements = Arrays.copyOf(elements, len + 1);
            else {
                // 否则，数组分两段复制
                newElements = new Object[len + 1];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index, newElements, index + 1, numMoved);
            }
            newElements[index] = element;
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }

    public E remove(int index) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            E oldValue = get(elements, index);
            int numMoved = len - index - 1;
            if (numMoved == 0)
                setArray(Arrays.copyOf(elements, len - 1));
            else {
                Object[] newElements = new Object[len - 1];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index + 1, newElements, index,
                                 numMoved);
                setArray(newElements);
            }
            return oldValue;
        } finally {
            lock.unlock();
        }
    }

    public boolean remove(Object o) {
        Object[] snapshot = getArray();
        int index = indexOf(o, snapshot, 0, snapshot.length);
        return (index < 0) ? false : remove(o, snapshot, index);
    }

    private boolean remove(Object o, Object[] snapshot, int index) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] current = getArray();
            int len = current.length;
            if (snapshot != current) findIndex: {
                int prefix = Math.min(index, len);
                for (int i = 0; i < prefix; i++) {
                    if (current[i] != snapshot[i] && eq(o, current[i])) {
                        index = i;
                        break findIndex;
                    }
                }
                if (index >= len)
                    return false;
                if (current[index] == o)
                    break findIndex;
                index = indexOf(o, current, index, len);
                if (index < 0)
                    return false;
            }
            Object[] newElements = new Object[len - 1];
            System.arraycopy(current, 0, newElements, 0, index);
            System.arraycopy(current, index + 1, newElements, index, len - index - 1);
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    void removeRange(int fromIndex, int toIndex) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;

            if (fromIndex < 0 || toIndex > len || toIndex < fromIndex)
                throw new IndexOutOfBoundsException();
            int newlen = len - (toIndex - fromIndex);
            int numMoved = len - toIndex;
            if (numMoved == 0)
                setArray(Arrays.copyOf(elements, newlen));
            else {
                Object[] newElements = new Object[newlen];
                System.arraycopy(elements, 0, newElements, 0, fromIndex);
                System.arraycopy(elements, toIndex, newElements,
                                 fromIndex, numMoved);
                setArray(newElements);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 当元素不存在时add，O(n)
     */
    public boolean addIfAbsent(E e) {
        Object[] snapshot = getArray();
        return indexOf(e, snapshot, 0, snapshot.length) >= 0 ? false :
            addIfAbsent(e, snapshot);
    }

    private boolean addIfAbsent(E e, Object[] snapshot) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] current = getArray();
            int len = current.length;
            if (snapshot != current) {
                // Optimize for lost race to another addXXX operation
                int common = Math.min(snapshot.length, len);
                for (int i = 0; i < common; i++)
                    if (current[i] != snapshot[i] && eq(e, current[i]))
                        return false;
                if (indexOf(e, current, common, len) >= 0)
                        return false;
            }
            Object[] newElements = Arrays.copyOf(current, len + 1);
            newElements[len] = e;
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns {@code true} if this list contains all of the elements of the
     * specified collection.
     *
     * @param c collection to be checked for containment in this list
     * @return {@code true} if this list contains all of the elements of the
     *         specified collection
     * @throws NullPointerException if the specified collection is null
     * @see #contains(Object)
     */
    public boolean containsAll(Collection<?> c) {
        Object[] elements = getArray();
        int len = elements.length;
        for (Object e : c) {
            if (indexOf(e, elements, 0, len) < 0)
                return false;
        }
        return true;
    }

    /**
     * Removes from this list all of its elements that are contained in
     * the specified collection. This is a particularly expensive operation
     * in this class because of the need for an internal temporary array.
     *
     * @param c collection containing elements to be removed from this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of this list
     *         is incompatible with the specified collection
     *         (<a href="../Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this list contains a null element and the
     *         specified collection does not permit null elements
     *         (<a href="../Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see #remove(Object)
     */
    public boolean removeAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                // temp array holds those elements we know we want to keep
                int newlen = 0;
                Object[] temp = new Object[len];
                for (int i = 0; i < len; ++i) {
                    Object element = elements[i];
                    if (!c.contains(element))
                        temp[newlen++] = element;
                }
                if (newlen != len) {
                    setArray(Arrays.copyOf(temp, newlen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retains only the elements in this list that are contained in the
     * specified collection.  In other words, removes from this list all of
     * its elements that are not contained in the specified collection.
     *
     * @param c collection containing elements to be retained in this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of this list
     *         is incompatible with the specified collection
     *         (<a href="../Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this list contains a null element and the
     *         specified collection does not permit null elements
     *         (<a href="../Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see #remove(Object)
     */
    public boolean retainAll(Collection<?> c) {
        if (c == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                // temp array holds those elements we know we want to keep
                int newlen = 0;
                Object[] temp = new Object[len];
                for (int i = 0; i < len; ++i) {
                    Object element = elements[i];
                    if (c.contains(element))
                        temp[newlen++] = element;
                }
                if (newlen != len) {
                    setArray(Arrays.copyOf(temp, newlen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Appends all of the elements in the specified collection that
     * are not already contained in this list, to the end of
     * this list, in the order that they are returned by the
     * specified collection's iterator.
     *
     * @param c collection containing elements to be added to this list
     * @return the number of elements added
     * @throws NullPointerException if the specified collection is null
     * @see #addIfAbsent(Object)
     */
    public int addAllAbsent(Collection<? extends E> c) {
        Object[] cs = c.toArray();
        if (cs.length == 0)
            return 0;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            int added = 0;
            // uniquify and compact elements in cs
            for (int i = 0; i < cs.length; ++i) {
                Object e = cs[i];
                if (indexOf(e, elements, 0, len) < 0 &&
                    indexOf(e, cs, 0, added) < 0)
                    cs[added++] = e;
            }
            if (added > 0) {
                Object[] newElements = Arrays.copyOf(elements, len + added);
                System.arraycopy(cs, 0, newElements, len, added);
                setArray(newElements);
            }
            return added;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes all of the elements from this list.
     * The list will be empty after this call returns.
     */
    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            setArray(new Object[0]);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Appends all of the elements in the specified collection to the end
     * of this list, in the order that they are returned by the specified
     * collection's iterator.
     *
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     * @see #add(Object)
     */
    public boolean addAll(Collection<? extends E> c) {
        Object[] cs = (c.getClass() == CopyOnWriteArrayList.class) ?
            ((CopyOnWriteArrayList<?>)c).getArray() : c.toArray();
        if (cs.length == 0)
            return false;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len == 0 && cs.getClass() == Object[].class)
                setArray(cs);
            else {
                Object[] newElements = Arrays.copyOf(elements, len + cs.length);
                System.arraycopy(cs, 0, newElements, len, cs.length);
                setArray(newElements);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Inserts all of the elements in the specified collection into this
     * list, starting at the specified position.  Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in this list in the order that they are returned by the
     * specified collection's iterator.
     *
     * @param index index at which to insert the first element
     *        from the specified collection
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException if the specified collection is null
     * @see #add(int,Object)
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        Object[] cs = c.toArray();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (index > len || index < 0)
                throw new IndexOutOfBoundsException("Index: "+index+
                                                    ", Size: "+len);
            if (cs.length == 0)
                return false;
            int numMoved = len - index;
            Object[] newElements;
            if (numMoved == 0)
                newElements = Arrays.copyOf(elements, len + cs.length);
            else {
                newElements = new Object[len + cs.length];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index,
                                 newElements, index + cs.length,
                                 numMoved);
            }
            System.arraycopy(cs, 0, newElements, index, cs.length);
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void forEach(Consumer<? super E> action) {
        if (action == null) throw new NullPointerException();
        Object[] elements = getArray();
        int len = elements.length;
        for (int i = 0; i < len; ++i) {
            @SuppressWarnings("unchecked") E e = (E) elements[i];
            action.accept(e);
        }
    }

    public boolean removeIf(Predicate<? super E> filter) {
        if (filter == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (len != 0) {
                int newlen = 0;
                Object[] temp = new Object[len];
                for (int i = 0; i < len; ++i) {
                    @SuppressWarnings("unchecked") E e = (E) elements[i];
                    if (!filter.test(e))
                        temp[newlen++] = e;
                }
                if (newlen != len) {
                    setArray(Arrays.copyOf(temp, newlen));
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void replaceAll(UnaryOperator<E> operator) {
        if (operator == null) throw new NullPointerException();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            Object[] newElements = Arrays.copyOf(elements, len);
            for (int i = 0; i < len; ++i) {
                @SuppressWarnings("unchecked") E e = (E) elements[i];
                newElements[i] = operator.apply(e);
            }
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }

    public void sort(Comparator<? super E> c) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            Object[] newElements = Arrays.copyOf(elements, elements.length);
            @SuppressWarnings("unchecked") E[] es = (E[])newElements;
            Arrays.sort(es, c);
            setArray(newElements);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Saves this list to a stream (that is, serializes it).
     *
     * @param s the stream
     * @throws java.io.IOException if an I/O error occurs
     * @serialData The length of the array backing the list is emitted
     *               (int), followed by all of its elements (each an Object)
     *               in the proper order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {

        s.defaultWriteObject();

        Object[] elements = getArray();
        // Write out array length
        s.writeInt(elements.length);

        // Write out all elements in the proper order.
        for (Object element : elements)
            s.writeObject(element);
    }

    /**
     * Reconstitutes this list from a stream (that is, deserializes it).
     * @param s the stream
     * @throws ClassNotFoundException if the class of a serialized object
     *         could not be found
     * @throws java.io.IOException if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {

        s.defaultReadObject();

        // bind to new lock
        resetLock();

        // Read in array length and allocate array
        int len = s.readInt();
        Object[] elements = new Object[len];

        // Read in all elements in the proper order.
        for (int i = 0; i < len; i++)
            elements[i] = s.readObject();
        setArray(elements);
    }

    /**
     * Returns a string representation of this list.  The string
     * representation consists of the string representations of the list's
     * elements in the order they are returned by its iterator, enclosed in
     * square brackets ({@code "[]"}).  Adjacent elements are separated by
     * the characters {@code ", "} (comma and space).  Elements are
     * converted to strings as by {@link String#valueOf(Object)}.
     *
     * @return a string representation of this list
     */
    public String toString() {
        return Arrays.toString(getArray());
    }

    /**
     * Compares the specified object with this list for equality.
     * Returns {@code true} if the specified object is the same object
     * as this object, or if it is also a {@link List} and the sequence
     * of elements returned by an {@linkplain List#iterator() iterator}
     * over the specified list is the same as the sequence returned by
     * an iterator over this list.  The two sequences are considered to
     * be the same if they have the same length and corresponding
     * elements at the same position in the sequence are <em>equal</em>.
     * Two elements {@code e1} and {@code e2} are considered
     * <em>equal</em> if {@code (e1==null ? e2==null : e1.equals(e2))}.
     *
     * @param o the object to be compared for equality with this list
     * @return {@code true} if the specified object is equal to this list
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof List))
            return false;

        List<?> list = (List<?>)(o);
        Iterator<?> it = list.iterator();
        Object[] elements = getArray();
        int len = elements.length;
        for (int i = 0; i < len; ++i)
            if (!it.hasNext() || !eq(elements[i], it.next()))
                return false;
        if (it.hasNext())
            return false;
        return true;
    }

    /**
     * Returns the hash code value for this list.
     *
     * <p>This implementation uses the definition in {@link List#hashCode}.
     *
     * @return the hash code value for this list
     */
    public int hashCode() {
        int hashCode = 1;
        Object[] elements = getArray();
        int len = elements.length;
        for (int i = 0; i < len; ++i) {
            Object obj = elements[i];
            hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
        }
        return hashCode;
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * <p>The returned iterator provides a snapshot of the state of the list
     * when the iterator was constructed. No synchronization is needed while
     * traversing the iterator. The iterator does <em>NOT</em> support the
     * {@code remove} method.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    public Iterator<E> iterator() {
        return new COWIterator<E>(getArray(), 0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned iterator provides a snapshot of the state of the list
     * when the iterator was constructed. No synchronization is needed while
     * traversing the iterator. The iterator does <em>NOT</em> support the
     * {@code remove}, {@code set} or {@code add} methods.
     */
    public ListIterator<E> listIterator() {
        return new COWIterator<E>(getArray(), 0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned iterator provides a snapshot of the state of the list
     * when the iterator was constructed. No synchronization is needed while
     * traversing the iterator. The iterator does <em>NOT</em> support the
     * {@code remove}, {@code set} or {@code add} methods.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public ListIterator<E> listIterator(int index) {
        Object[] elements = getArray();
        int len = elements.length;
        if (index < 0 || index > len)
            throw new IndexOutOfBoundsException("Index: "+index);

        return new COWIterator<E>(elements, index);
    }

    /**
     * Returns a {@link Spliterator} over the elements in this list.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#IMMUTABLE},
     * {@link Spliterator#ORDERED}, {@link Spliterator#SIZED}, and
     * {@link Spliterator#SUBSIZED}.
     *
     * <p>The spliterator provides a snapshot of the state of the list
     * when the spliterator was constructed. No synchronization is needed while
     * operating on the spliterator.
     *
     * @return a {@code Spliterator} over the elements in this list
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator
            (getArray(), Spliterator.IMMUTABLE | Spliterator.ORDERED);
    }

    static final class COWIterator<E> implements ListIterator<E> {
        /** Snapshot of the array */
        private final Object[] snapshot;
        /** Index of element to be returned by subsequent call to next.  */
        private int cursor;

        private COWIterator(Object[] elements, int initialCursor) {
            cursor = initialCursor;
            snapshot = elements;
        }

        public boolean hasNext() {
            return cursor < snapshot.length;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        @SuppressWarnings("unchecked")
        public E next() {
            if (! hasNext())
                throw new NoSuchElementException();
            return (E) snapshot[cursor++];
        }

        @SuppressWarnings("unchecked")
        public E previous() {
            if (! hasPrevious())
                throw new NoSuchElementException();
            return (E) snapshot[--cursor];
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor-1;
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         * @throws UnsupportedOperationException always; {@code remove}
         *         is not supported by this iterator.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         * @throws UnsupportedOperationException always; {@code set}
         *         is not supported by this iterator.
         */
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         * @throws UnsupportedOperationException always; {@code add}
         *         is not supported by this iterator.
         */
        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            Object[] elements = snapshot;
            final int size = elements.length;
            for (int i = cursor; i < size; i++) {
                @SuppressWarnings("unchecked") E e = (E) elements[i];
                action.accept(e);
            }
            cursor = size;
        }
    }

    /**
     * Returns a view of the portion of this list between
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * The returned list is backed by this list, so changes in the
     * returned list are reflected in this list.
     *
     * <p>The semantics of the list returned by this method become
     * undefined if the backing list (i.e., this list) is modified in
     * any way other than via the returned list.
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex high endpoint (exclusive) of the subList
     * @return a view of the specified range within this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public List<E> subList(int fromIndex, int toIndex) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            if (fromIndex < 0 || toIndex > len || fromIndex > toIndex)
                throw new IndexOutOfBoundsException();
            return new COWSubList<E>(this, fromIndex, toIndex);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sublist for CopyOnWriteArrayList.
     * This class extends AbstractList merely for convenience, to
     * avoid having to define addAll, etc. This doesn't hurt, but
     * is wasteful.  This class does not need or use modCount
     * mechanics in AbstractList, but does need to check for
     * concurrent modification using similar mechanics.  On each
     * operation, the array that we expect the backing list to use
     * is checked and updated.  Since we do this for all of the
     * base operations invoked by those defined in AbstractList,
     * all is well.  While inefficient, this is not worth
     * improving.  The kinds of list operations inherited from
     * AbstractList are already so slow on COW sublists that
     * adding a bit more space/time doesn't seem even noticeable.
     */
    private static class COWSubList<E>
        extends AbstractList<E>
        implements RandomAccess
    {
        private final CopyOnWriteArrayList<E> l;
        private final int offset;
        private int size;
        private Object[] expectedArray;

        // only call this holding l's lock
        COWSubList(CopyOnWriteArrayList<E> list,
                   int fromIndex, int toIndex) {
            l = list;
            expectedArray = l.getArray();
            offset = fromIndex;
            size = toIndex - fromIndex;
        }

        // only call this holding l's lock
        private void checkForComodification() {
            if (l.getArray() != expectedArray)
                throw new ConcurrentModificationException();
        }

        // only call this holding l's lock
        private void rangeCheck(int index) {
            if (index < 0 || index >= size)
                throw new IndexOutOfBoundsException("Index: "+index+
                                                    ",Size: "+size);
        }

        public E set(int index, E element) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                E x = l.set(index+offset, element);
                expectedArray = l.getArray();
                return x;
            } finally {
                lock.unlock();
            }
        }

        public E get(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                return l.get(index+offset);
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                return size;
            } finally {
                lock.unlock();
            }
        }

        public void add(int index, E element) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (index < 0 || index > size)
                    throw new IndexOutOfBoundsException();
                l.add(index+offset, element);
                expectedArray = l.getArray();
                size++;
            } finally {
                lock.unlock();
            }
        }

        public void clear() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                l.removeRange(offset, offset+size);
                expectedArray = l.getArray();
                size = 0;
            } finally {
                lock.unlock();
            }
        }

        public E remove(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                rangeCheck(index);
                checkForComodification();
                E result = l.remove(index+offset);
                expectedArray = l.getArray();
                size--;
                return result;
            } finally {
                lock.unlock();
            }
        }

        public boolean remove(Object o) {
            int index = indexOf(o);
            if (index == -1)
                return false;
            remove(index);
            return true;
        }

        public Iterator<E> iterator() {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                return new COWSubListIterator<E>(l, 0, offset, size);
            } finally {
                lock.unlock();
            }
        }

        public ListIterator<E> listIterator(int index) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (index < 0 || index > size)
                    throw new IndexOutOfBoundsException("Index: "+index+
                                                        ", Size: "+size);
                return new COWSubListIterator<E>(l, index, offset, size);
            } finally {
                lock.unlock();
            }
        }

        public List<E> subList(int fromIndex, int toIndex) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                checkForComodification();
                if (fromIndex < 0 || toIndex > size || fromIndex > toIndex)
                    throw new IndexOutOfBoundsException();
                return new COWSubList<E>(l, fromIndex + offset,
                                         toIndex + offset);
            } finally {
                lock.unlock();
            }
        }

        public void forEach(Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            int lo = offset;
            int hi = offset + size;
            Object[] a = expectedArray;
            if (l.getArray() != a)
                throw new ConcurrentModificationException();
            if (lo < 0 || hi > a.length)
                throw new IndexOutOfBoundsException();
            for (int i = lo; i < hi; ++i) {
                @SuppressWarnings("unchecked") E e = (E) a[i];
                action.accept(e);
            }
        }

        public void replaceAll(UnaryOperator<E> operator) {
            if (operator == null) throw new NullPointerException();
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int lo = offset;
                int hi = offset + size;
                Object[] elements = expectedArray;
                if (l.getArray() != elements)
                    throw new ConcurrentModificationException();
                int len = elements.length;
                if (lo < 0 || hi > len)
                    throw new IndexOutOfBoundsException();
                Object[] newElements = Arrays.copyOf(elements, len);
                for (int i = lo; i < hi; ++i) {
                    @SuppressWarnings("unchecked") E e = (E) elements[i];
                    newElements[i] = operator.apply(e);
                }
                l.setArray(expectedArray = newElements);
            } finally {
                lock.unlock();
            }
        }

        public void sort(Comparator<? super E> c) {
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int lo = offset;
                int hi = offset + size;
                Object[] elements = expectedArray;
                if (l.getArray() != elements)
                    throw new ConcurrentModificationException();
                int len = elements.length;
                if (lo < 0 || hi > len)
                    throw new IndexOutOfBoundsException();
                Object[] newElements = Arrays.copyOf(elements, len);
                @SuppressWarnings("unchecked") E[] es = (E[])newElements;
                Arrays.sort(es, lo, hi, c);
                l.setArray(expectedArray = newElements);
            } finally {
                lock.unlock();
            }
        }

        public boolean removeAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements)
                        throw new ConcurrentModificationException();
                    int len = elements.length;
                    if (lo < 0 || hi > len)
                        throw new IndexOutOfBoundsException();
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        Object element = elements[i];
                        if (!c.contains(element))
                            temp[newSize++] = element;
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                         lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        public boolean retainAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements)
                        throw new ConcurrentModificationException();
                    int len = elements.length;
                    if (lo < 0 || hi > len)
                        throw new IndexOutOfBoundsException();
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        Object element = elements[i];
                        if (c.contains(element))
                            temp[newSize++] = element;
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                         lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        public boolean removeIf(Predicate<? super E> filter) {
            if (filter == null) throw new NullPointerException();
            boolean removed = false;
            final ReentrantLock lock = l.lock;
            lock.lock();
            try {
                int n = size;
                if (n > 0) {
                    int lo = offset;
                    int hi = offset + n;
                    Object[] elements = expectedArray;
                    if (l.getArray() != elements)
                        throw new ConcurrentModificationException();
                    int len = elements.length;
                    if (lo < 0 || hi > len)
                        throw new IndexOutOfBoundsException();
                    int newSize = 0;
                    Object[] temp = new Object[n];
                    for (int i = lo; i < hi; ++i) {
                        @SuppressWarnings("unchecked") E e = (E) elements[i];
                        if (!filter.test(e))
                            temp[newSize++] = e;
                    }
                    if (newSize != n) {
                        Object[] newElements = new Object[len - n + newSize];
                        System.arraycopy(elements, 0, newElements, 0, lo);
                        System.arraycopy(temp, 0, newElements, lo, newSize);
                        System.arraycopy(elements, hi, newElements,
                                         lo + newSize, len - hi);
                        size = newSize;
                        removed = true;
                        l.setArray(expectedArray = newElements);
                    }
                }
            } finally {
                lock.unlock();
            }
            return removed;
        }

        public Spliterator<E> spliterator() {
            int lo = offset;
            int hi = offset + size;
            Object[] a = expectedArray;
            if (l.getArray() != a)
                throw new ConcurrentModificationException();
            if (lo < 0 || hi > a.length)
                throw new IndexOutOfBoundsException();
            return Spliterators.spliterator
                (a, lo, hi, Spliterator.IMMUTABLE | Spliterator.ORDERED);
        }

    }

    private static class COWSubListIterator<E> implements ListIterator<E> {
        private final ListIterator<E> it;
        private final int offset;
        private final int size;

        COWSubListIterator(List<E> l, int index, int offset, int size) {
            this.offset = offset;
            this.size = size;
            it = l.listIterator(index+offset);
        }

        public boolean hasNext() {
            return nextIndex() < size;
        }

        public E next() {
            if (hasNext())
                return it.next();
            else
                throw new NoSuchElementException();
        }

        public boolean hasPrevious() {
            return previousIndex() >= 0;
        }

        public E previous() {
            if (hasPrevious())
                return it.previous();
            else
                throw new NoSuchElementException();
        }

        public int nextIndex() {
            return it.nextIndex() - offset;
        }

        public int previousIndex() {
            return it.previousIndex() - offset;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            int s = size;
            ListIterator<E> i = it;
            while (nextIndex() < s) {
                action.accept(i.next());
            }
        }
    }

    // Support for resetting lock while deserializing
    private void resetLock() {
        UNSAFE.putObjectVolatile(this, lockOffset, new ReentrantLock());
    }
    private static final sun.misc.Unsafe UNSAFE;
    private static final long lockOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = CopyOnWriteArrayList.class;
            lockOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("lock"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
