package com.sk.domain.mi.dto.response.dashboard.charts;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

public record DashboardMetadata(

        @Schema(description = "상품데이터소스번호", example = "1") Integer goodsDataSourceNo,

        @Schema(description = "화폐", example = "$") String currency,

        @Schema(description = "현재 값 단위", example = "MMBtu") String currentUnitCd,

        @Schema(description = "마지막 업데이트 일시", example = "2021-01-01") LocalDate lastUpdateDt,

        @Schema(description = "기준 날짜", example = "2021-01-01") LocalDate baseDate

) {

}
