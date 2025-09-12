package com.sk.domain.mi.dto.response.newsletter;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record NewsletterListRes(
        @Schema(description = "뉴스레터 고유 ID", example = "NL20250912001")
        String newsletterId,

        @Schema(description = "발행일시", example = "2025-09-11")
        LocalDateTime date,

        @Schema(description = "발행번호", example = "102")
        String issueNo,

        @Schema(description = "뉴스레터 이름", example = "Oil Price Trend")
        String name,

        @Schema(description = "뉴스레터 제목", example = "Tech Stock Rally Continues - Weekly Market Analysis")
        String title,

        @Schema(description = "발행상태", example = "PUBLISHED", allowableValues = {"DRAFT", "PUBLISHED", "ARCHIVED"})
        String status) {
}