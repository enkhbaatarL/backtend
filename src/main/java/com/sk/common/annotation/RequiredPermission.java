package com.sk.common.annotation;

import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 실행 시 권한 검증을 위한 어노테이션
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredPermission {

    /**
     * 필요한 역할
     */
    Role[] roles() default {};

    /**
     * 회사 번호를 파라미터에서 가져올 경우 파라미터 이름
     */
    String companyParam() default "";

    /**
     * 사용자 번호를 파라미터에서 가져올 경우 파라미터 이름
     */
    String userParam() default "";

    /**
     * 프로바이더 번호를 파라미터에서 가져올 경우 파라미터 이름
     */
    String providerParam() default "";

    /**
     * 상품 번호를 파라미터에서 가져올 경우 파라미터 이름
     */
    String goodsParam() default "";

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

    /**
     * 부가 조건 지정
     */
    PermissionCondition condition() default PermissionCondition.NONE;

}
