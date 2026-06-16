package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ProbabilityDirection;
import com.sk.domain.shared.enums.nextgen.SignpostStrength;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioSignpostRow;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Signpost 정의 (시나리오 상세). 관측 가능한 사건과 그것이 시나리오 확률에 주는 변화.
 */
public record ScenarioSignpostDefinitionViewRes(

    @Schema(description = "Signpost 라벨 (관측 사건)", example = "이란 혁명수비대 호르무즈 해협 부분 통항 허용 발표")
    String label,

    @Schema(description = "확률 변화 방향 (실DB는 sup 단일 코드)", example = "sup")
    ProbabilityDirection direction,

    @Schema(description = "확률 변화 방향 라벨", example = "가능성강화")
    String directionLabel,

    @Schema(description = "확률 변화 강도 (점수)", example = "54")
    Integer probabilityImpact,

    @Schema(description = "Signpost 영향도 (verystrong/strong/medium/weak/veryweak)", example = "strong")
    SignpostStrength strength,

    @Schema(description = "Signpost 영향도 라벨", example = "강함")
    String strengthLabel,

    @Schema(description = "Signpost 근거")
    String rationale

) {

    public static ScenarioSignpostDefinitionViewRes of(ScenarioSignpostRow row) {
        ProbabilityDirection direction = row.probabilityDirection();
        SignpostStrength strength = row.strength();
        return new ScenarioSignpostDefinitionViewRes(
            row.label(),
            direction,
            direction == null ? null : direction.getLabel(),
            row.probabilityImpactScore() == null ? null : row.probabilityImpactScore().intValue(),
            strength,
            strength == null ? null : strength.getLabel(),
            row.signpostReason()
        );
    }

}
