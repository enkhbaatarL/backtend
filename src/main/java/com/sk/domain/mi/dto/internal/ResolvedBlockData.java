package com.sk.domain.mi.dto.internal;

import java.util.LinkedHashMap;
import java.util.Map;

public record ResolvedBlockData(
    Map<String, Object> content
) {

    public static ResolvedBlockData of(Map<String, Object> content) {
        return new ResolvedBlockData(content == null ? Map.of() : new LinkedHashMap<>(content));
    }

    public static ResolvedBlockData error(String message) {
        return of(Map.of("error", message));
    }

    public Map<String, Object> toContentMap() {
        return new LinkedHashMap<>(content);
    }
}
