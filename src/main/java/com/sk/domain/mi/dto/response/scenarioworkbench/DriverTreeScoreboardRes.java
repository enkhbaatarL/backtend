package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.MarketDirection;
import com.sk.domain.shared.enums.scenarioworkbench.MarketImpactLevel;
import com.sk.domain.shared.repository.master.tmm.projection.DriverTagScoreboardRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;

public record DriverTreeScoreboardRes(

    @Schema(description = "Driver Tree 1단계 코드", example = "supply")
    String lv1Id,

    @Schema(description = "Driver Tree 1단계 명칭", example = "공급")
    String lv1Label,

    @Schema(description = "Driver Tree 2단계 코드", example = "gas_field_capacity")
    String lv2Id,

    @Schema(description = "Driver Tree 2단계 명칭", example = "가스전 Capa/가동률")
    String lv2Label,

    @Schema(description = "Driver Tree 3단계 코드", example = "ras_laffan")
    String lv3Id,

    @Schema(description = "Driver Tree 3단계 명칭", example = "Ras Laffan")
    String lv3Label,

    @Schema(description = "영향도 수준", example = "high")
    MarketImpactLevel impactLevel,

    @Schema(description = "영향도 수준 라벨", example = "높음")
    String impactLevelLabel,

    @Schema(description = "방향", example = "bullish")
    MarketDirection direction,

    @Schema(description = "방향 라벨", example = "상승")
    String directionLabel,

    @Schema(description = "근거")
    String reason

) {

    public static DriverTreeScoreboardRes from(DriverTagScoreboardRow row) {
        MarketImpactLevel impactLevel = MarketImpactLevel.fromCode(row.magnitudeLevel());
        MarketDirection direction = MarketDirection.fromCode(row.direction());

        return new DriverTreeScoreboardRes(
            row.lv1Id(),
            row.lv1Label(),
            row.lv2Id(),
            row.lv2Label(),
            row.lv3Id(),
            row.lv3Label(),
            impactLevel,
            Optional.ofNullable(impactLevel).map(MarketImpactLevel::getLabel).orElse(null),
            direction,
            Optional.ofNullable(direction).map(MarketDirection::getLabel).orElse(null),
            row.driverReason()
        );
    }

}
