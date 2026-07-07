package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.GraphPeriodUnit;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import com.sk.domain.shared.repository.master.tmd.projection.MiBasePriceRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioPriceRow;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;

/**
 * 시나리오 그룹 펼침 그래프 데이터 (단일 인덱스 × 단일 기간단위).
 */
@Builder(toBuilder = true)
public record ScenarioGroupGraphRes(

    @Schema(description = "시나리오 그룹 ID", example = "1")
    Long scenarioGroupId,

    @Schema(description = "파라미터 제안 대상여부 (true=대상/PARAMETER_PROPOSAL, false=미대상/FILE_UPLOAD)", example = "true")
    Boolean parameterProposalTarget,

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

    @Schema(description = "기준선 시리즈 (MI Base)")
    List<BaseSeries> baseSeries,

    @Schema(description = "시나리오별 예측 시리즈")
    List<ScenarioSeries> scenarioSeries

) {

    private static final String CURRENCY = "USD";
    private static final String UNIT = "MMBtu";

    /**
     * 화면 및 엑셀 표기 가격 자릿수 (소수 2자리, 반올림).
     * */
    private static final int PRICE_SCALE = 2;

    public static ScenarioGroupGraphRes of(Long scenarioGroupId, boolean parameterProposalTarget, IndexName indexName, GraphPeriodUnit periodUnit,
                                           List<ScenarioBaseRow> scenarios, List<MiBasePriceRow> miBasePrices, List<ScenarioPriceRow> scenariosPrices,
                                           Set<Long> runningScenarioIds, Set<Long> completedScenarioIds) {

        // x축 = 시나리오 + MI Base 기간키 union (실행 시점이 달라 범위가 다를 수 있음 → 빠진 구간은 null 패딩)
        List<String> periodKeys = Stream.concat(
                scenariosPrices.stream().map(ScenarioPriceRow::periodKey),
                miBasePrices.stream().map(MiBasePriceRow::periodKey)
            )
            .distinct()
            .sorted()
            .toList();

        // 시나리오 시리즈
        List<ScenarioSeries> scenarioSeries = ScenarioSeries.of(scenarios, periodKeys, scenariosPrices, runningScenarioIds, completedScenarioIds);

        // MI Base 기준선 시리즈
        List<BaseSeries> baseSeries = BaseSeries.of(periodKeys, miBasePrices);

        return ScenarioGroupGraphRes.builder()
            .scenarioGroupId(scenarioGroupId)
            .parameterProposalTarget(parameterProposalTarget)
            .indexName(indexName.getCode())
            .periodUnit(periodUnit)
            .periodUnitLabel(periodUnit.getLabel())
            .currency(CURRENCY)
            .unit(UNIT)
            .dates(periodKeys.stream().map(periodUnit::formatPeriodKey).toList())
            .baseSeries(baseSeries)
            .scenarioSeries(scenarioSeries)
            .build();
    }

    /** 화면·엑셀 표기용 가격 정규화 — 소수 2자리 반올림(HALF_UP). null은 그대로. */
    private static BigDecimal scalePrice(BigDecimal value) {
        return value == null ? null : value.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * dates 중 구간에 포함되는 인덱스 목록
     */
    private static List<Integer> keepIndices(List<String> dates, ChartWindow window) {
        List<Integer> keep = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            if (window.contains(dates.get(i))) {
                keep.add(i);
            }
        }
        return keep;
    }

    /**
     * 값 목록에서 keepIdx 위치의 값만 같은 순서로 추린다.
     * null 시리즈는 null 유지 -> 미실행 시나리오
     */
    private static List<BigDecimal> sliceValues(List<BigDecimal> values, List<Integer> keepIdx) {
        if (values == null) return null;
        return keepIdx.stream()
            .map(i -> i < values.size() ? values.get(i) : null)
            .toList();
    }

    /**
     * [Dedicated 병합] 다른 기준선(CME 등)을 같은 인덱스 그래프에 합쳐 새 그래프를 만든다.
     *
     * MI(시나리오 + MI Base)와 외부 기준선(CME)은 산출 기준이 달라 기간 범위가 어긋날 수 있다.
     * 정합/클램프하지 않고 x축은 양측 표기(yyyy-MM / yyyy) union, 각 시리즈는 자기 값이 없는 구간을 null(엑셀 빈칸)로 재패딩한다.
     * 내부 periodKey 포맷이 서로 달라(MI=YYYYMM, CME=YYYY-MM) 표기 레벨에서 병합한다.
     *
     * @param extraDates      합칠 기준선의 표기 기간 목록 (extraBaseSeries 값과 동일 순서)
     * @param extraBaseSeries 합칠 기준선 시리즈 (extraDates 순서에 정렬돼 있어야 함)
     */
    public static ScenarioGroupGraphRes withBenchmark(ScenarioGroupGraphRes graph, List<String> extraDates, List<BaseSeries> extraBaseSeries) {

        List<String> mergedDates = Stream.concat(graph.dates().stream(), extraDates.stream())
            .distinct()
            .sorted()
            .toList();

        List<BaseSeries> baseSeries = new ArrayList<>();

        for (BaseSeries base : graph.baseSeries()) {
            baseSeries.add(new BaseSeries(base.seriesId(), base.seriesName(), realign(mergedDates, graph.dates(), base.values())));
        }
        for (BaseSeries base : extraBaseSeries) {
            baseSeries.add(new BaseSeries(base.seriesId(), base.seriesName(), realign(mergedDates, extraDates, base.values())));
        }

        List<ScenarioSeries> scenarioSeries = graph.scenarioSeries().stream()
            .map(series -> series.realignTo(mergedDates, graph.dates()))
            .toList();

        return graph.toBuilder()
            .dates(mergedDates)
            .baseSeries(baseSeries)
            .scenarioSeries(scenarioSeries)
            .build();
    }

    /**
     * 기존 dates 순서의 값들을 targetDates 순서로 재배열 (없는 구간 null). values가 null이면 null 유지(미실행 시나리오).
     */
    private static List<BigDecimal> realign(List<String> targetDates, List<String> sourceDates, List<BigDecimal> sourceValues) {
        if (sourceValues == null) {
            return null;
        }
        Map<String, BigDecimal> valueByDate = new HashMap<>();
        for (int i = 0; i < sourceDates.size() && i < sourceValues.size(); i++) {
            valueByDate.put(sourceDates.get(i), sourceValues.get(i));
        }
        return targetDates.stream().map(valueByDate::get).toList();
    }

    /**
     * 차트 표시 구간으로 x축(dates)과 모든 시리즈(MI Base + 시나리오) 값을 잘라낸 새 그래프를 만든다.
     * 구간 밖 기간은 dates에서 제거하고, 각 시리즈도 동일 위치의 값을 함께 제거한다.
     */
    public ScenarioGroupGraphRes clip(ChartWindow window) {
        List<Integer> keepIdx = keepIndices(dates, window);

        if (keepIdx.size() == dates.size()) return this;

        List<String> clippedDates = keepIdx.stream().map(dates::get).toList();

        List<BaseSeries> clippedBase = baseSeries.stream()
            .map(base -> new BaseSeries(
                base.seriesId(),
                base.seriesName(),
                sliceValues(base.values(), keepIdx)
            ))
            .toList();

        List<ScenarioSeries> clippedScenario = scenarioSeries.stream()
            .map(series -> series.toBuilder()
                .baseCaseValues(sliceValues(series.baseCaseValues(), keepIdx))
                .userAdjustedValues(sliceValues(series.userAdjustedValues(), keepIdx))
                .build())
            .toList();

        return toBuilder()
            .dates(clippedDates)
            .baseSeries(clippedBase)
            .scenarioSeries(clippedScenario)
            .build();
    }

    @Builder
    public record BaseSeries(

        @Schema(description = "시리즈 식별자", example = "MI_BASE")
        String seriesId,

        @Schema(description = "시리즈명", example = "MI Base")
        String seriesName,

        @Schema(description = "값 목록 (dates와 동일 순서)")
        List<BigDecimal> values

    ) {

        private static final String MI_BASE_SERIES_ID = "MI_BASE";
        private static final String MI_BASE_SERIES_NAME = "MI Base";

        static List<BaseSeries> of(List<String> periodKeys, List<MiBasePriceRow> miBasePrices) {

            if (miBasePrices.isEmpty()) {
                // 미적재 -> 기준선 시리즈 생략
                return List.of();
            }

            Map<String, BigDecimal> priceByPeriodKey = miBasePrices.stream()
                .collect(Collectors.toMap(MiBasePriceRow::periodKey, MiBasePriceRow::baseCasePrice, (v1, v2) -> v1));

            List<BigDecimal> series = periodKeys.stream().map(priceByPeriodKey::get).map(ScenarioGroupGraphRes::scalePrice).toList();

            return List.of(
                BaseSeries.builder()
                    .seriesId(MI_BASE_SERIES_ID)
                    .seriesName(MI_BASE_SERIES_NAME)
                    .values(series)
                    .build()
            );
        }

    }

    @Builder(toBuilder = true)
    public record ScenarioSeries(

        @Schema(description = "시나리오 ID", example = "11")
        Long scenarioId,

        @Schema(description = "시나리오명", example = "단기 봉쇄와 제한적 공급 대응")
        String scenarioName,

        @Schema(description = "BC(Base Case) 여부", example = "false")
        boolean baseCase,

        @Schema(description = "표시 순서", example = "1")
        Integer displayOrder,

        @Schema(description = "수급모델 실행 완료 여부 (완료 케이스 존재, 인덱스 무관 — run-status 폴링 API와 동일 기준)", example = "true")
        boolean executed,

        @Schema(description = "수급모델 실행중 여부 (RUNNING 케이스 존재)", example = "false")
        boolean modelRunning,

        @Schema(description = "상태 (pending/realized/discarded)", example = "pending")
        ScenarioDisplayStatus status,

        @Schema(description = "상태 라벨 (대기/실현/폐기)", example = "대기")
        String statusLabel,

        @Schema(description = "베이스케이스 가격 목록 (dates와 동일 순서, 빠진 구간 null). 해당 인덱스 완료 가격이 없으면 null(완료여도 그 인덱스 가격 없으면 null)", nullable = true)
        List<BigDecimal> baseCaseValues,

        @Schema(description = "사용자조정 가격 목록 (dates와 동일 순서, 빠진 구간 null). 해당 인덱스 완료 가격이 없으면 null", nullable = true)
        List<BigDecimal> userAdjustedValues

    ) {

        static List<ScenarioSeries> of(List<ScenarioBaseRow> scenarios, List<String> periodKeys, List<ScenarioPriceRow> scenariosPrices,
                                       Set<Long> runningScenarioIds, Set<Long> completedScenarioIds) {

            Map<Long, Map<String, ScenarioPriceRow>> pricesByScenario = scenariosPrices.stream()
                .collect(Collectors.groupingBy(ScenarioPriceRow::scenarioId,
                    Collectors.toMap(ScenarioPriceRow::periodKey, Function.identity())));

            return scenarios.stream()
                .map(scenario -> toSeries(scenario, periodKeys, pricesByScenario.get(scenario.scenarioId()),
                    runningScenarioIds != null && runningScenarioIds.contains(scenario.scenarioId()),
                    completedScenarioIds != null && completedScenarioIds.contains(scenario.scenarioId())))
                .toList();
        }

        private static ScenarioSeries toSeries(ScenarioBaseRow scenario, List<String> periodKeys, Map<String, ScenarioPriceRow> priceByPeriodKey, boolean modelRunning, boolean executed) {
            boolean hasPrices = priceByPeriodKey != null && !priceByPeriodKey.isEmpty();
            ScenarioDisplayStatus status = ScenarioDisplayStatus.from(scenario.status());
            return ScenarioSeries.builder()
                .scenarioId(scenario.scenarioId())
                .scenarioName(scenario.title())
                .baseCase(Boolean.TRUE.equals(scenario.isBaseline()))
                .displayOrder(scenario.displayOrder())
                .executed(executed)
                .modelRunning(modelRunning)
                .status(status)
                .statusLabel(status == null ? null : status.getLabel())
                .baseCaseValues(hasPrices ? values(periodKeys, priceByPeriodKey, ScenarioPriceRow::baseCasePrice) : null)
                .userAdjustedValues(hasPrices ? values(periodKeys, priceByPeriodKey, ScenarioPriceRow::userAdjustedPrice) : null)
                .build();
        }

        private static List<BigDecimal> values(List<String> periodKeys, Map<String, ScenarioPriceRow> priceByPeriodKey, Function<ScenarioPriceRow, BigDecimal> extractor) {
            return periodKeys.stream()
                .map(periodKey -> {
                    ScenarioPriceRow row = priceByPeriodKey.get(periodKey);
                    return row == null ? null : scalePrice(extractor.apply(row));
                })
                .toList();
        }

        /**
         * 값 시리즈를 sourceDates -> targetDates 순서로 재배열한 새 시리즈 (병합 시 x축 union 정렬용).
         */
        ScenarioSeries realignTo(List<String> targetDates, List<String> sourceDates) {
            return toBuilder()
                .baseCaseValues(realign(targetDates, sourceDates, baseCaseValues))
                .userAdjustedValues(realign(targetDates, sourceDates, userAdjustedValues))
                .build();
        }

    }

}
