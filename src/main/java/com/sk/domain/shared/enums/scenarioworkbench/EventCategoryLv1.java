package com.sk.domain.shared.enums.scenarioworkbench;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventCategoryLv1 {
    GEOPOLITICAL_RISK("geopolitical_risk", "지정학적 리스크"),
    WEATHER_CLIMATE("weather_climate", "기상 / 기후"),
    INFRASTRUCTURE_FACILITY("infrastructure_facility", "인프라 / 설비"),
    SUPPLY_DEMAND_BALANCE("supply_demand_balance", "수급 밸런스"),
    LOGISTICS_SHIPPING("logistics_shipping", "물류 / 해운"),
    ENERGY_MIX_TRANSITION("energy_mix_transition", "에너지 믹스 전환"),
    MACROECONOMY_POLICY("macroeconomy_policy", "거시경제 / 정책"),
    MARKET_TRADING_STRUCTURE("market_trading_structure", "시장 / 거래 구조");

    private final String code;
    private final String description;
}
