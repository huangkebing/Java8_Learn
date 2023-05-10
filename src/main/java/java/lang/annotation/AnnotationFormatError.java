package java.lang.annotation;

/**
 * 该错误在 java.lang.reflect.AnnotatedElement 反射获取注解时，可能会被抛出
 */
public class AnnotationFormatError extends Error {
    private static final long serialVersionUID = -4256701562333669892L;

    public AnnotationFormatError(String message) {
        super(message);
    }

    public AnnotationFormatError(String message, Throwable cause) {
        super(message, cause);
    }

    public AnnotationFormatError(Throwable cause) {
        super(cause);
    }
}
