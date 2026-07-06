package com.sk.domain.mi.dto.response.nextgen.priceinterp;

import com.sk.domain.shared.entity.master.tmm.MiScenarioPriceInterp;
import com.sk.domain.shared.enums.nextgen.PriceInterpretationStatus;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 시나리오별 가격전망 해석 항목
 */
public record PriceInterpretationItemRes(

    @Schema(description = "시나리오 ID", example = "11")
    Long scenarioId,

    @Schema(description = "시나리오명", example = "단기 외교 해결 시나리오")
    String scenarioName,

    @Schema(description = "BC(Base Case) 여부", example = "false")
    boolean baseCase,

    @Schema(description = "표시 순서", example = "1")
    Integer displayOrder,

    @Schema(description = "상태 (generating|generated|failed)", example = "generated")
    PriceInterpretationStatus status,

    @Schema(description = "상태 명 (생성중|생성완료|생성실패)", example = "생성완료")
    String statusLabel,

    @Schema(description = "현재 해석 내용")
    String content,

    @Schema(description = "revision(참고용)", example = "0")
    Integer revision

) {

    public static PriceInterpretationItemRes of(ScenarioBaseRow scenario, MiScenarioPriceInterp interp) {

        // 만료(15분)된 생성중은 실패로 표시
        PriceInterpretationStatus displayStatus = interp.displayStatus();

        return new PriceInterpretationItemRes(
            scenario.scenarioId(),
            scenario.title(),
            Boolean.TRUE.equals(scenario.isBaseline()),
            scenario.displayOrder(),
            displayStatus,
            displayStatus.getLabel(),
            interp.getCurrentContent(),
            interp.getRevision()
        );
    }

}
