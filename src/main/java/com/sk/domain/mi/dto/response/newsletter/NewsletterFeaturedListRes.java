package com.sk.domain.mi.dto.response.newsletter;

import io.swagger.v3.oas.annotations.media.Schema;

public record NewsletterFeaturedListRes(
        @Schema(description = "뉴스레터 고유 ID", example = "NL001")
        String newsletterId,

        @Schema(description = "발행주기", example = "WEEKLY", allowableValues = {"DAILY", "WEEKLY", "ON-DEMAND"})
        String frequency,

        @Schema(description = "뉴스레터 이름", example = "Oil Price Trends")
        String name,

        @Schema(description = "뉴스레터 설명", example = "유가와 관련한 산업 동향에 대해 분석하고 주요 뉴스의 Flow 및 Brent Crude 추이를 분석")
        String description,

        @Schema(description = "구독여부", example = "true")
        Boolean subscribed,

        @Schema(description = "가장 최근 발행번호", example = "102")
        String issueNo,

        @Schema(description = "뉴스레터 이미지 URL", example = "https://example.com/images/newsletter1.jpg")
        String image,

        @Schema(description = "활성화여부", example = "true")
        Boolean enabled) {
}