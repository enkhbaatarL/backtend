package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CompanyType {
    
    CUSTOMER("CUSTOMER", "고객사"),
    MANAGER("MANAGER", "관리사");

    private final String code;
    private final String name;
}
