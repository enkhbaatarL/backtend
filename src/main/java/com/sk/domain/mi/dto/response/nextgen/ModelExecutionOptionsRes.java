package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ModelInputMode;
import io.swagger.v3.oas.annotations.media.Schema;

public record ModelExecutionOptionsRes(

    @Schema(description = "시나리오 ID", example = "309")
    Long scenarioId,

    @Schema(description = "수급모델 실행 입력 방식", implementation = ModelInputMode.class, example = "PARAMETER_PROPOSAL")
    ModelInputMode inputMode,

    @Schema(description = "파라미터 제안 사용 가능 여부", example = "true")
    boolean parameterProposalAvailable,

    @Schema(description = "파일 업로드 사용 가능 여부", example = "false")
    boolean fileUploadAvailable
) {

    public static ModelExecutionOptionsRes of(Long scenarioId, ModelInputMode inputMode) {
        return new ModelExecutionOptionsRes(
            scenarioId,
            inputMode,
            inputMode.isParameterProposal(),
            !inputMode.isParameterProposal()
        );
    }
}
