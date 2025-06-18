package com.sk.domain.shared.enums;

public enum ComponentType {
    GENERAL, // 일반
    PIVOT, // 피벗 형태(일반)
    CPC_NINO_SST, // 크롤링: CPC
    NA_RIG_WEEKLY, // 크롤링: NA 주간
    WW_RIG_SUMMARY, // 크롤링: WW 요약
    OECD_CLI, // 실트론 RPA: OECD CLI
    TSMC, // 실트론 RPA: TSMC
    PMI_MANUFACTURE, // 실트론 RPA: PMI 제조업
}
