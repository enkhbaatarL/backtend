package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.response.nextgen.CmeBenchmarkGraphRes;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;

public interface CmeBenchmarkService {

    /**
     * CME 벤치마크 기준선 그래프 조회
     *
     * @param indexName  조회 인덱스 (JKM/TTF/HH)
     * @param periodUnit 기간 단위 (monthly/yearly). yearly는 월값 평균.
     */
    CmeBenchmarkGraphRes getCmeBenchmarkGraph(IndexName indexName, GraphPeriodUnit periodUnit);
}
