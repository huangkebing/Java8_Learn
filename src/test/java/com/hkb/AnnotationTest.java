package com.hkb;

import org.junit.Test;

import java.lang.annotation.*;

@TypeAnno(value = "abc", name = "annoTest")
public class AnnotationTest {
    @Test
    public void test1(){
        Class<AnnotationTest> annotationTestClass = AnnotationTest.class;
        TypeAnno typeAnnoClass = annotationTestClass.getAnnotation(TypeAnno.class);
        Class<? extends Annotation> type = typeAnnoClass.annotationType();
        System.out.println(typeAnnoClass.name());
    }
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface TypeAnno{
    String value();
    String name();
}
