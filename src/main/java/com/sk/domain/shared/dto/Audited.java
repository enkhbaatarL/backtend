package com.sk.domain.shared.dto;

public record Audited<T>(

    T target,
    AuditInfo auditInfo

) {
}
