package com.sk.domain.mi.conversion;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.conversion.strategy.ConversionStrategy;
import com.sk.domain.mi.conversion.strategy.basic.MovingAverageConversion;
import com.sk.domain.mi.conversion.strategy.basic.StandardDeviationConversion;
import com.sk.domain.mi.conversion.strategy.complex.BollingerBandsConversion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum FormulaService {

    //    WAFER_TO_CHIP_EA("WAFER_TO_CHIP_EA", "웨이퍼 -> 칩 개수 단위 변환"),
    BOLLINGER_BANDS("BOLLINGER_BANDS", BollingerBandsConversion.class, "볼린저밴드"),
    MOVING_AVERAGE("MOVING_AVERAGE", MovingAverageConversion.class, "이동평균선"),
    STANDARD_DEVIATION("STANDARD_DEVIATION", StandardDeviationConversion.class, "표준편차");

    private final String code;
    private final Class<? extends ConversionStrategy> strategyClass;
    private final String description;

    public static FormulaService findByFormulaServiceNm(String formulaService) {
        return Arrays.stream(values()).filter(s -> Objects.equals(s.code, formulaService))
            .findFirst()
            .orElseThrow(() -> SystemErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "FormulaService does not matched. formulaService: %s".formatted(formulaService)));
    }

}
