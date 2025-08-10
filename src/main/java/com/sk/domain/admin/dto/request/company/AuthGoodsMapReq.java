package com.sk.domain.admin.dto.request.company;

import com.sk.domain.shared.enums.GoodsAuthManagementSeparateCd;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AuthGoodsMapReq(

    @NotNull
    @Schema(description = "사용여부", example = "true")
    Boolean isUsed,

    @NotNull
    @Schema(description = "상품권한주체구분코드", example = "INDIVIDUAL", allowableValues = {"INDIVIDUAL", "COMPANY"})
    GoodsAuthManagementSeparateCd goodsAuthManagementSeparateCd,

    @Schema(description = "회사 상품권한 설정")
    CompanySetting companySetting,

    @Schema(description = "개인 상품권한 설정")
    List<IndividualSetting> IndividualSettings

) {

    public sealed interface AuthSetting permits CompanySetting, IndividualSetting {
        boolean isViewable();

        boolean isDownloadable();

        String usageBeginDate();

        String usageEndDate();
    }

    public record CompanySetting(
        @Schema(description = "보기권한 여부", example = "true")
        boolean isViewable,

        @Schema(description = "다운로드권한 여부", example = "false")
        boolean isDownloadable,

        @NotNull
        @Schema(description = "사용시작 일자", example = "20250101")
        String usageBeginDate,

        @NotNull
        @Schema(description = "사용종료 일자", example = "20251231")
        String usageEndDate) implements AuthSetting {
    }

    public record IndividualSetting(
        @Schema(description = "사용자 번호", example = "159")
        Integer userNo,

        @Schema(description = "보기권한 여부", example = "true")
        boolean isViewable,

        @Schema(description = "다운로드권한 여부", example = "false")
        boolean isDownloadable,

        @NotNull
        @Schema(description = "사용시작 일자", example = "20250101")
        String usageBeginDate,

        @NotNull
        @Schema(description = "사용종료 일자", example = "20251231")
        String usageEndDate) implements AuthSetting {
    }

}
