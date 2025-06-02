package com.sk.domain.mi.dto.response.dashboard.charts.graph;

import java.util.List;

import com.sk.domain.shared.enums.chart.ChartPredictDataType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardChartLine(
		@Schema(description = "권한 여부", example = "true") Boolean isPermitted,
		@Schema(description = "차트 데이터 타입", example = "ACTUAL") ChartPredictDataType dataType,
		@Schema(description = "차트 데이터 소스 번호(주요 요인 분석 조회시 사용)", example = "1") Integer goodsDataSourceNo,
		@Schema(description = "차트 데이터") List<DashboardChartRawData> rawData) {

	@Builder
	public DashboardChartLine(Boolean isPermitted, ChartPredictDataType dataType,
			Integer goodsDataSourceNo, List<DashboardChartRawData> rawData) {
		this.isPermitted = isPermitted;
		this.dataType = dataType;
		this.goodsDataSourceNo = goodsDataSourceNo;
		this.rawData = rawData;
	}

	//권한이 없으면 rawData 데이터를 보여주지 않는다
	public static DashboardChartLine of(Boolean isPermitted, ChartPredictDataType dataType,
			Integer goodsDataSourceNo, List<DashboardChartRawData> rawData) {
		return DashboardChartLine.builder()
				.isPermitted(isPermitted)
				.dataType(dataType)
				.goodsDataSourceNo(goodsDataSourceNo)
				.rawData(rawData)
				.rawData(isPermitted != null && isPermitted ? rawData : null)
				.build();
	}
}
