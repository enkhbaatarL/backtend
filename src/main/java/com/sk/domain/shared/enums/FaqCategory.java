package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// @formatter:off
@Getter
@RequiredArgsConstructor
public enum FaqCategory {

  CAT1("CATEGORY1", "카테고리1"), 
  CAT2("CATEGORY2", "카테고리2"), 
  CAT3("CATEGORY3", "카테고리3");

  private final String code;
  private final String name;

}
// @formatter:on