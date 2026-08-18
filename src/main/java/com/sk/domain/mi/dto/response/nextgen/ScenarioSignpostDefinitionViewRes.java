package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ProbabilityDirection;
import com.sk.domain.shared.enums.nextgen.SignpostStrength;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioSignpostRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Signpost 정의. 관측 가능한 사건과 그것이 시나리오 확률에 주는 변화.
 * 시나리오 상세와 설계 세션(One-view)이 공유한다 — code는 enum, 표시는 {@code ~Label}로 함께 내린다.
 */
public record ScenarioSignpostDefinitionViewRes(

    @Schema(description = "Signpost ID", example = "1")
    Long signpostId,

    @Schema(description = "Signpost 라벨 (관측 사건)", example = "이란 혁명수비대 호르무즈 해협 부분 통항 허용 발표")
    String label,

    @Schema(description = "확률 변화 방향 (실DB는 sup 단일 코드)", example = "sup")
    ProbabilityDirection direction,

    @Schema(description = "확률 변화 방향 라벨", example = "가능성강화")
    String directionLabel,

    @Schema(description = "확률 변화 강도 (점수) - v1 그룹은 정수 스케일, v4 그룹은 소수 스케일(0.25~1.25). DB 원본 그대로 전달", example = "1.25")
    BigDecimal probabilityImpact,

    @Schema(description = "Signpost 영향도 (verystrong/strong/medium/weak/veryweak)", example = "strong")
    SignpostStrength strength,

    @Schema(description = "Signpost 영향도 라벨", example = "높음")
    String strengthLabel,

    @Schema(description = "Signpost 근거")
    String rationale,

    @Schema(description = "표시 순서", example = "1")
    Integer displayOrder

) {

    /** 시나리오 상세 projection 기반 조립. */
    public static ScenarioSignpostDefinitionViewRes of(ScenarioSignpostRow row) {
        return of(
            row.signpostId(),
            row.label(),
            row.probabilityDirection(),
            row.probabilityImpactScore(),
            row.strength(),
            row.signpostReason(),
            row.displayOrder()
        );
    }

    /** 값 직접 조립 (설계 세션 등). direction/strength 라벨은 enum에서 파생. */
    public static ScenarioSignpostDefinitionViewRes of(Long signpostId, String label, ProbabilityDirection direction,
                                                       BigDecimal probabilityImpact, SignpostStrength strength,
                                                       String rationale, Integer displayOrder) {
        return new ScenarioSignpostDefinitionViewRes(
            signpostId,
            label,
            direction,
            direction == null ? null : direction.getLabel(),
            probabilityImpact,
            strength,
            strength == null ? null : strength.getLabel(),
            rationale,
            displayOrder
        );
    }

}
