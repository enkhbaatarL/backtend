package com.sk.domain.mi.dto.response.news;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 뉴스타임라인 목록 응답 DTO
 */
public record NewsTimelineRes(
    @Schema(description = "뉴스 이벤트 ID", example = "11")
    Long newsEventId,

    @Schema(description = "뉴스이벤트 한글명", example = "원유 가격 상승")
    String newsEventNameKr,

    @Schema(description = "뉴스이벤트 영문명", example = "Crude Oil Price Increase")
    String newsEventNameEn,

    @Schema(description = "뉴스시장카테고리코드", example = "CROIL")
    String newsMarketCategoryCode,

    @Schema(description = "뉴스 요약 ID", example = "11")
    Long newsSummaryId,

    @Schema(description = "데이터등록일련번호", example = "36033981")
    Long dataRegistrationSerialNo,

    @Schema(description = "NEWS최초생성일시", example = "2025-07-29 23:59:09")
    LocalDateTime firstCreated,

    @Schema(description = "간략요약", example = "")
    String briefSummary,

    @Schema(description = "요약", example = "")
    List<String> summary,

    @Schema(description = "뉴스 제목", example = "Approval to Amend Term of Warrants")
    String title,

    @Schema(description = "프로바이더", example = "reuters")
    String provider,

    @Schema(description = "이벤트 점수", example = "9.1")
    BigDecimal score,

    @Schema(description = "방향성", example = "Upward")
    String directionality
) {

}
