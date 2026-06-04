package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ImpactGrade;
import com.sk.domain.shared.repository.master.tmm.projection.EventIndexImpactRow;
import io.swagger.v3.oas.annotations.media.Schema;

// 이벤트 상세 화면 — 인덱스별 영향도. 단기·중장기를 nested 슬롯으로 표현
public record EventDetailImpactIndexRes(

    @Schema(description = "인덱스명", example = "JKM")
    String indexName,

    @Schema(description = "단기 영향 (없으면 null)")
    EventImpactIndexTermRes shortTerm,

    @Schema(description = "중장기 영향 (mid/long 병합. 둘 다 없으면 null)")
    EventImpactIndexTermRes midLongTerm

) {

    public static EventDetailImpactIndexRes from(EventIndexImpactRow row) {
        ImpactGrade shortGrade = row.impactShort();
        ImpactGrade midLongGrade = ImpactGrade.mergeMidLong(row.impactMid(), row.impactLong());
        return new EventDetailImpactIndexRes(
            row.targetId(),
            EventImpactIndexTermRes.of(shortGrade),
            EventImpactIndexTermRes.of(midLongGrade)
        );
    }

}
