package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.DesignEvidence;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes.EventBundle;
import com.sk.domain.mi.dto.response.nextgen.priceinterp.PriceInterpretationSectionRes;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupDetailBaseRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 시나리오 그룹 상세. 요약(Base Event 목록 + 근거 + 그룹 설명) + 가정·실행가능성(시나리오 표).
 * 가격 전망 탭은 별도 graph API, 개별 시나리오 상세는 scenario detail API 사용.
 */
public record ScenarioGroupDetailRes(

    @Schema(description = "시나리오 그룹 ID", example = "1")
    Long scenarioGroupId,

    @Schema(description = "시나리오 그룹명", example = "미-이란 충돌 및 호르무즈 해협 봉쇄")
    String groupName,

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "수정일시")
    LocalDateTime updatedAt,

    @Schema(description = "그룹을 구성하는 이벤트 묶음 (Base Event 표, 표시 순서. 설계 화면과 동일 계약)")
    List<EventBundle> eventBundles,

    @Schema(description = "생성 근거 마크다운 ('근거 보기' - Event 관계 분석/대표 시나리오 선정. v1 그룹 등 스냅샷이 없으면 null)", nullable = true)
    DesignEvidence evidence,

    @Schema(description = "시나리오 그룹 설명 (description 값을 summary 필드로 반환)")
    String summary,

    @Schema(description = "시나리오 수", example = "6")
    int scenarioCount,

    @Schema(description = "동적 표 헤더 정의 (그룹별 차원 칼럼)")
    List<ScenarioDimensionColumnRes> dimensionColumns,

    @Schema(description = "그룹 내 시나리오 (가정 및 실행 가능성 표 + 시나리오 상세 사이드바)")
    List<ScenarioGroupDetailRowRes> scenarios,

    @Schema(description = "시나리오별 가격전망 해석 section (버튼 활성 여부 + 해석 보유 시나리오)")
    PriceInterpretationSectionRes priceInterpretation

) {

    public static ScenarioGroupDetailRes of(ScenarioGroupDetailBaseRow base,
                                            List<EventBundle> eventBundles,
                                            DesignEvidence evidence,
                                            List<ScenarioDimensionColumnRes> dimensionColumns,
                                            List<ScenarioGroupDetailRowRes> scenarios,
                                            PriceInterpretationSectionRes priceInterpretation) {
        return new ScenarioGroupDetailRes(
            base.scenarioGroupId(),
            base.groupName(),
            base.createdAt(),
            base.updatedAt(),
            eventBundles,
            evidence,
            base.summary(),
            base.scenarioCount() == null ? scenarios.size() : base.scenarioCount(),
            dimensionColumns,
            scenarios,
            priceInterpretation
        );
    }

}
