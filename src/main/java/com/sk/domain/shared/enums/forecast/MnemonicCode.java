package com.sk.domain.shared.enums.forecast;

import lombok.Getter;

@Getter
public enum MnemonicCode {

    BRENT_CRUDE("LLCC.01", "Brent Crude"),
    DUBAI_CRUDE("D-PCAAT00", "Dubai Crude"),
    DRAM_ASP("DRAMASP", "DRAM ASP"),
    HH_PRC("HHPRC", "Henry Hub"),
    BRENT_CRUDE_ACTUAL("LCOc1", "Brent Crude"),
    HH_PRC_ACTUAL("IGBBL21", "Henry Hub"),
    HH_PRC2("HHPRC2", "Henry Hub"),
    HH_PRC_ACTUAL1("NGc1", "Henry Hub"),
    HH_PRC_ACTUAL2("NGc2", "Henry Hub"),
    HH_PRC_ACTUAL3("NGc3", "Henry Hub"),
    HH_PRC_ACTUAL4("NGc4", "Henry Hub");

    // Getter 메소드
    private final String code;
    private final String predictChartDetailExpression;

    // 생성자
    MnemonicCode(String code, String predictChartDetailExpression) {
        this.code = code;
        this.predictChartDetailExpression = predictChartDetailExpression;
    }

    /**
     * mnemonicCd를 통해 해당 Enum 상수를 찾는 유틸리티 메소드
     */
    public static MnemonicCode findByCode(String code) {
        for (MnemonicCode type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null; // 또는 예외 처리
    }

}
