package com.sk.domain.mi.service.chart.dto;

import java.util.List;
import java.util.Map;

/**
 * 원천 데이터 쿼리 컨텍스트 정보를 담는 record.
 */
public record RawDataQueryContext(
    List<String> selectClause,
    Map<String, String> columnExpressions,
    String dateColumn,
    String whereClause
) {
}
