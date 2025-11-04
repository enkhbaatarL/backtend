package com.sk.domain.mi.dto.response.news;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MarketRecapRes(
    @Schema(description = "시장 리캡 ID", example = "1")
    Long marketRecapId,

    @Schema(description = "기준 일자", example = "20250729")
    String criteriaDate,

    @Schema(description = "뉴스 시장 카테고리 코드", example = "STOCK")
    String categoryCode,

    @Schema(description = "센티먼츠")
    Sentiments sentiments,

    @Schema(description = "하이라이트")
    Highlights highlights,

    @Schema(description = "체크포인트")
    Checkpoints checkpoints
) {

    public record Sentiments(
        @Schema(description = "현재 신호 뉴스 수")
        SignalNewsCount current,

        @Schema(description = "이전 신호 뉴스 수")
        SignalNewsCount previous,

        @Schema(description = "최근 7일 신호 뉴스 수")
        SignalNewsCount last7days,

        @Schema(description = "최근 30일 신호 뉴스 수")
        SignalNewsCount last30days
    ) {

    }

    public record SignalNewsCount(
        @Schema(description = "상승 신호 뉴스 수", example = "15")
        Integer upwardCount,

        @Schema(description = "중립 신호 뉴스 수", example = "8")
        Integer neutralCount,

        @Schema(description = "하락 신호 뉴스 수", example = "3")
        Integer downwardCount,

        @Schema(description = "기간", example = "Sep 12 07:00 - Sep 13 07:00")
        String period
    ) {}

    public record HighlightItemRes(
        @Schema(description = "하이라이트 아이템 ID", example = "1")
        Long highlightItemId,

        @Schema(description = "제목", example = "Apple Stock Surges")
        String title,

        @Schema(description = "내용", example = "Apple shares rose 5% today...")
        List<String> content,

        @Schema(description = "동향", example = "UP")
        String momentum,

        @Schema(description = "관련 뉴스 목록")
        List<RelatedNewsRes> relatedNews
    ) {}

    public record CheckpointItemRes(
        @Schema(description = "체크포인트 아이템 ID", example = "1")
        Long checkpointItemId,

        @Schema(description = "제목", example = "Federal Reserve Meeting")
        String title,

        @Schema(description = "내용", example = "Fed to announce interest rate decision...")
        List<String> content
    ) {}

    public record Narrative(
        @Schema(description = "제목", example = "Tech Sector Leads Market Rally")
        String title,

        @Schema(description = "내용", example = "Technology stocks showed strong performance...")
        List<String> content
    ) {}

    public record Highlights(
        @Schema(description = "하이라이트 narrative")
        Narrative narrative,

        @Schema(description = "하이라이트 아이템 목록")
        List<HighlightItemRes> items,

        @Schema(description = "관련 뉴스 목록")
        List<RelatedNewsRes> relatedNews
    ) {

    }

    public record Checkpoints(
        @Schema(description = "체크포인트 narrative")
        Narrative narrative,

        @Schema(description = "체크포인트 아이템 목록")
        List<CheckpointItemRes> items
    ) {

    }

    public record RelatedNewsRes(
        @Schema(description = "뉴스 요약 ID", example = "123")
        Long newsSummaryId,

        @Schema(description = "뉴스 제목", example = "Apple Reports Strong Earnings")
        String title,

        @Schema(description = "프로바이더", example = "reuters")
        String provider
    ) {}
}
