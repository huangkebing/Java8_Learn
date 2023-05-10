package java.lang.annotation;

/**
 * 定义注解的生效时期，配合@Retention使用
 */
public enum RetentionPolicy {
    /**
     * 注解会在编译后失效
     */
    SOURCE,

    /**
     * 编译后依然有效，运行时不会被VM保留，运行时不生效
     */
    CLASS,

    /**
     * 运行时会被VM保留，运行时生效，可以被反射获取到
     */
    RUNTIME
}
