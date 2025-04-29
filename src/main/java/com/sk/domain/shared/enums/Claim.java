package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Claim {

    USER_NO("userNo", "사용자번호"),
    COMPANY_NO("companyNo", "고객사번호"),
    COMPANY_CD("companyCd", "고객사명"),
    ROLE("role", "역할"),
    OTHER_SERVICE_ACCESS("isOtherServiceAccessPossible", "타사서비스정보가능여부");

    private final String code;
    private final String description;

}
