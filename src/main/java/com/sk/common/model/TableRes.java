package com.sk.common.model;

import java.util.List;

public record TableRes(

    List<String> headers,
    List<Row> rows

) {

    public record Column(

        String field,
        String ColumnName

    ) {

    }

    public record Row(

        List<String> cells

    ) {

    }

}
