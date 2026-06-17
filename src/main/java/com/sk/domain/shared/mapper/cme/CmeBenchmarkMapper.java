package com.sk.domain.shared.mapper.cme;

import com.sk.domain.shared.mapper.dto.CmeBenchmarkPriceRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * CME 벤치마크 조회 매퍼 — 고객사 전용 DB({@code midb-skgas})의 {@code tif_skgas_odin} 대상.
 */
@Mapper
public interface CmeBenchmarkMapper {

    /**
     * 월별 CME 벤치마크 가격 조회 (Future, index_div='F').
     *
     * 기획서 SQL 3단계를 union으로 통합:
     *  - 최종 기준일(value_date 최대) 확인
     *  - 현재 기준 월물 — 최종 기준일의 (미래) 월물
     *  - 현재 기준 이전 월물 — value_month별 최신 기준일 값
     *
     * @param indexId   대상 인덱스 ID (CmeBenchmarkIndex 매핑)
     * @param fromMonth 표출 시작 월 (현재 연도 1월, YYYYMM)
     * @param toMonth   표출 종료 월 (2030년 12월, YYYYMM)
     * @return value_month 오름차순 월별 (value_month, index_value) 목록
     */
    List<CmeBenchmarkPriceRow> findBenchmarkMonthlyPrices(
        @Param("indexId") long indexId,
        @Param("fromMonth") String fromMonth,
        @Param("toMonth") String toMonth
    );

}
