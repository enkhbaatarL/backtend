package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

// 이벤트 상세 — Driver Tree Scoreboard. 인덱스(JKM/TTF/HH)별로 driver 묶음을 분리해 표시.
public record EventDriverTreeIndexRes(

    @Schema(description = "인덱스명", example = "JKM")
    String indexName,

    @Schema(description = "해당 인덱스에 매핑된 Driver 목록")
    List<DriverTreeScoreboardRes> drivers

) {
}
