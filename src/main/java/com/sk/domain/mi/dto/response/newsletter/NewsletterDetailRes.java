package com.sk.domain.mi.dto.response.newsletter;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record NewsletterDetailRes(
        @Schema(description = "뉴스레터 ID", example = "NL001")
        String newsletterId,

        @Schema(description = "발행 주기", example = "DAILY")
        String frequency,

        @Schema(description = "뉴스레터 이름", example = "Oil Price Trends")
        String name,

        @Schema(description = "뉴스레터 설명", example = "유가와 관련한 산업 동향에 대해 분석하고 주요 뉴스의 Flow 및 Brent Crude 추이를 분석")
        String description,

        @Schema(description = "구독 여부", example = "true")
        Boolean subscribed,

        @Schema(description = "발행 번호", example = "102")
        String issueNo,

        @Schema(description = "이미지 URL", example = "https://example.com/images/newsletter1.jpg")
        String image,

        @Schema(description = "활성화 여부", example = "true")
        Boolean enabled,

        @Schema(description = "발행목록")
        List<IssuedItem> issuedList) {

    public record IssuedItem(
            @Schema(description = "발행일자", example = "2025-01-15")
            LocalDate date,

            @Schema(description = "발행번호", example = "102")
            String no) {
    }
}
