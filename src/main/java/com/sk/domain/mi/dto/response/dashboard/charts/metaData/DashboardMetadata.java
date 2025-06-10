package com.sk.domain.mi.dto.response.dashboard.charts.metaData;

import java.time.LocalDate;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.dashboard.DashboardChartDataTableType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardMetadata(
		@Schema(description = "시장지수분석대상코드", example = "EN_LNG") String marketIndexAnalysisTargetCd,

		@Schema(description = "마지막 업데이트 일시", example = "2021-01-01") LocalDate lastUpdateDt, // 예측 데이터 마지막 업데이트 일시

		@Schema(description = "화폐", example = "$") String currency,

		@Schema(description = "현재 값 단위", example = "MMBtu") String currentUnitCd,

		@Schema(description = "기준 날짜(YYYY-MM)", example = "2021-01") String baseDate,

		@Schema(description = "데이터 테이블 타입", example = "FORECAST_SINGLE_LAYER") DashboardChartDataTableType dataTableType) {
	@Builder
	public DashboardMetadata(String marketIndexAnalysisTargetCd, LocalDate lastUpdateDt, String currency,
			String currentUnitCd, String baseDate, DashboardChartDataTableType dataTableType) {
		this.marketIndexAnalysisTargetCd = marketIndexAnalysisTargetCd;
		this.lastUpdateDt = lastUpdateDt;
		this.currency = currency;
		this.currentUnitCd = currentUnitCd;
		this.baseDate = baseDate;
		this.dataTableType = dataTableType;
	}

	public static DashboardMetadata of(String marketIndexAnalysisTargetCd, LocalDate lastUpdateDt, GoodsDataSourceDetail goodsDataSourceDetail, String baseDate, DashboardChartDataTableType dataTableType) {
		return DashboardMetadata.builder()
				.marketIndexAnalysisTargetCd(marketIndexAnalysisTargetCd)
				.lastUpdateDt(lastUpdateDt)
				.currency(goodsDataSourceDetail.getGoodsDataSourceCurrencyUnitNm())
				.currentUnitCd(goodsDataSourceDetail.getGoodsDataSourceUnitNm())
				.baseDate(baseDate)
				.dataTableType(dataTableType)
				.build();
	}
}
