package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// @formatter:off
@Getter
@RequiredArgsConstructor
public enum FileRelationshipObjectSeparateCode {

  TEMP("TEMP", "임시저장"), 
  BBS("BBS", "게시판"), 
  GDATS("GDATS", "상품정형데이타"), 
  GUDAT("GUDAT", "상품비정형데이타");

  private final String code;
  private final String name;

}
// @formatter:on
