package com.sk.domain.shared.service.permissions;

import com.sk.common.annotation.PermissionRule;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PermissionMessageFormatter {

    public String allowedOnlyFor(PermissionRule[] rules) {
        return "This API is allowed only for %s.".formatted(describeRules(rules));
    }

    public String allowedOnlyFor(Role[] roles, PermissionCondition condition) {
        return "This API is allowed only for %s.".formatted(describeRoles(roles, condition));
    }

    private String describeRules(PermissionRule[] rules) {
        return Arrays.stream(rules)
            .map(this::describeRule)
            .collect(Collectors.joining(" or "));
    }

    private String describeRule(PermissionRule rule) {
        if (rule.role() == Role.USER && hasCondition(rule, PermissionCondition.SKGAS_COMPANY_USER_ONLY)) {
            return "SKGAS USER";
        }
        if (rule.role() == Role.ADMIN && hasCondition(rule, PermissionCondition.THIRD_PARTY_ADMIN_ONLY)) {
            return "third-party access ADMIN";
        }
        return rule.role().getCode();
    }

    private String describeRoles(Role[] roles, PermissionCondition condition) {
        return Arrays.stream(roles)
            .map(role -> describeRole(role, condition))
            .collect(Collectors.joining(" or "));
    }

    private String describeRole(Role role, PermissionCondition condition) {
        if (role == Role.ADMIN && condition == PermissionCondition.THIRD_PARTY_ADMIN_ONLY) {
            return "third-party access ADMIN";
        }
        return role.getCode();
    }

    private boolean hasCondition(PermissionRule rule, PermissionCondition condition) {
        return Arrays.asList(rule.conditions()).contains(condition);
    }

}
