package com.sk.domain.admin.dto.response.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
public record FileExistRes(
        @Schema(description = "파일Exist", example = "")
        Boolean fileExist) {

    @Builder
    public FileExistRes(Boolean fileExist) {
        this.fileExist = fileExist;
    }

    public static FileExistRes of(Boolean fileExist) {
        return FileExistRes.builder()
                .fileExist(fileExist)
                .build();
    }
}
// @formatter:on