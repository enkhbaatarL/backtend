package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RelatedEventScenarioRes(

    @Schema(description = "관련 이벤트 (1건)")
    RelatedEventRes event,

    @Schema(description = "관련 시나리오")
    List<RelatedScenarioRes> scenarios

) {}
