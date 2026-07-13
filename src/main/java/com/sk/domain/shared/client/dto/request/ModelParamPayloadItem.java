package com.sk.domain.shared.client.dto.request;

import java.util.List;
import java.util.Map;

public record ModelParamPayloadItem(

    String parameterName,
    ModelParamTarget modelTarget,
    String rationale,
    List<Map<String, Object>> values
) {
}
