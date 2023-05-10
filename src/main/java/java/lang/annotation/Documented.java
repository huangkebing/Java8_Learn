package java.lang.annotation;

/**
 * 元注解 @Documented 是否生成文档
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// @Documented注解作用于注解声明
@Target(ElementType.ANNOTATION_TYPE)
public @interface Documented {
}
