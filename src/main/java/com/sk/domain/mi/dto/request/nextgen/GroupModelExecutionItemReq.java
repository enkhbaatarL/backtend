package com.sk.domain.mi.dto.request.nextgen;

import com.sk.domain.shared.client.dto.request.ModelParamPayloadItem;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 그룹 일괄 실행의 시나리오 한 건. 단건 JSON 실행 요청과 같은 계약에 scenarioId만 추가된다.
 */
public record GroupModelExecutionItemReq(

    @Schema(description = "시나리오 ID", example = "309")
    @NotNull
    Long scenarioId,

    @Schema(description = "Agent 모델 파라미터 제안 ID", example = "10001")
    @NotNull
    Long modelParamProposalId,

    @ArraySchema(schema = @Schema(implementation = ModelParamPayloadItem.class))
    @NotEmpty
    List<@Valid ModelParamPayloadItem> confirmationParameters

) {

    public ModelParamProposalExecutionReq toSingleExecutionRequest() {
        return new ModelParamProposalExecutionReq(modelParamProposalId, confirmationParameters);
    }
}
