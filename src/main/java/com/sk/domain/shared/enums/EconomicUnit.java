package com.sk.domain.shared.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 경제 지표 단위 Enum
 *
 * <p>Refinitiv 경제 달력 데이터에서 사용되는 단위를 정의합니다.</p>
 *
 * @see com.sk.domain.shared.entity.master.tif.EconomicCalendar #unit
 */
@Getter
@RequiredArgsConstructor
public enum EconomicUnit {

    // 통화 단위
    USD("USD", "$", "미국 달러", UnitCategory.CURRENCY),
    EUR("EUR", "€", "유로화", UnitCategory.CURRENCY),
    CNY("CNY", "¥", "위안화", UnitCategory.CURRENCY),

    // 비율/퍼센트
    PERCENT("Percent", "%", "퍼센트", UnitCategory.RATIO),
    RATIO("Ratio", "Ratio", "비율", UnitCategory.RATIO),

    // 잔고/지수
    BALANCE("Balance", "Bal.", "잔고/수지", UnitCategory.BALANCE),
    INDEX("Index", "Idx.", "지수", UnitCategory.INDEX),
    DIFFUSION_INDEX("Diff.Idx", "Diff.Idx", "확산 지수", UnitCategory.INDEX),

    // 에너지/원자재 단위
    BARREL_PER_DAY("Brl/Day", "bpd", "일일 배럴", UnitCategory.ENERGY),
    BARREL("Barrel", "Bbl", "배럴", UnitCategory.ENERGY),
    CUBIC_FEET("Cft", "cf", "입방피트", UnitCategory.ENERGY),

    // 무게 단위
    TON("Ton", "t", "미터톤", UnitCategory.WEIGHT),
    POUND("Pound", "lb", "파운드", UnitCategory.WEIGHT),

    // 농산물 단위
    BUSHEL("Bushel", "bu", "부셸", UnitCategory.AGRICULTURE),
    BALE("Bale", "Bale", "베일", UnitCategory.AGRICULTURE),

    // 수량/인원
    NUMBER_OF("No. of", "No.", "수량/건수", UnitCategory.QUANTITY),
    PERSON("Person", "p.", "인원수", UnitCategory.QUANTITY),

    // 시간
    HOUR("Hour", "hr", "시간", UnitCategory.TIME);

    /**
     * 원본 단위 코드
     */
    private final String code;

    /**
     * 약어 표시
     */
    @JsonValue
    private final String abbreviation;

    /**
     * 한글 설명
     */
    private final String description;

    /**
     * 단위 카테고리
     */
    private final UnitCategory category;

    /**
     * 코드로 EconomicUnit 찾기
     *
     * @param code 단위 코드 (예: "USD", "Percent")
     * @return 해당하는 EconomicUnit, 없으면 null
     */
    public static EconomicUnit fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        for (EconomicUnit unit : values()) {
            if (unit.code.equalsIgnoreCase(code)) {
                return unit;
            }
        }
        return null;
    }

    /**
     * 약어로 EconomicUnit 찾기
     *
     * @param abbreviation 약어 (예: "$", "%", "bpd")
     * @return 해당하는 EconomicUnit, 없으면 null
     */
    public static EconomicUnit fromAbbreviation(String abbreviation) {
        if (abbreviation == null || abbreviation.isBlank()) {
            return null;
        }

        for (EconomicUnit unit : values()) {
            if (unit.abbreviation.equalsIgnoreCase(abbreviation)) {
                return unit;
            }
        }
        return null;
    }

    /**
     * 카테고리별 단위 목록 조회
     *
     * @param category 단위 카테고리
     * @return 해당 카테고리의 단위 목록
     */
    public static List<EconomicUnit> findByCategory(UnitCategory category) {
        if (category == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(values())
            .filter(unit -> unit.category == category)
            .toList();
    }

    /**
     * 단위 카테고리 Enum
     */
    @Getter
    @RequiredArgsConstructor
    public enum UnitCategory {
        CURRENCY("통화"),
        RATIO("비율"),
        BALANCE("잔고/수지"),
        INDEX("지수"),
        ENERGY("에너지"),
        WEIGHT("무게"),
        AGRICULTURE("농산물"),
        QUANTITY("수량/인원"),
        TIME("시간");

        private final String description;
    }
}
