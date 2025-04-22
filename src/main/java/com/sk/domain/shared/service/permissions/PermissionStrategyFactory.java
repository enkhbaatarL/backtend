package com.sk.domain.shared.service.permissions;

import com.sk.domain.mi.service.MiUserService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.strategy.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionStrategyFactory {

    private final MiUserService userService;
    private final AuthorizationService authorizationService;

    public PermissionStrategy getStrategy(PermissionCondition condition) {

        // 현재 발급된 토근의 role
        Role role = authorizationService.getCurrentRole();

        if (role == Role.ADMIN) {
            if (condition == PermissionCondition.THIRD_PARTY_ADMIN_ONLY) {
                return new AdminWithThirdPartyAccessStrategy(userService, authorizationService);
            }
            return new AdminPermissionStrategy();
        }

        if (role == Role.OPERATOR) {
            return new OperatorPermissionStrategy();
        }

        if (role == Role.API_SUB_ADMIN) {
            return new ApiSubAdminPermissionStrategy();
        }

        if (role == Role.API_PLUS_OPERATOR) {
            return new ApiPlusOperatorPermissionStrategy(userService, authorizationService);
        }

        return new UserPermissionStrategy(userService, authorizationService);
    }

}
