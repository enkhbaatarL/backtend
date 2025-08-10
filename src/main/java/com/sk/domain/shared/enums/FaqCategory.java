package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// @formatter:off
@Getter
@RequiredArgsConstructor
public enum FaqCategory {

    CATEGORY1("CATEGORY1", "사용 안내"), 
    CATEGORY2("CATEGORY2", "권한 및 접근"), 
    CATEGORY3("CATEGORY3", "기타");

  private final String code;
  private final String name;

}
// @formatter:on