package com.sk.domain.admin.dto.response.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
public record FileExistRes(
        @Schema(description = "파일번호", example = "1") 
        Long fileNo, 

        @Schema(description = "파일Exist", example = "")
        Boolean fileExist) {

    @Builder
    public FileExistRes(Long fileNo, Boolean fileExist) {
        this.fileNo = fileNo;
        this.fileExist = fileExist;
    }

    public static FileExistRes of(Long fileNo, Boolean fileExist) {
        return FileExistRes.builder()
                .fileNo(fileNo)
                .fileExist(fileExist)
                .build();
    }
}
// @formatter:on