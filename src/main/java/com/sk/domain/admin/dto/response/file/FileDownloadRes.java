package com.sk.domain.admin.dto.response.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record FileDownloadRes(
    @Schema(description = "파일번호", example = "1") //
    Long fileNo,

    @Schema(description = "파일명", example = "sample.jpg") //
    String fileNm,

    @Schema(description = "파일크기(Byte)", example = "1024") //
    Long fileSize,
    
    @Schema(description = "파일데이타", example = "") //
    byte[] fileData
    ) {

  public static FileDownloadRes of(Long fileNo, String fileNm, Long fileSize, byte[] fileData) {
    return FileDownloadRes.builder()
        .fileNo(fileNo)
        .fileNm(fileNm)
        .fileSize(fileSize)
        .fileData(fileData)
        .build();
  }
}
