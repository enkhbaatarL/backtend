package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioSignpostDefinitionViewRes;
import com.sk.domain.shared.enums.nextgen.GlobalPressureBand;
import com.sk.domain.shared.enums.nextgen.ScenarioRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 설계 세션 시나리오 표의 한 행 (FE 컨벤션). 조회 응답과 편집 요청(검토/단계2/확정)이 동일 구조로 공유한다.
 * step1/step2 공용 — 단계 진행에 따라 채워지는 필드가 늘어난다(step1은 narrative/probability/signposts가 null).
 * scenarioRole/pressureBand/pressureRationale은 v4(멀티 이벤트) 그룹에서만 채워지며 v1은 null이다.
 */
public record DesignScenarioRow(

    @Schema(description = "시나리오 ID (신규 추가 행이면 null)", example = "90001")
    Long scenarioId,

    @Schema(description = "신규 추가(미채번) 행 여부", example = "false")
    boolean isNew,

    @Schema(description = "표시 순서", example = "1")
    Integer displayOrder,

    @Schema(description = "시나리오명", example = "단기 외교 해결")
    String scenarioName,

    @Schema(description = "BC(Base Case) 여부", example = "false")
    boolean baseCase,

    @Schema(description = "시나리오 상대 역할 (floor/ceiling/mixed 배지, v4 전용. 없으면 null)", example = "ceiling", nullable = true)
    ScenarioRole scenarioRole,

    @Schema(description = "시나리오 상대 역할 라벨", example = "상단", nullable = true)
    String scenarioRoleLabel,

    @Schema(description = "글로벌 가격 압력 구간 (5단계 배지, v4 전용. 없으면 null)", example = "strong_bullish", nullable = true)
    GlobalPressureBand pressureBand,

    @Schema(description = "글로벌 가격 압력 구간 라벨", example = "강한 상승", nullable = true)
    String pressureBandLabel,

    @Schema(description = "역할/압력 판정 사유 (v4 전용. 없으면 null)", nullable = true)
    String pressureRationale,

    @Schema(description = "차원값 목록 (dimensionColumns 순서와 매칭)")
    List<ScenarioDimensionRes> dimensions,

    @Schema(description = "시나리오 발생 확률 (0~100 정수). 산출 전이면 null", example = "75")
    Integer probability,

    @Schema(description = "시나리오 내러티브 (step2 산출). 이전 단계면 null")
    String narrative,

    @Schema(description = "시나리오별 Signpost (step2 산출). 이전 단계면 null/빈 목록")
    List<ScenarioSignpostDefinitionViewRes> signposts

) {

    public static DesignScenarioRow of(Long scenarioId, Integer displayOrder, String scenarioName,
                                       boolean baseCase, ScenarioRole scenarioRole, GlobalPressureBand pressureBand,
                                       String pressureRationale, List<ScenarioDimensionRes> dimensions,
                                       Integer probability, String narrative,
                                       List<ScenarioSignpostDefinitionViewRes> signposts) {
        return new DesignScenarioRow(
            scenarioId,
            scenarioId == null,
            displayOrder,
            scenarioName,
            baseCase,
            // 유리M 요청사항 - 역할 배지 미노출. 인자로 받은 값이 있어도 null로 내린다 (되돌릴 때는 scenarioRole, scenarioRole == null ? null : scenarioRole.getLabel() 복원)
            null,
            null,
            pressureBand,
            pressureBand == null ? null : pressureBand.getLabel(),
            pressureRationale,
            dimensions,
            probability,
            narrative,
            signposts
        );
    }

}
