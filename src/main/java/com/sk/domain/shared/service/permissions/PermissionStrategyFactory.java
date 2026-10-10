package com.sk.domain.shared.service.permissions;

import com.sk.common.annotation.PermissionRule;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.strategy.AdminPermissionStrategy;
import com.sk.domain.shared.service.permissions.strategy.AdminWithThirdPartyAccessStrategy;
import com.sk.domain.shared.service.permissions.strategy.ApiPlusOperatorPermissionStrategy;
import com.sk.domain.shared.service.permissions.strategy.ApiSubAdminPermissionStrategy;
import com.sk.domain.shared.service.permissions.strategy.OperatorPermissionStrategy;
import com.sk.domain.shared.service.permissions.strategy.PermissionStrategy;
import com.sk.domain.shared.service.permissions.strategy.SKGasUserPermissionStrategy;
import com.sk.domain.shared.service.permissions.strategy.SystemPermissionStrategy;
import com.sk.domain.shared.service.permissions.strategy.UserPermissionStrategy;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionStrategyFactory {

    private final AuthorizationService authService;

    /**
     * [Rule 기반] 권한 체크
     */
    public PermissionStrategy getStrategy(PermissionRule[] rules) {

        // 현재 발급된 토근의 role
        Role role = authService.getCurrentRole();

        return Arrays.stream(rules)
            .filter(rule -> rule.role() == role)
            .findFirst()
            .map(rule -> resolveStrategy(role, rule))
            .orElseGet(() -> resolveStrategy(role, PermissionCondition.NONE));
    }

    /**
     * [Role + Condition 기반] 권한 체크, Legacy
     */
    public PermissionStrategy getStrategy(PermissionCondition condition) {

        // 현재 발급된 토근의 role
        Role role = authService.getCurrentRole();

        return resolveStrategy(role, condition);
    }


    /**
     * [Rule 기반] API 접근 가능 여부 체크
     * - PermissionRule 간: OR 조건
     * - PermissionRule 내부 role + conditions: AND 조건
     */
    public boolean canAccessApi(PermissionRule[] rules) {

        // 현재 발급된 토근의 role
        Role role = authService.getCurrentRole();

        if (matchesAnyRule(rules, role)) {
            return true;
        }

        // [운영자 서비스 접속] 타사접속 가능 운영자는 USER 규칙(조건 포함)으로도 평가한다
        return role == Role.OPERATOR && authService.isServiceAccessOperator() && matchesAnyRule(rules, Role.USER);
    }

    private boolean matchesAnyRule(PermissionRule[] rules, Role role) {
        return Arrays.stream(rules)
            .filter(rule -> rule.role() == role)
            .anyMatch(rule -> Arrays.stream(rule.conditions()).allMatch(condition -> isConditionSatisfied(role, condition)));
    }

    /**
     * Rule 기반 전략 객체 조회
     */
    private PermissionStrategy resolveStrategy(Role role, PermissionRule rule) {

        if (role == Role.ADMIN && hasCondition(rule, PermissionCondition.THIRD_PARTY_ADMIN_ONLY)) {
            return new AdminWithThirdPartyAccessStrategy(authService);
        }

        if (role == Role.USER && hasCondition(rule, PermissionCondition.SKGAS_COMPANY_USER_ONLY)) {
            return new SKGasUserPermissionStrategy(authService);
        }

        return resolveStrategy(role, PermissionCondition.NONE);
    }

    /**
     * Role + Condition 기반 전략 객체 조회
     */
    private PermissionStrategy resolveStrategy(Role role, PermissionCondition condition) {

        if (role == Role.ADMIN) {
            if (condition == PermissionCondition.THIRD_PARTY_ADMIN_ONLY) {
                return new AdminWithThirdPartyAccessStrategy(authService);
            }
            return new AdminPermissionStrategy();
        }

        if (role == Role.OPERATOR) {
            return new OperatorPermissionStrategy(authService);
        }

        if (role == Role.USER && condition == PermissionCondition.SKGAS_COMPANY_USER_ONLY) {
            return new SKGasUserPermissionStrategy(authService);
        }

        if (role == Role.API_SUB_ADMIN) {
            return new ApiSubAdminPermissionStrategy();
        }

        if (role == Role.API_PLUS_OPERATOR) {
            return new ApiPlusOperatorPermissionStrategy(authService);
        }

        if (role == Role.SYSTEM) {
            return new SystemPermissionStrategy();
        }

        return new UserPermissionStrategy(authService);
    }

    private boolean hasCondition(PermissionRule rule, PermissionCondition condition) {
        return Arrays.asList(rule.conditions()).contains(condition);
    }

    private boolean isConditionSatisfied(Role role, PermissionCondition condition) {
        return switch (condition) {
            case NONE -> true;
            case THIRD_PARTY_ADMIN_ONLY -> role == Role.ADMIN && authService.isThirdPartyAccessAllowed();
            case SKGAS_COMPANY_USER_ONLY -> role == Role.USER && PermissionCondition.AllowedCompany.SKGAS.getCode().equals(authService.getCompanyCd());
            default -> throw new IllegalStateException("지원하지 않는 PermissionCondition 값입니다: " + condition);
        };
    }

}
