package com.sk.common.utils;

import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.Role;

public class RoleUtil {
    public static boolean isAdmin(User user) {
        if (user == null || user.getUserRoleCd() == null) {
            return false;
        }
        return user.getUserRoleCd().getCode() == Role.ADMIN.getCode();
    }

    public static boolean isSubAdmin(User user) {
        if (user == null || user.getUserRoleCd() == null) {
            return false;
        }
        return user.getUserRoleCd().getCode() == Role.SUB_ADMIN.getCode();
    }

    public static boolean isOperator(User user) {
        if (user == null || user.getUserRoleCd() == null) {
            return false;
        }
        return user.getUserRoleCd().getCode() == Role.OPERATOR.getCode();
    }

    public static boolean isUser(User user) {
        if (user == null || user.getUserRoleCd() == null) {
            return false;
        }
        return user.getUserRoleCd().getCode() == Role.USER.getCode();
    }

    public static boolean isClient(User user) {
        if (user == null || user.getUserRoleCd() == null) {
            return false;
        }
        return user.getUserRoleCd().getCode() == Role.CLIENT.getCode();
    }
}
