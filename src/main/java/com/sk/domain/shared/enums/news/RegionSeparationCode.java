package com.sk.domain.shared.enums.news;

public enum RegionSeparationCode {

    GLO("Global", "Global (글로벌)"),
    APAC("Asia Pacific", "Asia Pacific (아시아 태평양)"),
    NAM("North America", "North America (북미)"),
    LATAM("Latin America", "Latin America (라틴 아메리카)"),
    EUR("Europe", "Europe (유럽)"),
    ME("Middle East", "Middle East (중동)"),
    AFR("Africa","Africa (아프리카)");

    private final String name;
    private final String description;

    private RegionSeparationCode(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
