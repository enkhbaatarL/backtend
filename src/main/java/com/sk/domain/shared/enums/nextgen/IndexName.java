package com.sk.domain.shared.enums.nextgen;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IndexName {
    JKM("JKM", "JKM (아시아 LNG 현물 가격)", "East Asia Spot"),
    TTF("TTF", "TTF (유럽 가스 허브 가격)", "TTF"),
    HH("HH", "Henry Hub (미국 가스 가격)", "HH");

    private final String code;
    private final String label;
    /**
     * 수급모델 가격 테이블(tmd_mdl_scnrcaseprcl)의 prc_rgn_nm 적재값.
     * 화면 표기(code)와 다를 수 있어 조회 필터용으로 분리한다. (예: JKM ↔ East Asia Spot)
     */
    private final String priceRegionName;

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
