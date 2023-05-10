package java.lang.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 元注解 继承注解。若A注解中有@Inherited，A注解作用于父类，则在子类中A注解也生效
 */
@Documented
// 运行时有效
@Retention(RetentionPolicy.RUNTIME)
// 作用于注解定义声明
@Target(ElementType.ANNOTATION_TYPE)
public @interface Inherited {
}
