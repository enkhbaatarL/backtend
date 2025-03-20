package com.sk.domain.admin.dto.response.bbs;

import lombok.Builder;

@Builder
public record NoticeFileRes(
    Integer fileId) {

  public static NoticeFileRes of(Integer fileId) {
    return NoticeFileRes.builder()
        .fileId(fileId)
        .build();
  }
}
