package com.sk.domain.mi.dto.response.news;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 뉴스피드 목록 응답 DTO
 */
public record NewsfeedListRes(
    @Schema(description = "뉴스 중요도 ID", example = "11")
    Long newsSignalId,

    @Schema(description = "데이터등록일련번호", example = "36033981")
    Long dataRegistrationSerialNo,

    @Schema(description = "NEWS최초생성일시", example = "2025-07-29 23:59:09")
    LocalDateTime firstCreated,

    @Schema(description = "뉴스시장카테고리코드", example = "CROIL")
    String newsMarketCategoryCode,

    @Schema(description = "뉴스 요약 ID", example = "11")
    Integer newsSummaryId,

    @Schema(description = "간략요약", example = "")
    String briefSummary,

    @Schema(description = "요약", example = "")
    List<String> summary,

    @Schema(description = "뉴스 제목", example = "Approval to Amend Term of Warrants")
    String title,

    @Schema(description = "프로바이더", example = "reuters")
    String provider,

    @Schema(description = "영향도점수", example = "9.1")
    BigDecimal impactScore,

    @Schema(description = "방향성", example = "Upward")
    String directionality,

    @Schema(description = "방향점수", example = "-2.1")
    BigDecimal directionalScore,

    @Schema(description = "지역", example = "[Global, Asia Pacific]")
    List<String> regions
) {

}
