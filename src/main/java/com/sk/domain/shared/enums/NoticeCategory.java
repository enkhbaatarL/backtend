package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NoticeCategory {

  CAT1("CAT1", "카테고리1"), //
  CAT2("CAT2", "카테고리2");

  private final String code;
  private final String name;

}
