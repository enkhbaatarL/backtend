package com.sk.domain.mi.dto.response.chart;

import com.sk.domain.shared.entity.master.tmd.GoodsDataUnitConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public record UnitConversionRes(

    @Schema(description = "단위 변환 번호", example = "15")
    Integer unitConversionUnitNo,

    @Schema(description = "단위 변환 명", example = "")
    String unitConversionUnitNm,

    @Schema(description = "단위 변환 설명", example = "")
    String unitConversionUnitDesc

) {

    @Builder
    public UnitConversionRes(Integer unitConversionUnitNo, String unitConversionUnitNm, String unitConversionUnitDesc) {
        this.unitConversionUnitNo = unitConversionUnitNo;
        this.unitConversionUnitNm = unitConversionUnitNm;
        this.unitConversionUnitDesc = unitConversionUnitDesc;
    }

    public static List<UnitConversionRes> ofList(List<GoodsDataUnitConvert> goodsDataConversions) {
        return goodsDataConversions.stream().map(UnitConversionRes::of).toList();
    }

    public static UnitConversionRes of (GoodsDataUnitConvert entity) {
        return UnitConversionRes.builder()
            .unitConversionUnitNo(entity.getId().getUnitConvNo())
            .unitConversionUnitNm(entity.getUnitConvert().getConvertAfterUnitNm())
            .unitConversionUnitDesc(entity.getUnitConvert().getUnitConvertExpn())
            .build();
    }

}
