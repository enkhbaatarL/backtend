package com.sk.domain.mi.dto.response.news;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record MarketNewsDetailRes(
    @Schema(description = "뉴스 요약 ID", example = "11")
    Integer newsSummaryId,

    @Schema(description = "데이터등록일련번호", example = "36033981")
    Integer dataRegistrationSn,

    @Schema(description = "NEWS최초생성일시", example = "2025-07-29 23:59:09")
    LocalDateTime firstCreated,

    @Schema(description = "뉴스 제목", example = "Market Analysis: Tech Stocks Rally")
    String title,

    @Schema(description = "뉴스 본문 전체", example = "Today's market showed significant growth...")
    String story,

    @Schema(description = "프로바이더", example = "reuters")
    String provider) {

}
