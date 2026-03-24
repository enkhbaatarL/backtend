package com.sk.domain.mi.dto.internal;

import java.util.List;

public record BlockExecutionPlan(
    BlockExecutionClassification classification,
    BlockDataSourceSpec spec,
    List<String> validationErrors
) {

    public boolean isCanonical() {
        return classification == BlockExecutionClassification.CANONICAL;
    }
}
