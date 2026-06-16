package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.shared.enums.nextgen.ProbabilityDirection;
import com.sk.domain.shared.enums.nextgen.SignpostStrength;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 확정 요청의 편집 Signpost. 확정 단계에서 사용자가 편집 가능한 필드만 받는다.
 * <p>
 * 응답 뷰({@link com.sk.domain.mi.dto.response.nextgen.ScenarioSignpostDefinitionViewRes})와 달리
 * 표시용 라벨(directionLabel/strengthLabel)·확률영향점수(Agent 산출)는 제외하고, 사용여부({@code used})를 포함한다.
 */
public record DesignConfirmSignpost(

    @Schema(description = "Signpost ID", example = "1")
    Long signpostId,

    @Schema(description = "Signpost 라벨 (관측 사건)", example = "호르무즈 봉쇄 장기화")
    String label,

    @Schema(description = "확률 변화 방향 (phase1은 sup 단일)", example = "sup")
    ProbabilityDirection direction,

    @Schema(description = "Signpost 근거", example = "운송 리스크 확대 및 대체 항로 비용 증가")
    String rationale,

    @Schema(description = "Signpost 영향도 (verystrong/strong/medium/weak/veryweak)", example = "strong")
    SignpostStrength strength,

    @Schema(description = "표시 순서", example = "1")
    Integer displayOrder,

    @Schema(description = "사용 여부 (signpost on/off)", example = "true")
    boolean used

) {}
