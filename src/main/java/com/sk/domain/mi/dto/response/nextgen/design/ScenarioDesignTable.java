package com.sk.domain.mi.dto.response.nextgen.design;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 시나리오 설계 표 모델 (분기항목 헤더 + 시나리오 행).
 * 설계 세션 폴링 응답 / 검토·확정 요청 / Agent 콜백 본문이 공유한다.
 */
public record ScenarioDesignTable(
    List<Header> headers,
    List<Row> rows
) {

    /** 분기항목(컬럼) 헤더. */
    public record Header(String key, String label, Integer displayOrder) {}

    /** 시나리오 행. scenarioId가 null이면 신규 추가(미채번) 행. */
    public record Row(
        Long scenarioId,
        Integer displayOrder,
        String title,
        String narrative,
        Map<String, String> cells,
        Metrics metrics,
        Impact impact,
        String baselineYn,
        List<Signpost> signposts
    ) {}

    /** 확률 메트릭. */
    public record Metrics(BigDecimal probabilityScore, BigDecimal probabilityDelta) {}

    /** 인덱스별 시장 영향도. */
    public record Impact(String jkm, String ttf, String hh) {}

    /** 시나리오 Signpost. */
    public record Signpost(
        Long signpostId,
        String label,
        String probabilityDirectionCode,
        BigDecimal probabilityImpactScore,
        String signpostReason,
        Integer displayOrder,
        String strength,
        String useYn
    ) {}
}
