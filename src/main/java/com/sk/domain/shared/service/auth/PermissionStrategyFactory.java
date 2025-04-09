package com.sk.domain.shared.service.auth;

import com.sk.domain.mi.service.MiUserService;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionStrategyFactory {

    private final MiUserService userService;
    private final AuthorizationService authService;

    public PermissionStrategy getStrategy() {
        Role role = authService.getCurrentRole();

        if (role == Role.ADMIN) {
            if (authService.isThirdPartyAccessAllowed()) {
                return new AdminWithThirdPartyAccessStrategy(userService);
            }
            return new AdminPermissionStrategy();
        }

        if (role == Role.OPERATOR) {
            return new OperatorPermissionStrategy();
        }

        return new UserPermissionStrategy(userService, authService);
    }

}
