package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record FundamentalModelCaseListItemRes(

    @Schema(description = "case ID", example = "105")
    @NotNull
    Long caseId,

    @Schema(description = "case 제목", example = "JKM price upside case")
    @NotNull
    String title,

    @Schema(description = "case 최신 상태", implementation = FundamentalModelCaseStatus.class, example = "RUNNING")
    @NotNull
    FundamentalModelCaseStatus status,

    @Schema(
        description = "실행 유형. miBase 필드는 MI_BASE, cases 항목은 INDEPENDENT",
        implementation = FundamentalModelExecType.class,
        example = "INDEPENDENT",
        allowableValues = {"INDEPENDENT", "MI_BASE"},
        nullable = false,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    FundamentalModelExecType execType,

    @Schema(description = "case 업로더 이름", example = "홍길동")
    @NotNull
    String assigneeName,

    @Schema(description = "case 생성 시각")
    @NotNull
    LocalDateTime createdAt,

    @Schema(description = "case 최종 변경 시각")
    @NotNull
    LocalDateTime updatedAt,

    @Schema(
        description = "현재 회사·로그인 사용자가 이 Case를 원본으로 편집 중인 미삭제 DRAFT ID. 없으면 null이며 FINALIZED/DISCARDED Draft는 제외합니다.",
        format = "uuid",
        example = "aefb60dc-18b8-4ac2-beb4-a4d499950765",
        nullable = true,
        types = {"string", "null"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonInclude(JsonInclude.Include.ALWAYS)
    String draftId

) {
}
