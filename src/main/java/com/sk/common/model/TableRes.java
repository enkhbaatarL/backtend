package com.sk.common.model;

import java.util.List;

public record TableRes(

    List<Column> headers,
    List<Row> rows

) {

    public record Column(

        String field,
        String name

    ) {

        public static Column of(String field, String name) {
            return new Column(field, name);
        }

    }

    public record Row(

        List<String> cells

    ) {

        public static Row of(List<String> cells) {
            return new Row(cells);
        }

    }

    public static TableRes of(List<Column> headers, List<Row> rows) {
        return new TableRes(headers, rows);
    }

}
