package com.sk.domain.mi.conversion_on_demand;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FormulaService {

    WAFER_TO_CHIP_EA("WAFER_TO_CHIP_EA", "웨이퍼 -> 칩 개수 단위 변환"),
    BOLLINGER_BANDS("BOLLINGER_BANDS", "볼린저밴드"),
    MOVING_AVERAGE("MOVING_AVERAGE", "이동평균선"),
    STANDARD_DEVIATION("STANDARD_DEVIATION", "표준편차");

    private final String code;
    private final String description;

}
