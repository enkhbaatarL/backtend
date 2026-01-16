package com.sk.domain.mi.dto.response.news;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;

public record MarketRecapRes(
    @Schema(description = "시장 리캡 ID", example = "1")
    Long marketRecapId,

    @Schema(description = "기준 일자", example = "20250729")
    String criteriaDate,

    @Schema(description = "뉴스 시장 카테고리 코드", example = "CROIL")
    String categoryCode,

    @Schema(description = "센티먼츠")
    Sentiments sentiments,

    @Schema(description = "하이라이트")
    Highlights highlights,

    @Schema(description = "체크포인트")
    Checkpoints checkpoints,

    @Schema(description = "분석 주기", example = "24H")
    String analysisFrequency,

    @Schema(description = "분석 기간", example = "[\"1753747200000\", \"1753833600000\"]")
    List<String> analysisPeriod

) {

    /**
     * 데이터가 없을 때 반환할 빈 응답 생성
     */
    public static MarketRecapRes empty() {
        return new MarketRecapRes(
            null,                           // marketRecapId
            null,                           // criteriaDate
            null,                           // categoryCode
            Sentiments.empty(),             // sentiments
            Highlights.empty(),             // highlights
            Checkpoints.empty(),            // checkpoints
            null,                           // analysisFrequency
            Collections.emptyList()         // analysisPeriod
        );
    }

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
        public static Sentiments empty() {
            return new Sentiments(
                SignalNewsCount.empty(),    // current
                SignalNewsCount.empty(),    // previous
                SignalNewsCount.empty(),    // last7days
                SignalNewsCount.empty()     // last30days
            );
        }
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
    ) {
        public static SignalNewsCount empty() {
            return new SignalNewsCount(
                0,      // upwardCount
                0,      // neutralCount
                0,      // downwardCount
                null    // period
            );
        }
    }

    public record HighlightEventRes(
        @Schema(description = "하이라이트 이벤트 ID", example = "1")
        Long highlightEventId,

        @Schema(description = "이벤트 한글명", example = "애플 주가 급등")
        String nameKorean,

        @Schema(description = "이벤트 영문명", example = "Apple Stock Surges")
        String nameEnglish,

        @Schema(description = "동향", example = "Upward")
        String momentum,

        @Schema(description = "관련 뉴스 목록")
        List<RelatedEventRepresentativeNewsRes> relatedEventRepresentativeNewsResNews
    ) {}

    public record CheckpointRes(
        @Schema(description = "체크포인트 ID", example = "1")
        Long checkpointId,

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
    ) {
        public static Narrative empty() {
            return new Narrative(
                null,                       // title
                Collections.emptyList()     // content
            );
        }
    }

    public record Highlights(
        @Schema(description = "투데이 하이라이트 이벤트 narrative")
        Narrative narrative,

        @Schema(description = "하이라이트 이벤트 목록")
        List<HighlightEventRes> items,

        @Schema(description = "하이라이트 이벤트에 속하는 뉴스 이벤트들의 대표 뉴스 목록")
        List<RelatedEventRepresentativeNewsRes> relatedEventRepresentativeNewsResNews
    ) {
        public static Highlights empty() {
            return new Highlights(
                Narrative.empty(),              // narrative
                Collections.emptyList(),        // items
                Collections.emptyList()         // relatedEventRepresentativeNewsResNews
            );
        }
    }

    public record Checkpoints(
        @Schema(description = "체크포인트 narrative")
        Narrative narrative,

        @Schema(description = "체크포인트 목록")
        List<CheckpointRes> items
    ) {
        public static Checkpoints empty() {
            return new Checkpoints(
                Narrative.empty(),              // narrative
                Collections.emptyList()         // items
            );
        }
    }

    public record RelatedEventRepresentativeNewsRes(
        @Schema(description = "하이라이트 이벤트에 속하는 뉴스 이벤트의 대표 뉴스 요약 ID", example = "123")
        Long newsSummaryId,

        @Schema(description = "하이라이트 이벤트에 속하는 뉴스 이벤트의 대표 뉴스 제목", example = "Apple Reports Strong Earnings")
        String title,

        @Schema(description = "하이라이트 이벤트에 속하는 뉴스 이벤트의 대표 뉴스 프로바이더", example = "reuters")
        String provider,

        @Schema(description = "보기 권한 여부", example = "true")
        boolean isViewable
    ) {}
}
