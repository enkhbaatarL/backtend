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

    /**
     * 비교 그리드 기준으로 만든 요청에는 이 시나리오가 제안하지 않은 파라미터가 values 빈 배열로 섞여 올 수 있어 제외한다.
     * 나머지는 단건 실행 검증(제안 구조 일치)을 그대로 거친다.
     */
    public ModelParamProposalExecutionReq toSingleExecutionRequest() {
        List<ModelParamPayloadItem> proposedParameters = confirmationParameters.stream()
            .filter(item -> item == null || (item.values() != null && !item.values().isEmpty()))
            .toList();
        return new ModelParamProposalExecutionReq(modelParamProposalId, proposedParameters);
    }
}
