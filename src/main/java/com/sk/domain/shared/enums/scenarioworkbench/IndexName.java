package com.sk.domain.shared.enums.scenarioworkbench;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IndexName {
    JKM("JKM", "JKM (아시아 LNG 현물 가격)"),
    TTF("TTF", "TTF (유럽 가스 허브 가격)"),
    HH("HH", "Henry Hub (미국 가스 가격)");

    private final String code;
    private final String label;

    public static boolean isSupported(String dbValue) {
        return fromCode(dbValue) != null;
    }

    public static IndexName fromCode(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (IndexName index : values()) {
            if (index.code.equalsIgnoreCase(dbValue)) {
                return index;
            }
        }
        return null;
    }
}
