package com.sk.common.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

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

}
