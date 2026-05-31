package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.ImpactGrade;
import com.sk.domain.shared.repository.master.tmm.projection.DriverTagScoreboardRow;
import io.swagger.v3.oas.annotations.media.Schema;

// 이벤트 상세 — Driver Tree Scoreboard
public record DriverTreeScoreboardRes(

    @Schema(description = "Driver Tree 1단계 코드", example = "country_lng_supply")
    String lv1Id,

    @Schema(description = "Driver Tree 1단계 명칭", example = "국가별 LNG 공급량")
    String lv1Label,

    @Schema(description = "Driver Tree 2단계 코드", example = "existing_infra_supply")
    String lv2Id,

    @Schema(description = "Driver Tree 2단계 명칭", example = "기존 인프라 공급량")
    String lv2Label,

    @Schema(description = "Driver Tree 3단계 코드", example = "supply_liquefaction_rate")
    String lv3Id,

    @Schema(description = "Driver Tree 3단계 명칭", example = "Liquefaction rate")
    String lv3Label,

    @Schema(description = "단기 영향")
    EventImpactIndexTermRes shortTerm,

    @Schema(description = "중장기 영향")
    EventImpactIndexTermRes midLongTerm,

    @Schema(description = "근거")
    String reason

) {

    public static DriverTreeScoreboardRes from(DriverTagScoreboardRow row) {
        ImpactGrade shortGrade = row.impactShort();
        ImpactGrade midLongGrade = ImpactGrade.mergeMidLong(row.impactMid(), row.impactLong());
        return new DriverTreeScoreboardRes(
            row.lv1Id(),
            row.lv1Label(),
            row.lv2Id(),
            row.lv2Label(),
            row.lv3Id(),
            row.lv3Label(),
            EventImpactIndexTermRes.of(shortGrade),
            EventImpactIndexTermRes.of(midLongGrade),
            row.driverReason()
        );
    }

}
