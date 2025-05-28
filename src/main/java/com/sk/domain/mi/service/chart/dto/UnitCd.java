package com.sk.domain.mi.service.chart.dto;

/**
 * "<중분류>#<단위명>" 문자열을 타입‑세이프하게 감싸는 값 객체.
 */
public record UnitCd(

    String category,
    String unit

) {

    public static final String DELIM = "#";

    public UnitCd {
        category = category == null ? null : category.strip();
        unit = unit == null ? null : unit.strip();
    }

    /**
     * 문자열 → UnitCd
     * - "cat#kg" → category=cat, unit=kg
     * - "kg" → category=null, unit=kg
     * - "cat#" → category=cat, unit=null
     */
    public static UnitCd parse(String raw) {
        int idx = raw.indexOf(DELIM);
        if (idx < 0) {
            return new UnitCd(null, raw.strip());
        }
        String cat = raw.substring(0, idx);
        String uni = raw.substring(idx + 1);
        return new UnitCd(cat.isBlank() ? null : cat, uni.isBlank() ? null : uni);
    }

    /**
     * 직렬화 규칙
     * - cat!=null && unit!=null → "cat#unit"
     * - cat==null → "unit" (구분자 없음)
     * - unit==null → "cat" (구분자 없음)
     */
    @Override
    public String toString() {
        if (category == null) return unit;
        if (unit == null) return category;
        return category + DELIM + unit;
    }

}
