package com.sk.domain.mi.dto.response.news;

import com.sk.domain.shared.enums.news.RecapCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;

public record MarketRecapListRes(
    @Schema(description = "시장 리캡 ID", example = "1")
    Long marketRecapId,

    @Schema(description = "기준 일자", example = "20250729")
    String criteriaDate,

    @Schema(description = "뉴스 시장 카테고리 코드", example = "CROIL")
    String categoryCode,

    @Schema(description = "리캡 카테고리 (HIGHLIGHT/CHECKPOINT)", example = "HIGHLIGHT")
    RecapCategory recapCategory,

    @Schema(description = "아이템 목록 (HIGHLIGHT일 때: HighlightEventRes[], CHECKPOINT일 때: CheckpointRes[])")
    Object items
) {

    /**
     * HIGHLIGHT 타입 응답 생성
     */
    public static MarketRecapListRes ofHighlight(
        Long marketRecapId,
        String criteriaDate,
        String categoryCode,
        List<HighlightEventRes> highlightItems
    ) {
        return new MarketRecapListRes(
            marketRecapId,
            criteriaDate,
            categoryCode,
            RecapCategory.HIGHLIGHT,
            highlightItems
        );
    }

    /**
     * CHECKPOINT 타입 응답 생성
     */
    public static MarketRecapListRes ofCheckpoint(
        Long marketRecapId,
        String criteriaDate,
        String categoryCode,
        List<CheckpointRes> checkpointItems
    ) {
        return new MarketRecapListRes(
            marketRecapId,
            criteriaDate,
            categoryCode,
            RecapCategory.CHECKPOINT,
            checkpointItems
        );
    }

    /**
     * 데이터가 없을 때 반환할 빈 응답 생성
     */
    public static MarketRecapListRes empty(RecapCategory recapCategory) {
        return new MarketRecapListRes(
            null,
            null,
            null,
            recapCategory,
            Collections.emptyList()
        );
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

    public record RelatedEventRepresentativeNewsRes(
        @Schema(description = "하이라이트 이벤트에 속하는 뉴스 이벤트의 대표 뉴스 요약 ID", example = "123")
        Long newsSummaryId,

        @Schema(description = "하이라이트 이벤트에 속하는 뉴스 이벤트의 대표 뉴스 제목", example = "Apple Reports Strong Earnings")
        String title,

        @Schema(description = "하이라이트 이벤트에 속하는 뉴스 이벤트의 대표 뉴스 프로바이더", example = "reuters")
        String provider
    ) {}
}
