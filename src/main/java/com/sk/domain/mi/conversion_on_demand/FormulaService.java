package com.sk.domain.mi.conversion_on_demand;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FormulaService {

    WAFER_TO_CHIP_EA("WAFER_TO_CHIP_EA", "웨이퍼 -> 칩 개수 단위 변환");

    private final String code;
    private final String description;

}
