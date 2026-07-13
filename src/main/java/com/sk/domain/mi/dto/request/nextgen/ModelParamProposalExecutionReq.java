package com.sk.domain.mi.dto.request.nextgen;

import com.sk.domain.shared.client.dto.request.ModelParamPayloadItem;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ModelParamProposalExecutionReq(

    @Schema(description = "Agent 모델 파라미터 제안 ID", example = "10001")
    @NotNull
    Long modelParamProposalId,

    @ArraySchema(schema = @Schema(implementation = ModelParamPayloadItem.class))
    @NotEmpty
    List<@Valid ModelParamPayloadItem> confirmationParameters

) {
}
