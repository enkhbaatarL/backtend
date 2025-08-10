package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GoodsAuthCd {

    VIEW("VIEW", "보기권한"),
    DOWNLOAD("DOWNLOAD", "다운로드권한");

    private final String code;
    private final String description;

}
