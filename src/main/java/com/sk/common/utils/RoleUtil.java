package com.sk.common.utils;

import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;

public class RoleUtil {
    public static boolean isAdmin(User user) {
        if (user == null || user.getUserRoleCd() == null) {
            return false;
        }
        return user.getUserRoleCd().getCode() == UserRoleCd.ADMIN.getCode();
    }

    public static boolean isOperator(User user) {
        if (user == null || user.getUserRoleCd() == null) {
            return false;
        }
        return user.getUserRoleCd().getCode() == UserRoleCd.OPERATOR.getCode();
    }

}
