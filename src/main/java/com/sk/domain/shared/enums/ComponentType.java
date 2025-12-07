package com.sk.domain.shared.enums;

public enum ComponentType {
    GENERAL, // 일반
    PIVOT, // 피벗 형태(일반)
    CPC_NINO_SST, // 크롤링: CPC
    NA_RIG_WEEKLY, // 크롤링: NA 주간
    WW_RIG_SUMMARY, // 크롤링: WW 요약
    SHORT_TERM_EO, // 크롤링: Short Term Energy Outlook
    OECD_CLI, // 실트론 RPA: OECD CLI
    TSMC, // 실트론 RPA: TSMC
    PMI_MANUFACTURE, // 실트론 RPA: PMI 제조업
    GLOBAL_OIL, // 국제유가 크롤링
    GIE_EU, // GIE EU 총합
    KPLER_US_EXPORT, // Kpler US LNG 수출
    KPLER_WORLD_IMPORT, // Kpler LNG 수입
    SKENS_DATA_A, // SKENS 데이터 A
    SKENS_DATA_B, // SKENS 데이터 B

    YEAR_QUARTER_ADD; // 2025, 2025 Q1 이런식의 컬럼이 있을때, 2025-03-15 를 date 롤 맨앞에 추가.
}
