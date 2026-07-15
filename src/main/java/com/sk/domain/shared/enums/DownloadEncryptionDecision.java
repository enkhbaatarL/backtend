package com.sk.domain.shared.enums;

public enum DownloadEncryptionDecision {

    ENCRYPT(null),
    FEATURE_DISABLED("feature_disabled"),
    EXTENSION_NOT_TARGET("extension_not_target"),
    ROLE_NOT_TARGET("role_not_target"),
    COMPANY_NOT_TARGET("company_not_target");

    private final String skipReason;

    DownloadEncryptionDecision(String skipReason) {
        this.skipReason = skipReason;
    }

    public boolean shouldEncrypt() {
        return this == ENCRYPT;
    }

    public String skipReason() {
        return skipReason;
    }
}
