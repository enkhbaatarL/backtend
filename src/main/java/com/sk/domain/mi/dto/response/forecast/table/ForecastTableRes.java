package com.sk.domain.mi.dto.response.forecast.table;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Schema(description = "Forecast 테이블 응답")
public record ForecastTableRes(
        @Schema(description = "조회 시작일자") String startDate,
        @Schema(description = "조회 종료일자") String endDate,
        @Schema(description = "데이터 소스 번호") Integer goodsDataSourceNo,
        @Schema(description = "정렬 순서") String order,
        @Schema(description = "테이블 헤더 리스트") List<String> tableHeaderList,
        @Schema(description = "테이블 데이터 리스트") List<ForecastTableRow> tableDataList) {

    @Builder
    public ForecastTableRes(String startDate, String endDate, Integer goodsDataSourceNo, String order,
            List<String> tableHeaderList, List<ForecastTableRow> tableDataList) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.order = order;
        this.tableHeaderList = tableHeaderList;
        this.tableDataList = tableDataList;
    }

    @Builder
    public record ForecastTableRow(
            @Schema(description = "예측결과기간") String periodLabel,
            @Schema(description = "기간 데이터 값 리스트") List<Double> values) {
    }
}
