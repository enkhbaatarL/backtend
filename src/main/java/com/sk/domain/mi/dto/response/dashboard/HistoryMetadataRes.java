package com.sk.domain.mi.dto.response.dashboard;

import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record HistoryMetadataRes(

    @Schema(description = "설정 명", example = "Henry Hub Price Forecast")
    String targetSettingNm,

    @Schema(description = "예측기간 단위구분 코드", example = "D", allowableValues = {"D", "M"})
    PeriodUnitSeparateCd forecastPeriodUnitSeparateCd,

    @ArraySchema(
        arraySchema = @Schema(description = "예측 수행일 목록"),
        schema = @Schema(description = "예측 수행일", example = "20250611")
    )
    List<String> fulfillmentDate,

    @Schema(description = "현물 & 선물 리스트")
    List<SettingDetail> settingDetails

) {

    public record SettingDetail(

        @Schema(description = "예측설정 상세번호", example = "1004")
        Integer forecastSettingDetailNo,

        @Schema(description = "예측설정 설명", example = "Actual")
        String forecastSettingDetailExpn

    ) {

    }

}
