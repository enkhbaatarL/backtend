package com.sk.common.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

/**
 * MySQL 전문검색(FULLTEXT) MATCH ... AGAINST 를 JPQL/QueryDSL 에서 호출할 수 있도록 등록
 * (Hibernate6 -> 미등록 SQL function 호출을 허용하지 않으므로 FunctionContributor 로 패턴을 등록)
 * 등록 이름 fulltext_match 는 function('fulltext_match', 컬럼, 검색어) 형태로 호출하며  match(컬럼) against (검색어 in boolean mode) 로 렌더링되고 관련도 점수(double)를 반환
 */
public class FullTextFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().registerPattern(
            "fulltext_match",
            "match(?1) against (?2 in boolean mode)",
            functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.DOUBLE)
        );
    }

}
