package com.sk.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 문자열 파라미터에 대해 양쪽 공백을 제거하고 빈 문자열을 허용하지 않는 어노테이션
 * 
 * null -> null
 * "" -> null
 * " " -> null
 * "  data  " -> "data"
 * "data" -> "data"
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrimmedNotEmpty {
    // @RequestParam과 함께 사용할 속성
    String value() default "";
    boolean required() default false;
    String defaultValue() default "";
}