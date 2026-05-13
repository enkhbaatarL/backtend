package com.sk.domain.shared.service.permissions;

import com.sk.common.annotation.PermissionRule;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.strategy.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

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

        if (role == Role.API_SUB_ADMIN) {
            return new ApiSubAdminPermissionStrategy();
        }

        if (role == Role.API_PLUS_OPERATOR) {
            return new ApiPlusOperatorPermissionStrategy(authService);
        }

        return new UserPermissionStrategy(authService);
    }

    private boolean hasCondition(PermissionRule rule, PermissionCondition condition) {
        return Arrays.asList(rule.conditions()).contains(condition);
    }

}
