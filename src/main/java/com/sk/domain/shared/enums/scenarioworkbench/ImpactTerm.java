package com.sk.domain.shared.enums.scenarioworkbench;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImpactTerm {

    SHORT("short", "단기"),
    MID_LONG("mid_long", "중장기");

    @JsonValue
    private final String code;
    private final String label;

    public static ImpactTerm fromCode(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        String lower = dbValue.toLowerCase();
        for (ImpactTerm term : values()) {
            if (term.code.equals(lower)) {
                return term;
            }
        }
        return null;
    }

}
