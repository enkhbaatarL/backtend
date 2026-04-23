package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record EventRelatedRecordRes(

    @Schema(description = "게시일시")
    LocalDateTime publishedAt,

    @Schema(description = "제목")
    String title,

    @Schema(description = "요약")
    String summary,

    @Schema(description = "출처", example = "Reuters")
    String source

) {
}
