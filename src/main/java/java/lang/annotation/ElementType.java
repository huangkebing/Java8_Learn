package java.lang.annotation;

/**
 * 注解可使用类型，配合@Target
 */
public enum ElementType {
    /** 类、接口、注解、枚举类型声明*/
    TYPE,

    /** 字段(包含枚举常量)类型声明*/
    FIELD,

    /** 方法类型声明 */
    METHOD,

    /** 参数声明 */
    PARAMETER,

    /** 构造器声明 */
    CONSTRUCTOR,

    /** 局部变量声明 */
    LOCAL_VARIABLE,

    /** 注解类型声明 */
    ANNOTATION_TYPE,

    /** 包声明 */
    PACKAGE,

    /**
     * 类型参数声明 1.8新增
     */
    TYPE_PARAMETER,

    /**
     * 使用类型 1.8新增
     */
    TYPE_USE
}
