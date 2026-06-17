package com.sk.domain.mi.service.nextgen;

import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.domain.mi.dto.response.nextgen.CmeBenchmarkGraphRes;
import com.sk.domain.shared.enums.nextgen.CmeBenchmarkIndex;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.mapper.cme.CmeBenchmarkMapper;
import com.sk.domain.shared.mapper.dto.CmeBenchmarkPriceRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CmeBenchmarkServiceImpl implements CmeBenchmarkService {

    /** 표출 종료 연도 (기획서: 현재 연도 ~ 2030년). */
    private static final int END_YEAR = 2030;
    /** 연차트 평균 소수 자릿수. */
    private static final int YEARLY_AVG_SCALE = 2;

    private final CmeBenchmarkMapper cmeBenchmarkMapper;

    /**
     * CME 데이터가 적재된 전용 테넌트 라우팅 키 ({@code midb-skgas}).
     * 우선순위: 명시 프로퍼티 > 전용 DB 환경변수 > 기본값.
     */
    @Value("${mi.next-gen.cme.benchmark.datasource-key:${SKT_MI_DB_DEDICATED_NAME:midb-skgas}}")
    private String cmeDataSourceKey;

    @Override
    public CmeBenchmarkGraphRes getCmeBenchmarkGraph(IndexName indexName, GraphPeriodUnit periodUnit) {
        CmeBenchmarkIndex benchmarkIndex = CmeBenchmarkIndex.from(indexName);

        int currentYear = LocalDate.now().getYear();
        String fromMonth = "%d01".formatted(currentYear);
        String toMonth = "%d12".formatted(END_YEAR);

        List<CmeBenchmarkPriceRow> rows = queryOnDedicatedDataSource(benchmarkIndex.getIndexId(), fromMonth, toMonth);

        // value_month(YYYYMM) → 기간키. 연도별이면 연 단위 월평균으로 집계.
        Map<String, BigDecimal> valueByPeriodKey = periodUnit == GraphPeriodUnit.YEARLY
            ? aggregateYearly(rows)
            : toMonthlyMap(rows);

        List<String> periodKeys = new ArrayList<>(valueByPeriodKey.keySet());
        List<String> dates = periodKeys.stream().map(periodUnit::formatPeriodKey).toList();
        List<BigDecimal> values = periodKeys.stream().map(valueByPeriodKey::get).toList();

        return CmeBenchmarkGraphRes.of(indexName, periodUnit, dates, values);
    }

    /** 전용 테넌트 데이터소스로 라우팅하여 매퍼 조회 */
    private List<CmeBenchmarkPriceRow> queryOnDedicatedDataSource(long indexId, String fromMonth, String toMonth) {
        try {
            DataSourceContextHolder.setDataSourceKey(cmeDataSourceKey);
            return cmeBenchmarkMapper.findBenchmarkMonthlyPrices(indexId, fromMonth, toMonth);
        } finally {
            DataSourceContextHolder.clearDataSourceKey();
        }
    }

    /** 월별 — value_month(YYYYMM) 그대로 기간키. SQL이 이미 오름차순. */
    private Map<String, BigDecimal> toMonthlyMap(List<CmeBenchmarkPriceRow> rows) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (CmeBenchmarkPriceRow row : rows) {
            map.put(row.getValueMonth(), row.getIndexValue());
        }
        return map;
    }

    /** 연도별 — 연(YYYY) 단위 월값 평균 (현업 확정: 월평균 = 연도값). */
    private Map<String, BigDecimal> aggregateYearly(List<CmeBenchmarkPriceRow> rows) {
        Map<String, List<BigDecimal>> valuesByYear = new LinkedHashMap<>();
        for (CmeBenchmarkPriceRow row : rows) {
            String year = row.getValueMonth().substring(0, 4);
            valuesByYear.computeIfAbsent(year, k -> new ArrayList<>()).add(row.getIndexValue());
        }

        Map<String, BigDecimal> avgByYear = new LinkedHashMap<>();
        valuesByYear.forEach((year, values) -> {
            BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            avgByYear.put(year, sum.divide(BigDecimal.valueOf(values.size()), YEARLY_AVG_SCALE, RoundingMode.HALF_UP));
        });
        return avgByYear;
    }
}
