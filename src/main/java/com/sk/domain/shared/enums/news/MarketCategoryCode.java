package com.sk.domain.shared.enums.news;

import lombok.Getter;

public enum MarketCategoryCode {
    CROIL("CROIL", "Crude Oil", "원유"),
    LNG("LNG", "Natural Gas", "천연가스"),
    GAS("GAS", "Gas", "가스"),
    SCDT("SCDT", "Semiconductor", "반도체"),
    BTRY("BTRY", "EV Battery", "EV배터리");

    @Getter
    private final String code;
    @Getter
    private final String name;
    @Getter
    private final String description;

    MarketCategoryCode(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static MarketCategoryCode fromCode(String code) {
        for (MarketCategoryCode category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }

}
