package com.sk.common.annotation;

import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 권한 규칙 단위 어노테이션
 * - role + conditions
 * - PermissionRule.conditions 간 `AND` 규칙
 * - PermissionRule 간 `OR` 규칙
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionRule {

    /**
     * 허용 역할
     */
    Role role();

    /**
     * 부가 조건 (AND: 모든 조건을 충족해야 함)
     */
    PermissionCondition[] conditions() default {};

}
