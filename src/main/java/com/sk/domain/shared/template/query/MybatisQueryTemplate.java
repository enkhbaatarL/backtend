package com.sk.domain.shared.template.query;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

@Component
public class MybatisQueryTemplate {
    private final String CREATED_DATE_COLUMN = "crt_dtm"; //모든 TABLE 고정, 데이터 생성 일자
    private final String MODIFIED_DATE_COLUMN = "mod_dtm"; //모든 TABLE 고정, 데이터 수정 일자

    /**
     * 알림 조회 쿼리 테이블 별칭 템플릿
     * @param tableName
     * @return
     */
    public String getTableAliasForAlarm(String tableName) {
        return tableName + " AS t1";
    }

    /**
     * 알림 조회 쿼리 SELECT 컬럼 템플릿
     * @param columns
     * @param tableName
     * @param dateColumn
     * @param defaultSelectColumn
     * @param alias
     * @return
     */
    public String getSelectQueryForAlarm(String columns, String tableName, String dateColumn, String defaultSelectColumn, String alias, String conditionKeys) {
        // return String.format("""
        //         %s
        //         ,(
        //             SELECT %s
        //             FROM %s t2
        //             WHERE
        //             (
        //                 t2.%s < t1.%s
        //                 OR (t2.%s = t1.%s AND t2.%s < t1.%s)
        //                 OR (t2.%s = t1.%s AND t2.%s = t1.%s AND t2.%s < t1.%s)
        //             )
        //             AND t2.del_yn = 'N'
        //             ORDER BY t2.%s DESC, t2.%s DESC, t2.%s DESC
        //             LIMIT 1
        //         ) AS %s
        //         """, columns, defaultSelectColumn, tableName, CREATED_DATE_COLUMN, CREATED_DATE_COLUMN, CREATED_DATE_COLUMN, CREATED_DATE_COLUMN,
        //         dateColumn, dateColumn, CREATED_DATE_COLUMN, CREATED_DATE_COLUMN, dateColumn, dateColumn, defaultSelectColumn, defaultSelectColumn,
        //         CREATED_DATE_COLUMN, dateColumn, defaultSelectColumn, alias);
        String selectQuery1 = String.format("""
                %s,
                (
                    SELECT t2.%s
                    FROM %s t2
                    WHERE t2.%s < t1.%s
                      AND t2.del_yn = 'N'
                """, 
                columns, 
                defaultSelectColumn, 
                tableName, 
                dateColumn, dateColumn);

                if (conditionKeys != null) {
                    String[] conditionKeyArray = conditionKeys.split(",");
                    for (String conditionKey : conditionKeyArray) {
                        selectQuery1 += String.format("""
                                AND t2.%s = t1.%s
                                """, conditionKey, conditionKey);
                    }
                }

                selectQuery1 += String.format("""
                    ORDER BY t2.%s DESC, t2.%s DESC
                    LIMIT 1
                ) AS %s""", dateColumn, defaultSelectColumn, alias);

        return selectQuery1;
    }

    /**
     * 알림 조회 쿼리 WHERE 조건 템플릿
     * @param hours
     * @return
     */
    public String getWhereQueryForAlarm(String startDate, String endDate, String queryWhere) {
        // return String.format("""
        //         t1.%s >= DATE_SUB(NOW(), INTERVAL %d HOUR)
        //         AND t1.del_yn = 'N'
        //         """, CREATED_DATE_COLUMN, hours);

        if (queryWhere == null || queryWhere.isEmpty()) {
            return String.format("""
                    t1.%s BETWEEN '%s' AND '%s'
                    AND t1.del_yn = 'N'
                    """, MODIFIED_DATE_COLUMN, startDate, endDate);
        } else {
            return String.format("""
                    t1.%s BETWEEN '%s' AND '%s'
                    AND %s
                    AND t1.del_yn = 'N'
                    """, MODIFIED_DATE_COLUMN, startDate, endDate, queryWhere);
        }
    }

    /**
     * 알림 조회 쿼리 ORDER BY 템플릿
     * @param dateColumn
     * @param defaultSelectColumn
     * @return
     */
    public String getOrderByQueryForAlarm(String dateColumn, String defaultSelectColumn) {
        return String.format("""
                t1.%s ASC, t1.%s ASC
                """, dateColumn, defaultSelectColumn);
    }
}
