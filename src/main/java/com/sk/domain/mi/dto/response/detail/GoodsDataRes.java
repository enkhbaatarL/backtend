package com.sk.domain.mi.dto.response.detail;

import com.sk.domain.shared.dto.AuditInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GoodsDataRes(

    @Schema(description = "상품 데이터 명", example = "Eye on the tiger")
    String goodsDataNm,

    @Schema(description = "상품 데이터 설명", example = "데이터 설명")
    String goodsDataExpn,

    @Schema(description = "상품 데이터 요약", example = "데이터 요약")
    String goodsDataSummaryCntt,

    @Schema(description = "프로바이더 명", example = "Wood Mackenzie")
    String providerNm,

    @Schema(description = "데이터 정보 목록")
    List<DataInfo> dataInfo,

    @Schema(description = "보기 권한", example = "true")
    Boolean isViewable,

    @Schema(description = "다운로드 권한", example = "true")
    Boolean isDownloadable,

    AuditInfo auditInfo

) {

    public record DataInfo(

        @Schema(description = "데이터 정보 타입", example = "A_TYPES", allowableValues = {"A_TYPES", "A_LANGUAGES"})
        String type,

        @Schema(description = "데이터 정보 값", example = "Insight")
        String value

    ) {

    }

}
