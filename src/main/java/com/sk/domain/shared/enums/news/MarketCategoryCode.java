package com.sk.domain.shared.enums.news;

public enum MarketCategoryCode {
    CROIL("CROIL", "원유"),
    LNG("LNG", "천연가스"),
    SCDT("SCDT", "반도체"),
    BTRY("BTRY", "EV배터리");

    private final String code;
    private final String name;

    MarketCategoryCode(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static MarketCategoryCode fromCode(String code) {
        for (MarketCategoryCode category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

}
