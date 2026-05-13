package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public enum PermissionCondition {

    NONE, // 조건 없음

    THIRD_PARTY_ADMIN_ONLY, // [ADMIN] 타사 접속 가능 관리자

    SKGAS_COMPANY_USER_ONLY; // [USER] sk gas 소속

    @Getter
    @RequiredArgsConstructor
    public enum AllowedCompany {

        SKGAS("SKGAS");
        private final String code;

    }

}
