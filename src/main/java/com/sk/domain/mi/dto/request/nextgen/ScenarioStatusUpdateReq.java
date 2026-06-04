package com.sk.domain.mi.dto.request.nextgen;

import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 시나리오 판정 상태 변경 요청 (대기/실현/폐기).
 */
public record ScenarioStatusUpdateReq(

    @Schema(description = "변경할 판정 상태 (pending/realized/discarded)", example = "realized")
    @NotNull(message = "status는 필수입니다")
    ScenarioStatus status

) {}
