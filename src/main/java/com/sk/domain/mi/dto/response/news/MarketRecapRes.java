package com.sk.domain.mi.dto.response.news;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MarketRecapRes(
    @Schema(description = "시장 리캡 ID", example = "1")
    Long mrktRcapId,

    @Schema(description = "기준 일자", example = "20250729")
    String crtrDt,

    @Schema(description = "뉴스 시장 카테고리 코드", example = "STOCK")
    String nwsMrktCtgrCd,

    @Schema(description = "상승 신호 뉴스 수", example = "15")
    Integer upwSgnlNwsCnt,

    @Schema(description = "중립 신호 뉴스 수", example = "8")
    Integer ntrlSgnlNwsCnt,

    @Schema(description = "하락 신호 뉴스 수", example = "3")
    Integer dwnSgnlNwsCnt,

    @Schema(description = "하이라이트 제목", example = "Tech Sector Leads Market Rally")
    String hiTitl,

    @Schema(description = "하이라이트 내용", example = "Technology stocks showed strong performance...")
    String hiCntt,

    @Schema(description = "체크 포인트 제목", example = "Key Points to Watch")
    String ckptTitl,

    @Schema(description = "체크 포인트 내용", example = "Watch for Fed announcements...")
    String ckptCntt,

    @Schema(description = "하이라이트 아이템 목록")
    List<HighlightItemRes> highlightItems,

    @Schema(description = "체크포인트 아이템 목록")
    List<CheckpointItemRes> checkpointItems
) {

    public record HighlightItemRes(
        @Schema(description = "하이라이트 아이템 ID", example = "1")
        Long mrktRcapHlItmId,

        @Schema(description = "제목", example = "Apple Stock Surges")
        String titl,

        @Schema(description = "내용", example = "Apple shares rose 5% today...")
        String cntt,

        @Schema(description = "동향", example = "UP")
        String mntm,

        @Schema(description = "관련 뉴스 목록")
        List<RelatedNewsRes> relatedNews
    ) {}

    public record CheckpointItemRes(
        @Schema(description = "체크포인트 아이템 ID", example = "1")
        Long mrktRcapCkptItmId,

        @Schema(description = "제목", example = "Federal Reserve Meeting")
        String titl,

        @Schema(description = "내용", example = "Fed to announce interest rate decision...")
        String cntt
    ) {}

    public record RelatedNewsRes(
        @Schema(description = "뉴스 요약 ID", example = "123")
        Long newsSummaryId,

        @Schema(description = "뉴스 제목", example = "Apple Reports Strong Earnings")
        String title,

        @Schema(description = "프로바이더", example = "reuters")
        String provider
    ) {}
}
