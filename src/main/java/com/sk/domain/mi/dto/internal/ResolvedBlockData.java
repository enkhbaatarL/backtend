package com.sk.domain.mi.dto.internal;

import java.util.LinkedHashMap;
import java.util.Map;

public record ResolvedBlockData(
    Map<String, Object> content,
    java.util.List<Map<String, Object>> expandedContents
) {

    public static ResolvedBlockData of(Map<String, Object> content) {
        return new ResolvedBlockData(
            content == null ? Map.of() : new LinkedHashMap<>(content),
            java.util.List.of()
        );
    }

    public static ResolvedBlockData expanded(Map<String, Object> content, java.util.List<Map<String, Object>> expandedContents) {
        java.util.List<Map<String, Object>> normalizedExpanded = expandedContents == null
            ? java.util.List.of()
            : expandedContents.stream()
                .map(item -> item == null ? Map.<String, Object>of() : new LinkedHashMap<>(item))
                .toList();
        return new ResolvedBlockData(
            content == null ? Map.of() : new LinkedHashMap<>(content),
            normalizedExpanded
        );
    }

    public static ResolvedBlockData error(String message) {
        return of(Map.of("error", message));
    }

    public Map<String, Object> toContentMap() {
        return new LinkedHashMap<>(content);
    }

    public java.util.List<Map<String, Object>> toExpandedContents() {
        return expandedContents.stream()
            .<Map<String, Object>>map(LinkedHashMap::new)
            .toList();
    }
}
