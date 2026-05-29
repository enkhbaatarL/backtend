package com.sk.domain.mi.dto.request.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record FundamentalModelRunCallbackReq(

    @NotNull(message = "status는 필수입니다")
    @Schema(description = "콜백으로 전달되는 최종 상태", implementation = FundamentalModelRunStatus.class, example = "COMPLETED")
    FundamentalModelRunStatus status,

    @NotNull(message = "finishedAt은 필수입니다")
    @Schema(description = "Python 프로세스 기준 실행 완료/실패 시각")
    LocalDateTime finishedAt,

    @Schema(description = "실패 사유", example = "EXTERNAL_ERROR")
    String failureReason,

    @Schema(description = "실패 상세 메시지", example = "Model execution failed due to invalid source data.")
    String errorMessage

) {
}
