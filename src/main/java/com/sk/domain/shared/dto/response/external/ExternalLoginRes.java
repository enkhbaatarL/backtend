package com.sk.domain.shared.dto.response.external;

public record ExternalLoginRes(
    String sessionId
) {
    public static ExternalLoginRes of(String sessionId) {
        return new ExternalLoginRes(sessionId);
    }
}