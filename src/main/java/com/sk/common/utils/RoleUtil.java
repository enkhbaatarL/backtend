package com.sk.common.utils;

import com.sk.domain.shared.enums.Role;

public class RoleUtil {
    public static boolean isAdmin(Role role) {
        if (role == null || role.getCode() == null) {
            return false;
        }
        return role.getCode().equals(Role.ADMIN.getCode());
    }

    public static boolean isSubAdmin(Role role) {
        if (role == null || role.getCode() == null) {
            return false;
        }
        return role.getCode().equals(Role.SUB_ADMIN.getCode());
    }

    public static boolean isOperator(Role role) {
        if (role == null || role.getCode() == null) {
            return false;
        }
        return role.getCode().equals(Role.OPERATOR.getCode());
    }

    public static boolean isUser(Role role) {
        if (role == null || role.getCode() == null) {
            return false;
        }
        return role.getCode().equals(Role.USER.getCode());
    }

    public static boolean isClient(Role role) {
        if (role == null || role.getCode() == null) {
            return false;
        }
        return role.getCode().equals(Role.CLIENT.getCode());
    }
}
