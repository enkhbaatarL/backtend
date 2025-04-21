package com.sk.domain.mi.dto.response.chart;

import com.sk.domain.shared.entity.master.tmd.GoodsDataUnitConvert;
import com.sk.domain.shared.enums.Interval;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public record ChartMetadata(

    @Schema(description = "기본 값 단위", example = "MMBtu")
    String defaultUnit,

    @Schema(description = "현재 값 단위", example = "MMBtu")
    String currentUnit,

    @Schema(description = "차트 간격", example = "ONE_YEAR")
    Interval currentInterval,

    @Schema(description = "변경할 수 있는 단위", example = "ONE_YEAR")
    List<String> changeableUnit

) {

    @Builder
    public ChartMetadata(String defaultUnit, String currentUnit, Interval currentInterval, List<String> changeableUnit) {
        this.defaultUnit = defaultUnit;
        this.currentUnit = currentUnit;
        this.currentInterval = currentInterval;
        this.changeableUnit = changeableUnit;
    }

    public static ChartMetadata of(String defaultUnit, String unitConversionCd, Interval interval, List<GoodsDataUnitConvert> goodsDataConversions) {

        List<String> changeable = goodsDataConversions.stream().map(s -> s.getUnitConvert().getConvertAfterUnitNm()).collect(Collectors.toList());
        changeable.add(defaultUnit);

        return ChartMetadata.builder()
            .defaultUnit(defaultUnit)
            .currentUnit(StringUtils.defaultIfEmpty(unitConversionCd, defaultUnit))
            .currentInterval(interval)
            .changeableUnit(changeable)
            .build();
    }

}
