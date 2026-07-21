package com.sk.common.utils;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringPath;

/**
 * ngram FULLTEXT 컬럼에 대한 BOOLEAN MODE 검색 조건 생성 유틸
 * 사용자가 입력한 키워드를 공백 단위로 나눠 OR 로 묶고, 각 토큰을 부분 매칭으로 검색한다.
 */
public final class FullTextSearchUtil {

    private FullTextSearchUtil() {
    }

    /**
     * ngram FULLTEXT BOOLEAN MODE 검색 조건을 만듦. 검색어가 비면 null 을 반환한다.
     */
    public static BooleanExpression matchBoolean(StringPath column, String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        String against = "+(%s)".formatted(keyword.trim().replaceAll("\\s+", " "));
        return Expressions.numberTemplate(Double.class, "function('fulltext_match', {0}, {1})", column, against).gt(0);
    }

}
