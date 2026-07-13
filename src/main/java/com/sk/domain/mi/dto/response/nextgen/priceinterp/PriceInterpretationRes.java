package com.sk.domain.mi.dto.response.nextgen.priceinterp;

import com.sk.domain.shared.entity.master.tmm.MiScenarioPriceInterp;
import com.sk.domain.shared.enums.nextgen.PriceInterpretationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 단일 시나리오 가격전망 해석 결과
 */
public record PriceInterpretationRes(

    @Schema(description = "시나리오 ID", example = "11")
    Long scenarioId,

    @Schema(description = "상태 (generating/generated/failed)", example = "generated")
    PriceInterpretationStatus status,

    @Schema(description = "상태 라벨", example = "생성완료")
    String statusLabel,

    @Schema(description = "현재 해석 내용", example = "단기 외교 해결 시나리오에서는 ...")
    String content,

    @Schema(description = "참고용 revision", example = "1")
    Integer revision,

    @Schema(description = "수정일시")
    LocalDateTime updatedAt

) {

    public static PriceInterpretationRes of(MiScenarioPriceInterp interp) {
        PriceInterpretationStatus displayStatus = interp.displayStatus(); // 만료된 생성중은 실패로 표시
        return new PriceInterpretationRes(
            interp.getScenario().getScenarioId(),
            displayStatus,
            displayStatus.getLabel(),
            interp.getCurrentContent(),
            interp.getRevision(),
            interp.getModifyDt()
        );
    }

}
