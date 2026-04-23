package com.sk.domain.shared.enums.scenarioworkbench;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventCategoryLv2 {
    WAR_MILITARY_CONFLICT("war_military_conflict", "전쟁 / 군사 분쟁", EventCategoryLv1.GEOPOLITICAL_RISK),
    GEOPOLITICAL_TENSION_INSTABILITY("geopolitical_tension_instability", "지정학적 긴장 / 불안정", EventCategoryLv1.GEOPOLITICAL_RISK),
    ECONOMIC_SANCTIONS_TRADE_RESTRICTIONS("economic_sanctions_trade_restrictions", "경제 제재 / 무역 제한", EventCategoryLv1.GEOPOLITICAL_RISK),
    REGIME_CHANGE_POLITICAL_TRANSITION("regime_change_political_transition", "정권 교체 / 정치 전환", EventCategoryLv1.GEOPOLITICAL_RISK),

    WINTER_COLD_WAVE("winter_cold_wave", "겨울 한파", EventCategoryLv1.WEATHER_CLIMATE),
    SUMMER_HEAT_WAVE("summer_heat_wave", "여름 고온", EventCategoryLv1.WEATHER_CLIMATE),
    NATURAL_DISASTER("natural_disaster", "자연재해", EventCategoryLv1.WEATHER_CLIMATE),

    UPSTREAM_PRODUCTION_FACILITY_CAPACITY_UTILIZATION_CHANGE("upstream_production_facility_capacity_utilization_change", "상류 생산 시설 Capa/가동률 변화", EventCategoryLv1.INFRASTRUCTURE_FACILITY),
    LIQUEFACTION_TERMINAL_FACILITY_CAPACITY_UTILIZATION_CHANGE("liquefaction_terminal_facility_capacity_utilization_change", "액화 / 터미널 시설 Capa/가동률 변화", EventCategoryLv1.INFRASTRUCTURE_FACILITY),
    PIPELINE_TRANSPORT_INFRASTRUCTURE_CAPACITY_UTILIZATION_CHANGE("pipeline_transport_infrastructure_capacity_utilization_change", "파이프라인/수송 인프라 Capa/가동률 변화", EventCategoryLv1.INFRASTRUCTURE_FACILITY),
    POWER_PLANT_FACILITY_CAPACITY_UTILIZATION_CHANGE("power_plant_facility_capacity_utilization_change", "발전소 (원전, 화력) 설비 Capa/가동률 변화", EventCategoryLv1.INFRASTRUCTURE_FACILITY),

    INVENTORY_STORAGE_LEVEL("inventory_storage_level", "재고 / 저장 수준", EventCategoryLv1.SUPPLY_DEMAND_BALANCE),
    REGIONAL_DEMAND_COMPETITION("regional_demand_competition", "지역간 수요 경쟁 (유럽 vs. 아시아)", EventCategoryLv1.SUPPLY_DEMAND_BALANCE),

    OCEAN_FREIGHT_CHARTER_RATE_CHANGE("ocean_freight_charter_rate_change", "해상 운임 / 용선료 변동", EventCategoryLv1.LOGISTICS_SHIPPING),
    CARGO_FLOW_CHANGE_PORT_CANAL_DISRUPTION("cargo_flow_change_port_canal_disruption", "화물 흐름 변화 / 항만·운하 차질", EventCategoryLv1.LOGISTICS_SHIPPING),

    NUCLEAR_POWER_EXPANSION_REDUCTION("nuclear_power_expansion_reduction", "원자력 발전 확대 / 축소", EventCategoryLv1.ENERGY_MIX_TRANSITION),
    RENEWABLE_POWER_EXPANSION_REDUCTION("renewable_power_expansion_reduction", "재생에너지 발전 확대 / 축소", EventCategoryLv1.ENERGY_MIX_TRANSITION),
    ALTERNATIVE_FUEL_AVAILABILITY("alternative_fuel_availability", "대체 연료 가용성", EventCategoryLv1.ENERGY_MIX_TRANSITION),

    INTEREST_RATE_FX_POLICY_CHANGE("interest_rate_fx_policy_change", "금리 / 통화정책 변화", EventCategoryLv1.MACROECONOMY_POLICY),
    COUNTRY_ECONOMIC_CONDITION_CHANGE("country_economic_condition_change", "국가별 경기 변화", EventCategoryLv1.MACROECONOMY_POLICY),

    TRANSACTION_HEDGING_COST_CHANGE("transaction_hedging_cost_change", "거래 / 헷징 비용 변동", EventCategoryLv1.MARKET_TRADING_STRUCTURE),
    INTER_INDEX_SPREAD_GAP("inter_index_spread_gap", "인덱스별 스프레드 Gap", EventCategoryLv1.MARKET_TRADING_STRUCTURE);

    private final String code;
    private final String description;
    private final EventCategoryLv1 parent;
}
