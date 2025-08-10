package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivationStatus {

    ACTIVE("ACTIVE", "활성"),
    INACTIVE("INACTIVE", "비활성");

    private final String code;
    private final String description;

    public boolean isActive() {
        return this == ACTIVE;
    }

}
