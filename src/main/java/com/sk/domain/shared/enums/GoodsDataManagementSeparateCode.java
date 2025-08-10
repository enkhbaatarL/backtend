package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// @formatter:off
@Getter
@RequiredArgsConstructor
public enum GoodsDataManagementSeparateCode {

  GDAT("GDAT", "상품데이타"), 
  GDATS("GDATS", "상품데이타소스"), 
  GUDAT("GUDAT", "상품비정형데이타");

  private final String code;
  private final String name;

}
// @formatter:on
