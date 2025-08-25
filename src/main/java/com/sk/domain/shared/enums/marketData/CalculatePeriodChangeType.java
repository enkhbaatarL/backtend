package com.sk.domain.shared.enums.marketData;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CalculatePeriodChangeType { // 기간 변화 계산 타입
    DOD("DoD"),
    MOM("MoM"),
    WOW("WoW"),
    YOY("YoY"),
    FYA("5YA");

    private String periodName;
    CalculatePeriodChangeType(String periodName){
        this.periodName = periodName;
    }
}
