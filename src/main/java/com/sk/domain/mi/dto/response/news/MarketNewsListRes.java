package com.sk.domain.mi.dto.response.news;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record MarketNewsListRes(
    @Schema(description = "뉴스 요약 ID", example = "11")
    Integer newsSummaryId,

    @Schema(description = "데이터등록일련번호", example = "36033981")
    Integer dataRegistrationSn,

    @Schema(description = "NEWS최초생성일시", example = "2025-07-29 23:59:09")
    LocalDateTime firstCreated,

    @Schema(description = "뉴스 제목", example = "Approval to Amend Term of Warrants")
    String title,

    @Schema(description = "프로바이더", example = "reuters")
    String provider,

    @Schema(description = "카테고리", example = "[CROIL, BTRY]")
    List<String> categories,

    @Schema(description = "지역", example = "[Global, Asia Pacific]")
    List<String> regions,

    @Schema(description = "산업", example = "[Energy, Financials]")
    List<String> industries
    ) {

}
