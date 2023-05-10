package java.lang.annotation;

/**
 * 定义了注解的保留时期
 */
@Documented
// 注解在运行时有效
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
// 注解作用于注解声明
@Target(ElementType.ANNOTATION_TYPE)
public @interface Retention {
    /**
     * 保留策略，只能定义一个
     */
    RetentionPolicy value();
}
