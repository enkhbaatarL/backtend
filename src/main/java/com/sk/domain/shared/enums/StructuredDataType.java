package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StructuredDataType {
    AAA("DATABASE", "TABLE", "COLUMN1, COLUMN2, COLUMN3", "WHERE"),
    BBB("DATABASE", "TABLE", "COLUMN1, COLUMN2, COLUMN3", "WHERE"),
    CCC("DATABASE", "TABLE", "COLUMN", "WHERE"),
    DDD("DATABASE", "TABLE", "COLUMN", "WHERE");

    private final String database;
    private final String table;
    private final String column;
    private final String where;
}
