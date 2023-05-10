package java.lang.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 使用 @Native 注解修饰成员变量，则表示这个变量可以被本地代码引用，常常被代码生成工具使用。
 * 如Integer类中的MIN_VALUE : @Native public static final int MIN_VALUE = 0x80000000;
 */
@Documented
// 作用于字段类型声明
@Target(ElementType.FIELD)
// 编译后失效
@Retention(RetentionPolicy.SOURCE)
public @interface Native {
}
