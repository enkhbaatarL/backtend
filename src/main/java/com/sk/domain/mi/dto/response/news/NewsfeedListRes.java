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
    Long newsSignificanceId,

    @Schema(description = "중요 뉴스 ID", example = "11")
    Long significantNewsId,

    @Schema(description = "데이터등록일련번호", example = "36033981")
    Long dataRegistrationSerialNo,

    @Schema(description = "NEWS최초생성일시", example = "2025-07-29 23:59:09")
    LocalDateTime firstCreated,

    @Schema(description = "뉴스시장카테고리코드", example = "CROIL")
    String newsMarketCategoryCode,

    @Schema(description = "뉴스 요약 ID", example = "11")
    Long newsSummaryId,

    @Schema(description = "유사그룹상태코드", example = "HEAD")
    String similarityGroupStatusCode,

    @Schema(description = "간략요약", example = "")
    String briefSummary,

    @Schema(description = "요약", example = "")
    List<String> summary,

    @Schema(description = "뉴스 제목", example = "Approval to Amend Term of Warrants")
    String title,

    @Schema(description = "프로바이더", example = "reuters")
    String provider,

    @Schema(description = "뉴스 중요도 점수", example = "9.1")
    BigDecimal newsSignificanceScore,

    @Schema(description = "중요 뉴스 점수", example = "9.1")
    BigDecimal significantNewsScore,

    @Schema(description = "방향성", example = "Upward")
    String directionality,

    @Schema(description = "지역", example = "[Global, Asia Pacific]")
    List<String> regions,

    @Schema(description = "이벤트명 목록", example = "[Event A, Event B]")
    List<String> events,

    @Schema(description = "유사 뉴스 목록 (HEAD인 경우에만)", nullable = true)
    List<SimilarNewsItem> similarNewsList
) {

    /**
     * 유사그룹 내 개별 뉴스 항목
     */
    public record SimilarNewsItem(
        @Schema(description = "뉴스 요약 ID")
        Long newsSummaryId,

        @Schema(description = "뉴스 제목")
        String title,

        @Schema(description = "NEWS최초생성일시")
        LocalDateTime firstCreated,

        @Schema(description = "데이터등록일련번호")
        Long dataRegistrationSerialNo
    ) {}
}
