package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BulletinBoardType {

    NOTICE("NOTICE", "공지사항"), //
    FAQ("FAQ", "FAQ");

    private final String code;
    private final String name;

}
