package com.sk.domain.shared.client.dto.request;

import java.util.List;

public record AgentModelParamConfirmationReq(

    Data data
) {

    public static AgentModelParamConfirmationReq of(List<ModelParamPayloadItem> confirmationParameters) {
        return new AgentModelParamConfirmationReq(new Data(confirmationParameters));
    }

    public record Data(
        List<ModelParamPayloadItem> confirmationParameters
    ) {
    }
}
