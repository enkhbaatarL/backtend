package com.sk.domain.mi.dto.response.newsletter;

import io.swagger.v3.oas.annotations.media.Schema;
import com.sk.domain.mi.dto.response.charts.Series;
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

        @Schema(description = "이미지 URL", example = "https://example.com/images/newsletter1.jpg")
        String image,

        @Schema(description = "활성화 여부", example = "true")
        Boolean enabled,

        @Schema(description = "발행본 상세")
        IssueDetail issueDetail) {

    public record IssueDetail(
            @Schema(description = "발행본 제목", example = "Oil Prices Surge Amid Middle East Tensions")
            String title,

            @Schema(description = "발행일자", example = "2025년 9월 12일")
            String date,

            @Schema(description = "발행본 요약", example = "지정학적 긴장이 고조되면서 유가는 변동성을 이어갔습니다...")
            String summary,

            @Schema(description = "주요 뉴스 흐름 차트")
            NewsFlowChart newsFlowChart,

            @Schema(description = "관련 뉴스")
            RelatedNews relatedNews,

            @Schema(description = "참고")
            List<Reference> references) {
    }

    public record NewsFlowChart(
            @Schema(description = "차트 제목", example = "주요 뉴스 Flow 및 Brent 유가 추이")
            String title,

            @Schema(description = "집계 기준 시간", example = "뉴욕시간 전일 20:00부터 당일 17:00까지")
            String aggregationTime,

            @Schema(description = "차트 데이터")
            ChartData chartData) {
    }

    public record RelatedNews(
            @Schema(description = "집계 기준 시간", example = "뉴욕시간 전일 20:00부터 당일 17:00까지")
            String aggregationTime,

            @Schema(description = "뉴스 목록")
            List<NewsEventItem> newsEventItems) {
    }

    // 새로운 ChartData 구조
    public record ChartData(
            @Schema(description = "타임스탬프 배열 (Unix timestamp in milliseconds)")
            List<String> timestamps,

            @Schema(description = "시리즈 데이터 배열 (high, low, open, close, volume)")
            List<Series> series) {

        public static ChartData of(List<String> timestamps, List<Series> series) {
            return new ChartData(timestamps, series);
        }
    }

    public record NewsEventItem(
            @Schema(description = "뉴스 이벤트 ID", example = "NEWS_001")
            String newsId,

            @Schema(description = "뉴스 이벤트 제목", example = "유럽 주요 경제 지표 발표")
            String title,

            @Schema(description = "뉴스 이벤트 내용", example = "건설 생산, 총계(EA19), 전월 대비 변동, 물량 전월 IC +0.92% 증가한 스0.76% (05:00)")
            List<String> content,

            @Schema(description = "타임스탬프 (Unix timestamp in milliseconds)", example = "1641859200000")
            Long timestamp,

            @Schema(description = "발생일자", example = "2025-09-10")
            String createdDate,

            @Schema(description = "발생시간", example = "10:00")
            String createTime,

            @Schema(description = "뉴스 소스", example = "Bloomberg")
            String source,

            @Schema(description = "차트 표시 여부", example = "true")
            Boolean showInChart) {
    }

    public record Reference(
            @Schema(description = "참고 자료 제목", example = "EIA 주간 석유 재고 보고서")
            String title,

            @Schema(description = "참고 자료 설명", example = "미국 에너지정보청(EIA)의 주간 석유 재고 및 생산량 통계")
            String description,

            @Schema(description = "참고 자료 유형", example = "CHART")
            String type,

            @Schema(description = "데이터 항목 목록")
            List<DataItem> dataItems) {
    }

    public record DataItem(
            @Schema(description = "항목명", example = "원유 재고량")
            String name,

            @Schema(description = "값", example = "421.5")
            String value,

            @Schema(description = "단위", example = "백만 배럴")
            String unit,

            @Schema(description = "전주 대비 변화량", example = "+2.3")
            String change) {
    }
}