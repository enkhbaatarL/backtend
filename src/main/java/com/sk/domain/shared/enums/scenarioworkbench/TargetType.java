package com.sk.domain.shared.enums.scenarioworkbench;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// note: tmm_impactresultm.target_type 컬럼 값(price|spread)에 대응
@Getter
@RequiredArgsConstructor
public enum TargetType {
    PRICE("price", "가격"),
    SPREAD("spread", "스프레드");

    private final String code;
    private final String label;

    public boolean matches(String dbValue) {
        return this.code.equalsIgnoreCase(dbValue);
    }
}
