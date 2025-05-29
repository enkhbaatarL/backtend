package com.sk.domain.mi.dto.response.dashboard.charts.graph;

import java.util.List;

import com.sk.domain.shared.enums.chart.ChartPredictDataType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardChartLine(
		@Schema(description = "권한 여부", example = "true") Boolean isPermitted,
		@Schema(description = "차트 데이터 타입", example = "ACTUAL") ChartPredictDataType dataType,
		@Schema(description = "차트 데이터") List<DashboardChartRawData> rawData) {

	@Builder
	public DashboardChartLine(Boolean isPermitted, ChartPredictDataType dataType,
			List<DashboardChartRawData> rawData) {
		this.isPermitted = isPermitted;
		this.dataType = dataType;
		this.rawData = rawData;
	}

	//권한이 없으면 rawData 데이터를 보여주지 않는다
	public static DashboardChartLine of(Boolean isPermitted, ChartPredictDataType dataType,
			List<DashboardChartRawData> rawData) {
		return DashboardChartLine.builder()
				.isPermitted(isPermitted)
				.dataType(dataType)
				.rawData(rawData)
				// .rawData(isPermitted != null && isPermitted ? rawData : null)
				.build();
	}
}
