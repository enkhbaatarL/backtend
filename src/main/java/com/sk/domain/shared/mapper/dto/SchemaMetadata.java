package com.sk.domain.shared.mapper.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SchemaMetadata {

    private String dbName;
    private String tableName;
    private String columnName;
    private String dataType; // e.g., VARCHAR, INT
    private String columnType; // e.g., VARCHAR(255), INT(11)
    private Integer characterMaximumLength; // 50

}
