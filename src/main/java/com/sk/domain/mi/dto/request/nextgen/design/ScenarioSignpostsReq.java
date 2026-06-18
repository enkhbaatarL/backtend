package com.sk.domain.mi.dto.request.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.design.DesignEditTable;
import jakarta.validation.constraints.NotNull;

/**
 * 단계1 완료 / 단계2 진입 요청 (Signpost 생성).
 */
public record ScenarioSignpostsReq(

    @NotNull
    DesignEditTable table

) {

}
