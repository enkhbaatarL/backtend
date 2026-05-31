package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.ImpactGrade;
import com.sk.domain.shared.enums.scenarioworkbench.ImpactTerm;
import com.sk.domain.shared.enums.scenarioworkbench.MarketDirection;
import com.sk.domain.shared.enums.scenarioworkbench.MarketImpactLevel;
import com.sk.domain.shared.repository.master.tmm.projection.EventIndexImpactRow;
import io.swagger.v3.oas.annotations.media.Schema;

public record EventImpactIndexRes(

    @Schema(description = "인덱스명", example = "JKM")
    String indexName,

    @Schema(description = "시장 영향도 수준", example = "high")
    MarketImpactLevel marketImpactLevel,

    @Schema(description = "시장 영향도 수준 라벨", example = "높음")
    String marketImpactLevelLabel,

    @Schema(description = "가격 방향", example = "bullish")
    MarketDirection direction,

    @Schema(description = "가격 방향 라벨", example = "상승")
    String directionLabel,

    @Schema(description = "영향 시점 구분 (short=단기 / mid_long=중장기)", example = "short")
    ImpactTerm impactTerm,

    @Schema(description = "영향 시점 라벨", example = "단기")
    String impactTermLabel

) {

    // 목록은 인덱스당 1개만 노출 - 단기 vs 중장기 중 영향 강도가 큰 시점을 선택함. 강도가 같으면 단기
    public static EventImpactIndexRes fromRow(EventIndexImpactRow row) {

        // 단기
        ImpactGrade shortGrade = row.impactShort();

        // 중장기
        ImpactGrade midLongGrade = ImpactGrade.mergeMidLong(row.impactMid(), row.impactLong());

        int shortMagnitude = shortGrade == null ? -1 : shortGrade.magnitude();
        int midLongMagnitude = midLongGrade == null ? -1 : midLongGrade.magnitude();

        if (midLongMagnitude > shortMagnitude) {
            return of(row.targetId(), midLongGrade, ImpactTerm.MID_LONG);
        }

        return of(row.targetId(), shortGrade, ImpactTerm.SHORT);

    }

    private static EventImpactIndexRes of(String indexName, ImpactGrade grade, ImpactTerm term) {
        MarketImpactLevel level = grade == null ? null : grade.getLevel();
        MarketDirection direction = grade == null ? null : grade.getDirection();
        return new EventImpactIndexRes(
            indexName,
            level,
            level == null ? null : level.getLabel(),
            direction,
            direction == null ? null : direction.getLabel(),
            term,
            term == null ? null : term.getLabel()
        );
    }

}
