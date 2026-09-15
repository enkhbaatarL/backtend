package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record FundamentalModelCaseDetailRes(

    @Schema(description = "case ID", example = "101")
    Long caseId,

    @Schema(description = "case 제목", example = "JKM price upside case")
    String title,

    @Schema(description = "case 설명", example = "May demand shock scenario case")
    String description,

    @Schema(description = "case 최신 상태", implementation = FundamentalModelCaseStatus.class, example = "RUNNING")
    FundamentalModelCaseStatus status,

    @Schema(
        description = "실행 유형. MI_BASE이면 결과 조회만 허용되며 수정·삭제·재실행은 허용되지 않음",
        implementation = FundamentalModelExecType.class,
        example = "MI_BASE",
        allowableValues = {"INDEPENDENT", "MI_BASE"},
        nullable = false,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    FundamentalModelExecType execType,

    @Schema(description = "case 업로더 이름", example = "홍길동")
    String assigneeName,

    @Schema(description = "case 업로더 사용자 번호", example = "1")
    Integer assigneeUserNo,

    @Schema(description = "case 관련 input/output/intermediate 파일명 목록", implementation = FundamentalModelCaseFilesRes.class)
    FundamentalModelCaseFilesRes files,

    @Schema(description = "최신 실행 정보. PENDING이면 null입니다.", implementation = FundamentalModelRunStatusRes.class)
    FundamentalModelRunStatusRes latestRun,

    @Schema(description = "case 생성 시각")
    LocalDateTime createdAt,

    @Schema(description = "case 최종 변경 시각")
    LocalDateTime updatedAt

) {
}
