package java.lang.annotation;

/**
 * 元注解 @Target
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// 注解作用于注解声明
@Target(java.lang.annotation.ElementType.ANNOTATION_TYPE)
public @interface Target {
    /**
     * 可以应用注释类型的ElementType的数组
     */
    ElementType[] value();
}
