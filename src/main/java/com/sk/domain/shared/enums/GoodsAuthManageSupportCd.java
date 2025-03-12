package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GoodsAuthManageSupportCd {

    COMPANY("COMPANY", "회사"),
    INDIVIDUAL("INDIVIDUAL", "개인");

    private final String code;
    private final String description;

}
