package com.sk.domain.mi.dto.response.charts;

import com.sk.common.utils.StringUtil;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.Interval;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

public record ChartMetadata(

    @Schema(description = "현재 값 단위", example = "MMBtu")
    String currentUnitCd,

    @Schema(description = "차트 간격", example = "ONE_YEAR")
    Interval currentInterval,

    @Schema(description = "변경할 수 있는 단위", example = "ONE_YEAR")
    Set<String> changeableUnitCd

) {

    @Builder
    public ChartMetadata(String currentUnitCd, Interval currentInterval, Set<String> changeableUnitCd) {
        this.currentUnitCd = currentUnitCd;
        this.currentInterval = currentInterval;
        this.changeableUnitCd = changeableUnitCd;
    }

    public static ChartMetadata of(GoodsDataSourceDetail goodsDataSourceDetail, String unitConversionCd, Set<String> changeable, Interval interval) {
        return ChartMetadata.builder()
            .currentUnitCd(StringUtils.defaultIfEmpty(unitConversionCd,
                StringUtil.joinPoundSignNonNull(goodsDataSourceDetail.getGoodsDataSourceCurrencyUnitNm(), goodsDataSourceDetail.getGoodsDataSourceUnitNm())))
            .currentInterval(interval)
            .changeableUnitCd(changeable)
            .build();
    }

}
