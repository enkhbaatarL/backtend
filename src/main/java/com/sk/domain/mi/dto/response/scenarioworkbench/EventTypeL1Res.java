package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.entity.master.tmm.MiEventTypeL1;
import io.swagger.v3.oas.annotations.media.Schema;

public record EventTypeL1Res(

    @Schema(description = "이벤트 유형 L1 ID", example = "1")
    Long eventTypeL1Id,

    @Schema(description = "이벤트 유형 L1 코드", example = "geopolitical_risk")
    String code,

    @Schema(description = "이벤트 유형 L1 라벨", example = "지정학적 리스크")
    String label

) {

    public static EventTypeL1Res from(MiEventTypeL1 entity) {
        return new EventTypeL1Res(
            entity.getEventTypeL1Id(),
            entity.getLevel1Code(),
            entity.getLevel1Label()
        );
    }

}
