package com.sk.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 실행 시 복합 권한 규칙 검증을 위한 어노테이션
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredPermissionRules {

    /**
     * 복합 권한 규칙
     * - PermissionRule 간: OR 조건
     * - PermissionRule 내부 role + conditions: AND 조건
     */
    PermissionRule[] rules() default {};

    /**
     * 상품 번호를 파라미터에서 가져올 경우 파라미터 이름
     */
    String goodsParam() default "";

    /**
     * 상품 하위 엔티티 번호를 파라미터에서 가져올 경우 파라미터 이름
     * - ex) fileNo, goodsDataSourceNo
     */
    String goodsNestedParam() default "";

    /**
     * 상품 VIEW 권한 필요 여부
     * - true: 해당 상품에 대한 VIEW 권한이 필요함
     */
    boolean requireViewPermission() default false;

    /**
     * 상품 DOWNLOAD 권한 필요 여부
     * - true: 해당 상품에 대한 DOWNLOAD 권한이 필요함
     */
    boolean requireDownloadPermission() default false;

}
