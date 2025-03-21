package com.sk.domain.admin.dto.response.bbs;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record NoticeFilterRes(
        @Schema(description = "카테고리", example = "")
        List<BulletinBoardFilterDto> category,

        @Schema(description = "게시여부", example = "")
        List<BulletinBoardFilterDto> status,

        @Schema(description = "게시대상", example = "")
        List<BulletinBoardFilterDto> target) {

    public static NoticeFilterRes of(List<BulletinBoardFilterDto> category, List<BulletinBoardFilterDto> status,
            List<BulletinBoardFilterDto> target) {
        return new NoticeFilterRes(category, status, target);
    }
}
// @formatter:on