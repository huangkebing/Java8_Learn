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


    //--------------批量操作--------------
    /**
     * 如果此集合包含指定集合中的所有元素，则返回 true。
     *
     * @param  c 要检查是否在此集合中的元素的集合
     * @return 如果此集合包含指定集合中的所有元素，则为 true
     * @throws ClassCastException 如果指定集合中的一个或多个元素的类型与此集合不兼容
     * @throws NullPointerException 如果指定的集合包含一个或多个null元素，并且此集合不允许null元素或者，如果指定的集合为null
     * @see    #contains(Object)
     */
    boolean containsAll(Collection<?> c);

    /**
     * 将指定集合中的所有元素添加到此集合（可选操作）
     *
     * @param c 包含要添加到此集合的元素的集合
     * @return 如果此集合因调用而更改，则为true
     * @throws UnsupportedOperationException 如果此集合不支持 addAll 操作
     * @throws ClassCastException 如果指定集合的元素的类阻止将其添加到此集合
     * @throws NullPointerException 如果指定的集合包含 null 元素，并且此集合不允许 null 元素，或者指定的集合为 null
     * @throws IllegalArgumentException 如果指定集合的元素的某些属性阻止将其添加到此集合
     * @throws IllegalStateException 如果由于插入限制，此时无法添加所有元素
     * @see #add(Object)
     */
    boolean addAll(Collection<? extends E> c);

    /**
     * 删除此集合中所有包含在指定集合中的元素(可选操作)。此调用返回后，此集合将不包含与指定集合相同的元素
     *
     * @param c 包含要从此集合中删除的元素的集合
     * @return 如果此集合因调用而更改，则为true
     * @throws UnsupportedOperationException 如果此集合不支持removeAll方法
     * @throws ClassCastException 如果此集合中一个或多个元素的类型与指定的集合不兼容
     * @throws NullPointerException 如果此集合包含一个或多个null元素，并且指定的集合不支持null元素，或者如果指定的集合为null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    boolean removeAll(Collection<?> c);

    /**
     * 删除此集合中满足给定predicate(即predicate判断为true)的所有元素
     * 循环期间发生的错误或运行时异常将传递给调用方
     *
     * 实现规范：
     * 默认实现使用其迭代器遍历集合的所有元素。符合的元素使用迭代器的remove方法移除
     * 如果集合的迭代器不支持删除，则会在第一个匹配元素上抛出{@code UnsupportedOperationException}
     *
     * @param filter 一个predicate，它为要删除的元素返回{@code true}
     * @return {@code true}如果删除了任何一个元素
     * @throws NullPointerException 如果指定的filter为空
     * @throws UnsupportedOperationException 如果无法从此集合中删除元素。如果无法删除匹配元素，或者通常不支持删除，则实现可能会引发此异常
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
     * 仅保留此集合中包含在给定集合中的元素 (可选操作)
     *
     * @param c 包含要保留在此集合中的元素的集合
     * @return 如果此集合因调用而更改，则为 true
     * @throws UnsupportedOperationException 如果此集合不支持 retainAll 操作
     * @throws ClassCastException 如果此集合中一个或多个元素的类型与给定的集合不兼容
     * @throws NullPointerException 如果此集合包含一个或多个null元素，并且指定的集合不允许null元素，或者如果指定的集合为null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    boolean retainAll(Collection<?> c);

    /**
     * 从此集合中删除所有元素 (可选操作)，此方法返回后，集合将为空。
     *
     * @throws UnsupportedOperationException 如果此集合不支持清除操作
     */
    void clear();


    //--------------比较和哈希--------------
    /**
     * 参考Object.equals()
     * @param o 要与此集合比较的对象
     * @return 如果指定的对象等于此集合，则为 true
     * @see Object#equals(Object)
     * @see Set#equals(Object)
     * @see List#equals(Object)
     */
    boolean equals(Object o);

    /**
     * 返回此集合的哈希值
     * 应该注意，任何重写Object.equals方法的类也必须重写Object.hashCode方法，以满足Object.hashCode方法的一般协定
     * 特别是，c1.equals(c2)意味着c1.hashCode()==c2.hashCode()
     *
     * @return 此集合的哈希值
     * @see Object#hashCode()
     * @see Object#equals(Object)
     */
    int hashCode();

    /**
     * 在此集合中的元素上创建{@link Spliterator}
     *
     * @return 此集合中元素上的{@code Spliterator}
     * @since 1.8
     */
    @Override
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, 0);
    }

    /**
     * 返回一个连续的Stream，并将此集合作为其源
     *
     * @return 返回一个连续的Stream
     * @since 1.8
     */
    default Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * 返回一个可能并行的Stream，并将此集合作为其源。此方法允许返回顺序流。
     *
     * @return 此集合中的元素上可能并行的Stream
     * @since 1.8
     */
    default Stream<E> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }
}
