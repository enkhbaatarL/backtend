package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioPriceRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 시나리오 그룹 펼침 그래프 데이터 (단일 인덱스 × 단일 기간단위).
 */
public record ScenarioGroupGraphRes(

    @Schema(description = "시나리오 그룹 ID", example = "1")
    Long scenarioGroupId,

    @Schema(description = "인덱스명", example = "JKM")
    String indexName,

    @Schema(description = "기간 단위 (monthly/yearly)", example = "monthly")
    GraphPeriodUnit periodUnit,

    @Schema(description = "기간 단위 라벨", example = "월별")
    String periodUnitLabel,

    @Schema(description = "통화 (USD 고정)", example = "USD")
    String currency,

    @Schema(description = "단위 (MMBtu 고정)", example = "MMBtu")
    String unit,

    @ArraySchema(
        arraySchema = @Schema(description = "x축 기간 목록 (모든 시리즈 공유, 월별 yyyy-MM / 연도별 yyyy)"),
        schema = @Schema(description = "기간", example = "2026-06")
    )
    List<String> dates,

    @Schema(description = "기준선 시리즈 (CME, MI Base). 소스 확보 전까지 빈 배열")
    List<BaseSeries> baseSeries,

    @Schema(description = "시나리오별 예측 시리즈")
    List<ScenarioSeries> scenarioSeries

) {

    private static final String CURRENCY = "USD";
    private static final String UNIT = "MMBtu";

    public static ScenarioGroupGraphRes of(Long scenarioGroupId, IndexName indexName, GraphPeriodUnit periodUnit, List<ScenarioBaseRow> scenarios, List<ScenarioPriceRow> prices, Set<Long> executedScenarioIds, Set<Long> runningScenarioIds) {

        // x축 = 전 시나리오 기간키 union (시나리오마다 실행 시점이 달라 범위가 다를 수 있음 → 빠진 구간은 null 패딩)
        List<String> periodKeys = prices.stream()
            .map(ScenarioPriceRow::periodKey)
            .distinct()
            .sorted()
            .toList();

        Map<Long, Map<String, ScenarioPriceRow>> pricesByScenario = prices.stream()
            .collect(Collectors.groupingBy(ScenarioPriceRow::scenarioId,
                Collectors.toMap(ScenarioPriceRow::periodKey, Function.identity())));

        List<ScenarioSeries> scenarioSeries = scenarios.stream()
            .map(scenario -> ScenarioSeries.of(scenario, periodKeys, pricesByScenario.get(scenario.scenarioId()),
                executedScenarioIds != null && executedScenarioIds.contains(scenario.scenarioId()),
                runningScenarioIds != null && runningScenarioIds.contains(scenario.scenarioId())))
            .toList();

        return new ScenarioGroupGraphRes(
            scenarioGroupId,
            indexName.getCode(),
            periodUnit,
            periodUnit.getLabel(),
            CURRENCY,
            UNIT,
            periodKeys.stream().map(periodUnit::formatPeriodKey).toList(),
            List.of(),
            scenarioSeries
        );
    }

    public record BaseSeries(

        @Schema(description = "시리즈 식별자", example = "CME")
        String seriesId,

        @Schema(description = "시리즈명", example = "CME")
        String seriesName,

        @Schema(description = "값 목록 (dates와 동일 순서)")
        List<BigDecimal> values

    ) {}

    public record ScenarioSeries(

        @Schema(description = "시나리오 ID", example = "11")
        Long scenarioId,

        @Schema(description = "시나리오명", example = "단기 봉쇄와 제한적 공급 대응")
        String scenarioName,

        @Schema(description = "BC(Base Case) 여부", example = "false")
        boolean baseCase,

        @Schema(description = "수급모델 실행 완료 여부", example = "true")
        boolean executed,

        @Schema(description = "수급모델 실행중 여부", example = "false")
        boolean modelRunning,

        @Schema(description = "판정 상태 (pending/realized/discarded)", example = "pending")
        ScenarioDisplayStatus status,

        @Schema(description = "판정 상태 라벨 (대기/실현/폐기)", example = "대기")
        String statusLabel,

        @Schema(description = "베이스케이스 가격 목록 (dates와 동일 순서, 빠진 구간 null). 미실행이면 null", nullable = true)
        List<BigDecimal> baseCaseValues,

        @Schema(description = "사용자조정 가격 목록 (dates와 동일 순서, 빠진 구간 null). 미실행이면 null", nullable = true)
        List<BigDecimal> userAdjustedValues

    ) {

        static ScenarioSeries of(ScenarioBaseRow scenario, List<String> periodKeys, Map<String, ScenarioPriceRow> priceByPeriodKey, boolean executed, boolean modelRunning) {
            // executed/modelRunning은 최신 실행 상태(exec_stat_cd)로 판정. 그래프 값(line)은 최신 완료 가격이 있으면 표시(재실행 중에도 직전 결과 유지).
            boolean hasPrices = priceByPeriodKey != null && !priceByPeriodKey.isEmpty();
            ScenarioDisplayStatus status = ScenarioDisplayStatus.from(scenario.status());
            return new ScenarioSeries(
                scenario.scenarioId(),
                scenario.title(),
                Boolean.TRUE.equals(scenario.isBaseline()),
                executed,
                modelRunning,
                status,
                status == null ? null : status.getLabel(),
                hasPrices ? values(periodKeys, priceByPeriodKey, ScenarioPriceRow::baseCasePrice) : null,
                hasPrices ? values(periodKeys, priceByPeriodKey, ScenarioPriceRow::userAdjustedPrice) : null
            );
        }

        private static List<BigDecimal> values(List<String> periodKeys, Map<String, ScenarioPriceRow> priceByPeriodKey, Function<ScenarioPriceRow, BigDecimal> extractor) {
            return periodKeys.stream()
                .map(periodKey -> {
                    ScenarioPriceRow row = priceByPeriodKey.get(periodKey);
                    return row == null ? null : extractor.apply(row);
                })
                .toList();
        }

    }

}
