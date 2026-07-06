package com.sk.domain.mi.dto.response.news;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record MarketNewsDetailRes(
    @Schema(description = "뉴스 요약 ID", example = "11")
    Long newsSummaryId,

    @Schema(description = "데이터등록일련번호", example = "36033981")
    Integer dataRegistrationSn,

    @Schema(description = "NEWS최초생성일시", example = "2025-07-29 23:59:09")
    LocalDateTime firstCreated,

    @Schema(description = "뉴스 제목", example = "Market Analysis: Tech Stocks Rally")
    String title,

    @Schema(description = "뉴스 본문 제목", example = "본문")
    String storyTitle,

    @Schema(description = "뉴스 본문 전체", example = "Today's market showed significant growth...")
    String story,

    @Schema(description = "뉴스 요약 제목", example = "요약")
    String summaryTitle,

    @Schema(description = "뉴스 요약", example = "테크주 강세로 시장이 상승했다...")
    String summary,

    @Schema(description = "프로바이더", example = "reuters")
    String provider,

    @Schema(description = "태그 목록")
    List<String> tagList) {

}
