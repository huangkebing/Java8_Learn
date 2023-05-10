package java.util;

import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 集合结构中的根接口。集合表示一组对象，这些对象称为元素。
 * 某些集合允许重复元素，而某些集合则不允许，有些是有序的，有些是无序的。
 * JDK 不提供此接口的任何直接实现：它提供了更具体的子接口（如Set和List）的实现。
 * 此接口通常用于传递集合，并在需要最大通用性的地方操作它们。
 *
 * 所有通用Collection实现类（通常通过其子接口之一如List、Set间接实现 Collection）都应提供两个“标准”构造函数：
 * 一个 void（无参数）构造函数，用于创建一个空集合，
 * 以及一个有参构造函数，该构造函数具有单个Collection的参数，该构造函数创建与其参数具有相同元素的新集合
 * 实际上，第二个构造函数允许用户复制任何集合，生成所需实现类型的等效集合
 * 接口中无法强制实施此约定（因为接口不能包含构造函数），但Java平台库中的所有通用集合实现都符合此约定
 *
 * @param <E> 此集合中元素的类型
 * @since 1.2
 */

public interface Collection<E> extends Iterable<E> {
    //--------------查询操作----------------
    /**
     * 返回此集合中的元素数。如果此集合包含的元素数量超过Integer.MAX_VALUE，则返回Integer.MAX_VALUE
     */
    int size();

    /**
     * 如果此集合不包含任何元素，则返回 true
     */
    boolean isEmpty();

    /**
     * 如果此集合包含指定的元素，则返回 true
     * 更正式地说，当且仅当此集合包含至少一个元素e时返回true，使得o==null？e==null:o.equals(e)
     *
     * @param o 要测试其在此集合中的是否存在的元素
     * @return 如果此集合包含指定的元素，则返回 true
     * @throws ClassCastException 如果指定元素的类型与此集合不兼容
     * @throws NullPointerException 如果指定的元素为 null，并且此集合不允许 null 元素
     */
    boolean contains(Object o);

    /**
     * 返回此集合中元素的迭代器。对于元素的返回顺序没有保证（除非此集合是提供顺序保证的集合实例）
     */
    Iterator<E> iterator();

    /**
     * 返回一个包含此集合中所有元素的数组。如果此集合对其迭代器返回其元素的顺序做出任何保证，则此方法必须以相同的顺序返回元素
     * 此方法会分配一个新数组，即使这个集合底层是依托数组实现的。因此，调用方修改数组不会影响集合
     * 此方法充当数组和集合之间转化的桥梁
     *
     * @return 包含此集合中所有元素的数组
     */
    Object[] toArray();

    /**
     * 返回包含此集合中所有元素的数组，返回数组的运行时类型是指定数组的运行时类型。
     * 如果集合的元素数量在指定的数组中足够存放，则在其中返回该集合。否则，将使用指定数组的运行时类型和集合的大小分配一个新数组
     *
     * 如果数组的元素多于集合，则紧跟在集合末尾之后的数组中的元素会被设置为null
     * 仅当调用方确定此集合不包含任何null元素时，这才可用于确定此集合的长度
     *
     * 如果此集合对其迭代器返回其元素的顺序做出任何保证，则此方法必须以相同的顺序返回元素
     * 与toArray()方法一样，此方法充当基于数组和基于集合的 API 之间的桥梁
     * 此外，该方法允许精确控制输出数组的运行时类型，并且在某些情况下可用于节省分配成本
     *
     * 假设x是一个已知只包含字符串的集合。以下代码可用于将集合转储到新分配的String数组中：
     * String[] y = x.toArray(new String[0]);
     * 请注意，toArray(new Object[0])在方法上与toArray()相同
     *
     * @param <T> 要包含集合的数组的运行时类型
     * @param a 存储此集合的元素的数组（如果它足够大）;否则，将分配相同运行时类型的新数组
     * @return 包含此集合中所有元素的数组
     * @throws ArrayStoreException 如果指定数组的运行时类型不是此集合中每个元素的运行时类型的超类
     * @throws NullPointerException 如果指定的数组为空
     */
    <T> T[] toArray(T[] a);

    //--------------修改操作--------------
    /**
     * 可选择实现操作
     * 如果此集合因调用而更改，则返回true；如果此集合不允许重复且已包含指定的元素，则返回false
     * 支持此操作的集合可能会限制可以添加到此集合的元素,集合类应在其文档中明确指定对可以添加哪些元素的任何限制
     * 如果集合除了已经包含该元素外的任何原因拒绝添加特定元素，则必须引发异常，而不是返回false
     *
     * @param e 要确保其在此集合中的存在元素
     * @return 如果此集合因调用而更改，则为 true
     * @throws UnsupportedOperationException 如果此集合不支持添加操作
     * @throws ClassCastException 如果指定元素的类阻止将其添加到此集合
     * @throws NullPointerException 如果指定的元素为null，并且此集合不允许null元素
     * @throws IllegalArgumentException 如果元素的某些属性阻止将其添加到此集合
     * @throws IllegalStateException 如果由于插入限制而此时无法添加元素
     */
    boolean add(E e);

    /**
     * 可选择实现操作
     * 从此集合中删除指定元素的单个实例（如果存在），更正式地说，删除元素e，符合要求(o==null?e==null:o.equals(e))
     * 如果此集合包含指定的元素（如果此集合由于调用而更改），则返回 true
     *
     * @param o 要从此集合中删除的元素（如果存在）
     * @return 如果由于此调用而删除了元素，则为true
     * @throws ClassCastException 如果指定元素的类型与此集合不兼容
     * @throws NullPointerException 如果指定的元素为null，并且此集合不允许null元素
     * @throws UnsupportedOperationException 如果此集合不支持删除操作
     */
    boolean remove(Object o);


    // Bulk Operations

    /**
     * Returns <tt>true</tt> if this collection contains all of the elements
     * in the specified collection.
     *
     * @param  c collection to be checked for containment in this collection
     * @return <tt>true</tt> if this collection contains all of the elements
     *         in the specified collection
     * @throws ClassCastException if the types of one or more elements
     *         in the specified collection are incompatible with this
     *         collection
     *         (<a href="#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified collection contains one
     *         or more null elements and this collection does not permit null
     *         elements
     *         (<a href="#optional-restrictions">optional</a>),
     *         or if the specified collection is null.
     * @see    #contains(Object)
     */
    boolean containsAll(Collection<?> c);

    /**
     * Adds all of the elements in the specified collection to this collection
     * (optional operation).  The behavior of this operation is undefined if
     * the specified collection is modified while the operation is in progress.
     * (This implies that the behavior of this call is undefined if the
     * specified collection is this collection, and this collection is
     * nonempty.)
     *
     * @param c collection containing elements to be added to this collection
     * @return <tt>true</tt> if this collection changed as a result of the call
     * @throws UnsupportedOperationException if the <tt>addAll</tt> operation
     *         is not supported by this collection
     * @throws ClassCastException if the class of an element of the specified
     *         collection prevents it from being added to this collection
     * @throws NullPointerException if the specified collection contains a
     *         null element and this collection does not permit null elements,
     *         or if the specified collection is null
     * @throws IllegalArgumentException if some property of an element of the
     *         specified collection prevents it from being added to this
     *         collection
     * @throws IllegalStateException if not all the elements can be added at
     *         this time due to insertion restrictions
     * @see #add(Object)
     */
    boolean addAll(Collection<? extends E> c);

    /**
     * Removes all of this collection's elements that are also contained in the
     * specified collection (optional operation).  After this call returns,
     * this collection will contain no elements in common with the specified
     * collection.
     *
     * @param c collection containing elements to be removed from this collection
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call
     * @throws UnsupportedOperationException if the <tt>removeAll</tt> method
     *         is not supported by this collection
     * @throws ClassCastException if the types of one or more elements
     *         in this collection are incompatible with the specified
     *         collection
     *         (<a href="#optional-restrictions">optional</a>)
     * @throws NullPointerException if this collection contains one or more
     *         null elements and the specified collection does not support
     *         null elements
     *         (<a href="#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    boolean removeAll(Collection<?> c);

    /**
     * Removes all of the elements of this collection that satisfy the given
     * predicate.  Errors or runtime exceptions thrown during iteration or by
     * the predicate are relayed to the caller.
     *
     * @implSpec
     * The default implementation traverses all elements of the collection using
     * its {@link #iterator}.  Each matching element is removed using
     * {@link Iterator#remove()}.  If the collection's iterator does not
     * support removal then an {@code UnsupportedOperationException} will be
     * thrown on the first matching element.
     *
     * @param filter a predicate which returns {@code true} for elements to be
     *        removed
     * @return {@code true} if any elements were removed
     * @throws NullPointerException if the specified filter is null
     * @throws UnsupportedOperationException if elements cannot be removed
     *         from this collection.  Implementations may throw this exception if a
     *         matching element cannot be removed or if, in general, removal is not
     *         supported.
     * @since 1.8
     */
    default boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        boolean removed = false;
        final Iterator<E> each = iterator();
        while (each.hasNext()) {
            if (filter.test(each.next())) {
                each.remove();
                removed = true;
            }
        }
        return removed;
    }

    /**
     * Retains only the elements in this collection that are contained in the
     * specified collection (optional operation).  In other words, removes from
     * this collection all of its elements that are not contained in the
     * specified collection.
     *
     * @param c collection containing elements to be retained in this collection
     * @return <tt>true</tt> if this collection changed as a result of the call
     * @throws UnsupportedOperationException if the <tt>retainAll</tt> operation
     *         is not supported by this collection
     * @throws ClassCastException if the types of one or more elements
     *         in this collection are incompatible with the specified
     *         collection
     *         (<a href="#optional-restrictions">optional</a>)
     * @throws NullPointerException if this collection contains one or more
     *         null elements and the specified collection does not permit null
     *         elements
     *         (<a href="#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    boolean retainAll(Collection<?> c);

    /**
     * Removes all of the elements from this collection (optional operation).
     * The collection will be empty after this method returns.
     *
     * @throws UnsupportedOperationException if the <tt>clear</tt> operation
     *         is not supported by this collection
     */
    void clear();


    //--------------比较和哈希--------------

    /**
     * Compares the specified object with this collection for equality. <p>
     *
     * While the <tt>Collection</tt> interface adds no stipulations to the
     * general contract for the <tt>Object.equals</tt>, programmers who
     * implement the <tt>Collection</tt> interface "directly" (in other words,
     * create a class that is a <tt>Collection</tt> but is not a <tt>Set</tt>
     * or a <tt>List</tt>) must exercise care if they choose to override the
     * <tt>Object.equals</tt>.  It is not necessary to do so, and the simplest
     * course of action is to rely on <tt>Object</tt>'s implementation, but
     * the implementor may wish to implement a "value comparison" in place of
     * the default "reference comparison."  (The <tt>List</tt> and
     * <tt>Set</tt> interfaces mandate such value comparisons.)<p>
     *
     * The general contract for the <tt>Object.equals</tt> method states that
     * equals must be symmetric (in other words, <tt>a.equals(b)</tt> if and
     * only if <tt>b.equals(a)</tt>).  The contracts for <tt>List.equals</tt>
     * and <tt>Set.equals</tt> state that lists are only equal to other lists,
     * and sets to other sets.  Thus, a custom <tt>equals</tt> method for a
     * collection class that implements neither the <tt>List</tt> nor
     * <tt>Set</tt> interface must return <tt>false</tt> when this collection
     * is compared to any list or set.  (By the same logic, it is not possible
     * to write a class that correctly implements both the <tt>Set</tt> and
     * <tt>List</tt> interfaces.)
     *
     * @param o object to be compared for equality with this collection
     * @return <tt>true</tt> if the specified object is equal to this
     * collection
     *
     * @see Object#equals(Object)
     * @see Set#equals(Object)
     * @see List#equals(Object)
     */
    boolean equals(Object o);

    /**
     * Returns the hash code value for this collection.  While the
     * <tt>Collection</tt> interface adds no stipulations to the general
     * contract for the <tt>Object.hashCode</tt> method, programmers should
     * take note that any class that overrides the <tt>Object.equals</tt>
     * method must also override the <tt>Object.hashCode</tt> method in order
     * to satisfy the general contract for the <tt>Object.hashCode</tt> method.
     * In particular, <tt>c1.equals(c2)</tt> implies that
     * <tt>c1.hashCode()==c2.hashCode()</tt>.
     *
     * @return the hash code value for this collection
     *
     * @see Object#hashCode()
     * @see Object#equals(Object)
     */
    int hashCode();

    /**
     * Creates a {@link Spliterator} over the elements in this collection.
     *
     * Implementations should document characteristic values reported by the
     * spliterator.  Such characteristic values are not required to be reported
     * if the spliterator reports {@link Spliterator#SIZED} and this collection
     * contains no elements.
     *
     * <p>The default implementation should be overridden by subclasses that
     * can return a more efficient spliterator.  In order to
     * preserve expected laziness behavior for the {@link #stream()} and
     * {@link #parallelStream()}} methods, spliterators should either have the
     * characteristic of {@code IMMUTABLE} or {@code CONCURRENT}, or be
     * <em><a href="Spliterator.html#binding">late-binding</a></em>.
     * If none of these is practical, the overriding class should describe the
     * spliterator's documented policy of binding and structural interference,
     * and should override the {@link #stream()} and {@link #parallelStream()}
     * methods to create streams using a {@code Supplier} of the spliterator,
     * as in:
     * <pre>{@code
     *     Stream<E> s = StreamSupport.stream(() -> spliterator(), spliteratorCharacteristics)
     * }</pre>
     * <p>These requirements ensure that streams produced by the
     * {@link #stream()} and {@link #parallelStream()} methods will reflect the
     * contents of the collection as of initiation of the terminal stream
     * operation.
     *
     * @implSpec
     * The default implementation creates a
     * <em><a href="Spliterator.html#binding">late-binding</a></em> spliterator
     * from the collections's {@code Iterator}.  The spliterator inherits the
     * <em>fail-fast</em> properties of the collection's iterator.
     * <p>
     * The created {@code Spliterator} reports {@link Spliterator#SIZED}.
     *
     * @implNote
     * The created {@code Spliterator} additionally reports
     * {@link Spliterator#SUBSIZED}.
     *
     * <p>If a spliterator covers no elements then the reporting of additional
     * characteristic values, beyond that of {@code SIZED} and {@code SUBSIZED},
     * does not aid clients to control, specialize or simplify computation.
     * However, this does enable shared use of an immutable and empty
     * spliterator instance (see {@link Spliterators#emptySpliterator()}) for
     * empty collections, and enables clients to determine if such a spliterator
     * covers no elements.
     *
     * @return a {@code Spliterator} over the elements in this collection
     * @since 1.8
     */
    @Override
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, 0);
    }

    /**
     * Returns a sequential {@code Stream} with this collection as its source.
     *
     * <p>This method should be overridden when the {@link #spliterator()}
     * method cannot return a spliterator that is {@code IMMUTABLE},
     * {@code CONCURRENT}, or <em>late-binding</em>. (See {@link #spliterator()}
     * for details.)
     *
     * @implSpec
     * The default implementation creates a sequential {@code Stream} from the
     * collection's {@code Spliterator}.
     *
     * @return a sequential {@code Stream} over the elements in this collection
     * @since 1.8
     */
    default Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Returns a possibly parallel {@code Stream} with this collection as its
     * source.  It is allowable for this method to return a sequential stream.
     *
     * <p>This method should be overridden when the {@link #spliterator()}
     * method cannot return a spliterator that is {@code IMMUTABLE},
     * {@code CONCURRENT}, or <em>late-binding</em>. (See {@link #spliterator()}
     * for details.)
     *
     * @implSpec
     * The default implementation creates a parallel {@code Stream} from the
     * collection's {@code Spliterator}.
     *
     * @return a possibly parallel {@code Stream} over the elements in this
     * collection
     * @since 1.8
     */
    default Stream<E> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }
}
