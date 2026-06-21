package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRegion;
import com.sk.domain.shared.enums.fundamentalmodel.TradeCountryHeaderKind;
import com.sk.domain.shared.enums.fundamentalmodel.TradeFlowDirection;
import com.sk.domain.shared.enums.fundamentalmodel.TradeFlowSelector;
import com.sk.domain.shared.enums.fundamentalmodel.TradeFlowUnit;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FundamentalModelTradeFlowsRes(

    @Schema(description = "case ID", example = "106")
    Long caseId,

    @Schema(description = "조회한 Trade 유형", implementation = TradeFlowSelector.class, example = "LNG")
    TradeFlowSelector selector,

    @Schema(description = "조회 연도", example = "2026")
    Integer year,

    @Schema(description = "수치 단위", implementation = TradeFlowUnit.class, example = "MTPA")
    TradeFlowUnit unit,

    @Schema(description = "실제 적용된 필터", implementation = TradeFlowsAppliedFilters.class)
    TradeFlowsAppliedFilters appliedFilters,

    @ArraySchema(schema = @Schema(implementation = TradeCountryHeader.class,
        description = "From country 축 헤더 목록 (컬럼). region+country=null은 지역 집계"))
    List<TradeCountryHeader> fromCountries,

    @ArraySchema(schema = @Schema(implementation = TradeCountryHeader.class,
        description = "To country 축 헤더 목록 (행). 마지막 항목은 region=null, country=Total"))
    List<TradeCountryHeader> toCountries,

    @ArraySchema(schema = @Schema(implementation = TradeFlowItem.class,
        description = "0이 아닌 trade flow 값 목록 (sparse). 없는 셀은 0으로 처리"))
    List<TradeFlowItem> flows

) {

    public record TradeFlowsAppliedFilters(

        @ArraySchema(schema = @Schema(implementation = TradeFlowDirection.class, example = "TO_COUNTRY"))
        List<TradeFlowDirection> types,

        @ArraySchema(schema = @Schema(implementation = FundamentalModelRegion.class, example = "ASIA"))
        List<FundamentalModelRegion> regions

    ) {
    }

    public record TradeCountryHeader(

        @Schema(description = "행/열 식별자. FE lookup key로 사용", example = "COUNTRY:AFRICA:Angola")
        String key,

        @Schema(description = "화면 표시명", example = "Angola")
        String label,

        @Schema(description = "헤더 유형", implementation = TradeCountryHeaderKind.class, example = "COUNTRY")
        TradeCountryHeaderKind kind,

        @Schema(description = "지역. null이면 Total 행을 나타낸다", implementation = FundamentalModelRegion.class, example = "AFRICA")
        FundamentalModelRegion region,

        @Schema(description = "국가명. null이면 해당 지역의 집계 행/열", example = "Angola")
        String country

    ) {
    }

    public record TradeFlowItem(

        @Schema(description = "From country 헤더 식별자", example = "COUNTRY:AFRICA:Angola")
        String fromKey,

        @Schema(description = "To country 헤더 식별자", example = "COUNTRY:ASIA:Bangladesh")
        String toKey,

        @Schema(description = "수출 지역", implementation = FundamentalModelRegion.class, example = "AFRICA")
        FundamentalModelRegion fromRegion,

        @Schema(description = "수출 국가. null이면 지역 집계", example = "Angola")
        String fromCountry,

        @Schema(description = "수입 지역. null이면 Total 행", implementation = FundamentalModelRegion.class, example = "ASIA")
        FundamentalModelRegion toRegion,

        @Schema(description = "수입 국가. null이면 지역 집계. Total이면 합계 행", example = "Bangladesh")
        String toCountry,

        @Schema(description = "물량 (MTPA 기준). 소수점 다섯째 자리까지 절삭하며, 여섯째 자리에서 반올림하지 않는다.", example = "0.00177")
        Double value

    ) {
    }
}
