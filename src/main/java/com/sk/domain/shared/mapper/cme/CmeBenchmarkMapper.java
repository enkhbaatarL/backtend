package com.sk.domain.shared.mapper.cme;

import com.sk.domain.shared.mapper.dto.CmeBenchmarkPriceRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * CME 벤치마크 조회 매퍼 — 고객사 전용 DB({@code midb-skgas})의 {@code tif_skgas_odin} 대상.
 */
@Mapper
public interface CmeBenchmarkMapper {

    /**
     * 최종 데이터 기준일(value_date 최대) 조회 (Future, index_div='F').
     */
    LocalDateTime findLatestValueDate(@Param("indexId") String indexId);

    /**
     * 월별 CME 벤치마크 가격 조회 (Future, index_div='F').
     */
    List<CmeBenchmarkPriceRow> findBenchmarkMonthlyPrices(
        @Param("indexId") String indexId,
        @Param("fromMonth") String fromMonth,
        @Param("latestValueDate") LocalDateTime latestValueDate
    );

}
