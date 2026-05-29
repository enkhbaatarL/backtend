package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record RelatedEventRes(

    @Schema(description = "이벤트 ID", example = "1")
    Long eventId,

    @Schema(description = "이벤트명", example = "미국/이스라엘 이란 공습 개시 (Operation Epic Fury)")
    String eventName,

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "수정일시")
    LocalDateTime updatedAt,

    @Schema(description = "이벤트 유형 Lv1", example = "geopolitical_risk")
    String eventTypeLv1,

    @Schema(description = "이벤트 유형 Lv1 라벨", example = "지정학적 리스크")
    String eventTypeLv1Label,

    @Schema(description = "인덱스별 영향도 (JKM, TTF, HH)")
    List<EventImpactIndexRes> indexImpacts

) {}
