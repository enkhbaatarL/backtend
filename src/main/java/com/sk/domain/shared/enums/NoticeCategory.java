package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// @formatter:off
@Getter
@RequiredArgsConstructor
public enum NoticeCategory {

    CATEGORY1("CATEGORY1", "카테고리1"), 
    CATEGORY2("CATEGORY2", "카테고리2"),
    CATEGORY3("CATEGORY3", "카테고리3");

  private final String code;
  private final String name;

}
// @formatter:on