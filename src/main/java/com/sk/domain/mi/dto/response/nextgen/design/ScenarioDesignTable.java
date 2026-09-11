package com.sk.domain.mi.dto.response.nextgen.design;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 시나리오 설계 표 모델 (분기항목 헤더 + 시나리오 행).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScenarioDesignTable(
    List<Header> headers,
    List<Row> rows
) {

    /** 분기항목(컬럼) 헤더. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Header(
        String key,
        String label,
        Integer displayOrder
    ) {}

    /**
     * 시나리오 행. scenarioId가 null이면 신규 추가(미채번) 행.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Row(
        Long scenarioId,
        Integer displayOrder,
        String title,
        String narrative,
        Map<String, String> cells,
        Metrics metrics,
        String baselineYn,
        List<Signpost> signposts,
        String scenarioRole,
        String pressureTarget,
        String pressureBand,
        String pressureRationale
    ) {}

    /** 확률 메트릭. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Metrics(
        BigDecimal probabilityScore,
        BigDecimal probabilityDelta
    ) {}

    /** 시나리오 Signpost. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Signpost(
        Long signpostId,
        String label,
        String probabilityDirectionCode,
        String signpostReason,
        Integer displayOrder,
        String strength,
        String useYn
    ) {}

}
