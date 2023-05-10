package java.lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示方法重写了父类方法
 */
// 作用于方法声明
@Target(ElementType.METHOD)
// 编译后失效
@Retention(RetentionPolicy.SOURCE)
public @interface Override {
}
