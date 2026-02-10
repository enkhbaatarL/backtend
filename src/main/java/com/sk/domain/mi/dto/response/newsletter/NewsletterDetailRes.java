package com.sk.domain.mi.dto.response.newsletter;

import com.sk.common.model.TableRes;
import com.sk.domain.mi.dto.response.charts.Series;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;

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

        @Schema(description = "Base64 인코딩된 이미지 데이터", example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==")
        String imageBase64,

        @Schema(description = "활성화 여부", example = "true")
        Boolean enabled,

        @Schema(description = "발행본 상세")
        IssueDetail issueDetail) {

    public record IssueDetail(
            @Schema(description = "발행본 제목", example = "Oil Prices Surge Amid Middle East Tensions")
            String title,

            @Schema(description = "발행본 부가 정보", example = "부가적인 정보")
            String subInfo,

            @Schema(description = "발행일자", example = "2025년 9월 12일")
            String date,

            @Schema(description = "발행본 요약", example = "지정학적 긴장이 고조되면서 유가는 변동성을 이어갔습니다...")
            String summary,

            @Schema(description = "주식 시장 개장 시간", example = "1641859200000")
            String marketOpenTime,

            @Schema(description = "주식 시장 폐장 시간", example = "1641859200000")
            String marketCloseTime,

            @Schema(description = "주요 뉴스 흐름 차트")
            NewsFlowChart newsFlowChart,

            @Schema(description = "관련 뉴스")
            RelatedNews relatedNews,

            @Schema(description = "참고")
            List<Reference> references,

            @Schema(description = "이메일 제목", example = "[9/19(금) Oil Price Trends] 미-EU 우크라 안보 협상 및 이란 원유 제재 강화로 유가 강보합 $67.49/B (+1.1, +1.66%)")
            String emailSubject) {
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

    public record ChartData(
            @Schema(description = "타임스탬프 배열 (Unix timestamp in milliseconds)")
            List<String> timestamps,

            @Schema(description = "시리즈 데이터 배열 (high, low, open, close, volume)", example = "[]")
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
            Boolean showInChart,

            @Schema(description = "방향", example = "UP")
            String trend
            ) {
    }

    public record Reference(
        @Schema(description = "참고 제목", example = "WTI 및 달러 인덱스 5분 차트")
        String title,

        @Schema(description = "참고 설명", example = "WTI: 청색, 좌측\n달러인덱스: 회색, 우측\n상관관계: 20:00- 17:00+0.44")
        String description,

        @Getter
        @Schema(description = "참고 자료 유형", example = "TABLE", allowableValues = {"TABLE", "CHART"})
        String type,

        @Schema(description = "테이블 데이터 (type이 TABLE일 때만 사용)")
        TableRes tableData,

        @Schema(description = "차트 데이터 (type이 CHART일 때만 사용)")
        ChartData chartData) {

    public static Reference ofTable(String title, String description, TableRes tableData) {
        return new Reference(title, description, "TABLE", tableData, null);
    }

    public static Reference ofChart(String title, String description, ChartData chartData) {
        return new Reference(title, description, "CHART", null, chartData);
    }
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
