package com.sk.common.utils;

import com.sk.domain.shared.enums.Role;

public class RoleUtil {
    public static boolean isAdmin(Role role) {
        if (role == null || role.getCode() == null) {
            return false;
        }
        return role.getCode().equals(Role.ADMIN.getCode());
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

    public static boolean isApiSubAdmin(Role role) {
        if (role == null || role.getCode() == null) {
            return false;
        }
        return role.getCode().equals(Role.API_SUB_ADMIN.getCode());
    }

    public static boolean isApiOperator(Role role) {
        if (role == null || role.getCode() == null) {
            return false;
        }
        return role.getCode().equals(Role.API_OPERATOR.getCode());
    }

    public static boolean isApiClient(Role role) {
        if (role == null || role.getCode() == null) {
            return false;
        }
        return role.getCode().equals(Role.API_CLIENT.getCode());
    }
}
