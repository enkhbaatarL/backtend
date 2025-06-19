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
    OTHER_SERVICE_ACCESS("isOtherServiceAccessPossible", "타사서비스정보가능여부"),
    COMPANY_DOMAIN("companyDomain", "고객사도메인"),
    AUTH_METHOD("authMethod", "인증방식");

    private final String code;
    private final String description;

    @Getter
    @RequiredArgsConstructor
    public enum AuthMethodValue {

        SAML2("saml2", "SAML2 인증 방식"),
        OAUTH2_CLIENT_CREDENTIALS("client_credentials", "OAuth2 client_credentials 인증 방식");

        private final String code;
        private final String description;

    }

}
