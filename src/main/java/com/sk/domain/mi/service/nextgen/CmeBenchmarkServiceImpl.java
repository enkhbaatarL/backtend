package com.sk.domain.mi.service.nextgen;

import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.domain.mi.dto.response.nextgen.ChartWindow;
import com.sk.domain.mi.dto.response.nextgen.CmeBenchmarkGraphRes;
import com.sk.domain.shared.enums.nextgen.CmeBenchmarkIndex;
import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.mapper.cme.CmeBenchmarkMapper;
import com.sk.domain.shared.mapper.dto.CmeBenchmarkPriceRow;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmd.projection.MiBasePriceRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    /** 화면·엑셀 표기 가격 자릿수 (소수 2자리, 반올림). */
    private static final int PRICE_SCALE = 2;

    private final CmeBenchmarkMapper cmeBenchmarkMapper;
    private final ScenarioCaseRepository scenarioCaseRepository;

    /**
     * CME 데이터가 적재된 전용 테넌트 라우팅 키 ({@code midb-skgas}).
     * 우선순위: 명시 프로퍼티 > 전용 DB 환경변수 > 기본값.
     */
    @Value("${mi.next-gen.cme.benchmark.datasource-key:${SKT_MI_DB_DEDICATED_NAME:midb-skgas}}")
    private String cmeDataSourceKey;

    @Override
    public CmeBenchmarkGraphRes getCmeBenchmarkGraph(IndexName indexName, GraphPeriodUnit periodUnit) {
        // 차트 표시 구간 = MI Base 최종월(월별 끝 기준)에 맞춘 윈도우
        return getCmeBenchmarkGraph(indexName, periodUnit, ChartWindow.of(periodUnit, getMiBaseEndResult(indexName, periodUnit)));
    }

    @Override
    public CmeBenchmarkGraphRes getCmeBenchmarkGraph(IndexName indexName, GraphPeriodUnit periodUnit, ChartWindow window) {
        CmeBenchmarkIndex benchmarkIndex = CmeBenchmarkIndex.from(indexName);

        List<CmeBenchmarkPriceRow> rows = queryOnDedicatedDataSource(benchmarkIndex.getIndexId(), window.queryFromMonth());

        // value_month(YYYY-MM) 기간키. 연도별이면 연 단위 월평균으로 집계.
        Map<String, BigDecimal> valueByPeriodKey = periodUnit == GraphPeriodUnit.YEARLY ? toYearlyAverageMap(rows) : toMonthlyMap(rows);

        List<String> dates = new ArrayList<>(valueByPeriodKey.keySet());
        List<BigDecimal> values = dates.stream().map(valueByPeriodKey::get).toList();

        return CmeBenchmarkGraphRes.of(indexName, periodUnit, dates, values).clip(window);
    }

    /**
     * MI Base 가장 큰 기간 조회
     */
    private String getMiBaseEndResult(IndexName indexName, GraphPeriodUnit periodUnit) {
        return scenarioCaseRepository.findLatestMiBasePrices(indexName, periodUnit.getDbUnit()).stream()
            .map(MiBasePriceRow::periodKey)
            .max(Comparator.naturalOrder())
            .map(periodUnit::formatPeriodKey)
            .orElse(null);
    }

    /**
     * 월별 Price Map 반환
     */
    private Map<String, BigDecimal> toMonthlyMap(List<CmeBenchmarkPriceRow> rows) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (CmeBenchmarkPriceRow row : rows) {
            map.put(row.getValueMonth(), scalePrice(row.getIndexValue()));
        }
        return map;
    }

    /**
     * 연도별 집계 후 월별 Price Map 반환
     */
    private Map<String, BigDecimal> toYearlyAverageMap(List<CmeBenchmarkPriceRow> rows) {
        Map<String, List<BigDecimal>> valuesByYear = new LinkedHashMap<>();
        for (CmeBenchmarkPriceRow row : rows) {
            String year = row.getValueMonth().substring(0, 4);
            valuesByYear.computeIfAbsent(year, k -> new ArrayList<>()).add(row.getIndexValue());
        }

        Map<String, BigDecimal> avgByYear = new LinkedHashMap<>();
        valuesByYear.forEach((year, values) -> {
            BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            avgByYear.put(year, sum.divide(BigDecimal.valueOf(values.size()), PRICE_SCALE, RoundingMode.HALF_UP));
        });
        return avgByYear;
    }

    private static BigDecimal scalePrice(BigDecimal value) {
        return value == null ? null : value.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 전용 테넌트 데이터소스로 라우팅하여 조회.
     */
    private List<CmeBenchmarkPriceRow> queryOnDedicatedDataSource(String indexId, String fromMonth) {
        try {
            DataSourceContextHolder.setDataSourceKey(cmeDataSourceKey);

            // 최종 데이터 기준일 확인
            LocalDateTime latestValueDate = cmeBenchmarkMapper.findLatestValueDate(indexId);
            if (latestValueDate == null) return List.of();

            // 현재 기준 월물 union 현재 기준 이전 월물
            return cmeBenchmarkMapper.findBenchmarkMonthlyPrices(indexId, fromMonth, latestValueDate);
        } finally {
            DataSourceContextHolder.clearDataSourceKey();
        }
    }

}
