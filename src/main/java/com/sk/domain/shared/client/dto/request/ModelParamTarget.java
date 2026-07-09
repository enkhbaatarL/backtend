package com.sk.domain.shared.client.dto.request;

import java.util.List;

public record ModelParamTarget(

    String modelFile,
    String modelSheet,
    String tableName,
    List<String> valueFieldNames,
    List<String> slotAxesFieldNames
) {
}
