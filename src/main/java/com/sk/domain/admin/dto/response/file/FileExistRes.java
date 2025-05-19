package com.sk.domain.admin.dto.response.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
@Builder
public record FileExistRes(
        @Schema(description = "파일Exist", example = "")
        Boolean fileExist) {

    public static FileExistRes of(Boolean fileExist) {
        return FileExistRes.builder()
                .fileExist(fileExist)
                .build();
    }
}
// @formatter:on