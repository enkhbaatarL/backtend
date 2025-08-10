package com.sk.domain.admin.dto.response.bbs;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record FaqFilterRes(
        @Schema(description = "카테고리", example = "")
        List<BulletinBoardFilterDto> category) {

    public static FaqFilterRes of(List<BulletinBoardFilterDto> category) {
        return new FaqFilterRes(category);
    }
}
// @formatter:on