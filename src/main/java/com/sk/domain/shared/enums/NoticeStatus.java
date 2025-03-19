package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NoticeStatus {

  ACTIVE("ACTIVE", "게시중"), //
  INACTIVE("INACTIVE", "게시안함");

  private final String code;
  private final String name;

}
