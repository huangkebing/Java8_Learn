package java.lang.annotation;

/**
 * 所有注解都继承的接口
 */
public interface Annotation {

    boolean equals(Object obj);

    int hashCode();

    String toString();

    /**
     * 获得注解的Class对象
     */
    Class<? extends Annotation> annotationType();
}
