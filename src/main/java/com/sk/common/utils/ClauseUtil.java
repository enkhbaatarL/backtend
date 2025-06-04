package com.sk.common.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Stream;

@UtilityClass
public class ClauseUtil {

    public static final String BIZ_TARGET_FULFILL_DT_COLUMN = "prdc_fulf_dt";
    public static final String BIZ_TARGET_RESULT_DT_COLUMN = "prdc_rslt_dt";

    public static String getBizTargetSelectClause(String asIsSelectClause) {
        return asIsSelectClause + "," + BIZ_TARGET_FULFILL_DT_COLUMN;
    }

    public static String createWhereClause(String colName, String value) {
        return StringUtils.join(colName + " = ", value);
    }

    public static String createBetweenClause(String colName, String value1, String value2) {
        return StringUtils.join(colName + " BETWEEN ", value1, " AND ", value2);
    }

    public static String createSelectClause(String... cols) {
        StringBuilder sb = new StringBuilder();
        for (String col : cols) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(col);
        }
        return sb.toString();
    }

    public static String concatAlias(String column, String alias) {
        return String.join(" as ", column, alias);
    }

    public List<String> getSelectColumnList(String selectClause) {
        if (StringUtils.isBlank(selectClause)) {
            return List.of();
        }
        return Stream.of(selectClause.split(","))
            .map(String::strip)
            .toList();
    }

    public String[] getSelectColumnArrays(String selectClause) {
        if (StringUtils.isBlank(selectClause)) {
            return new String[0];
        }
        return Stream.of(selectClause.split(","))
            .map(String::strip)
            .toArray(String[]::new);
    }

    public String getMaxClause(String columnName, String alias) {
        if (StringUtils.isBlank(alias) || StringUtils.isBlank(alias)) {
            return "max(%s)".formatted(alias);
        }
        return "max(%s) as %s".formatted(columnName, alias);
    }

}
