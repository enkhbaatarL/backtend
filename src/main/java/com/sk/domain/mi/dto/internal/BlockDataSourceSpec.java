package com.sk.domain.mi.dto.internal;

import java.util.List;
import java.util.Map;

public record BlockDataSourceSpec(
    Locator locator,
    Map<String, Object> query,
    Assign assign,
    List<TransformRule> transform,
    OverridePolicy overridePolicy
) {

    public record Locator(
        String kind,
        Long refId,
        String refCode
    ) {
    }

    public record Assign(
        List<AssignField> fields
    ) {
    }

    public record AssignField(
        String fieldKey,
        String sourcePath,
        Boolean required
    ) {
    }

    public record TransformRule(
        String type,
        String sourcePath,
        String format,
        String timezone,
        Integer maxItems,
        Boolean dropBlank
    ) {
    }

    public record OverridePolicy(
        IssueStateOverride issueState
    ) {
    }

    public record IssueStateOverride(
        List<String> query,
        List<String> state,
        List<String> contentInput
    ) {
    }
}
