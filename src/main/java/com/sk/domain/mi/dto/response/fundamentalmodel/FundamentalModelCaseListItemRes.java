package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record FundamentalModelCaseListItemRes(

    @Schema(description = "case ID", example = "105")
    Long caseId,

    @Schema(description = "case 제목", example = "JKM price upside case")
    String title,

    @Schema(description = "case 최신 상태", implementation = FundamentalModelCaseStatus.class, example = "RUNNING")
    FundamentalModelCaseStatus status,

    @Schema(description = "case 업로더 이름. mock에서는 고정값입니다.", example = "홍길동")
    String assigneeName,

    @Schema(description = "case 생성 시각")
    LocalDateTime createdAt,

    @Schema(description = "case 최종 변경 시각")
    LocalDateTime updatedAt

) {
}
