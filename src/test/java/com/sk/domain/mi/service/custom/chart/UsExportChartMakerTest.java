package com.sk.domain.mi.service.custom.chart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.conversion.UnitConversionService;
import com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange.CalculatePeriodChangeValue;
import com.sk.domain.mi.dto.response.marketData.charts.graph.MarketDataChartRawData;
import com.sk.domain.mi.service.MiMarketDynamicQueryService;
import com.sk.domain.mi.validator.GoodsDataSourceValidator;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.marketData.CalculatePeriodChangeType;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("UsExportChartMaker 테스트")
class UsExportChartMakerTest {

    @Test
    @DisplayName("빈 raw data는 기존 empty chart 데이터를 유지한다")
    void keepEmptyChartData() {
        assertThat(UsExportChartMaker.convertToDashBoardData(
            List.of(), "date", "value"))
            .isEmpty();
    }

    @Test
    @DisplayName("월 경계와 nullable value의 기존 집계 결과를 유지한다")
    void keepExistingMonthBoundaryAndNullableValue() {
        List<Map<String, Object>> rawData = List.of(
            marketRow("2025-05-01T00:00", "1.25"),
            marketRow("2025-05-02T00:00", null),
            marketRow("2025-06-01T00:00", "3.50"));

        List<MarketDataChartRawData> result = UsExportChartMaker.convertToDashBoardData(
            rawData, "date", "value");

        assertThat(result).containsExactly(
            MarketDataChartRawData.of("2025-05-02", 1.25));
    }

    @Test
    @DisplayName("단일 월의 정상 합계 결과를 유지한다")
    void keepSingleMonthSum() {
        List<MarketDataChartRawData> result = UsExportChartMaker.convertToDashBoardData(
            List.of(
                marketRow("2025-06-01T00:00", "1.25"),
                marketRow("2025-06-02T00:00", "3.50")),
            "date",
            "value");

        assertThat(result).containsExactly(
            MarketDataChartRawData.of("2025-06-02", 4.75));
    }

    @ParameterizedTest(name = "[{index}] requiredDate={0}")
    @NullSource
    @ValueSource(strings = {"", "invalid", "20251301"})
    @DisplayName("null 또는 invalid 월 합산 날짜는 공개 detail 없는 내부 오류로 처리한다")
    void rejectInvalidAggregationDate(String requiredDate) {
        assertThatThrownBy(() -> UsExportChartMaker.convertToDashBoardData(
            List.of(marketRow(requiredDate, "1.0")), "date", "value"))
            .isInstanceOfSatisfying(SystemErrorException.class, exception -> {
                assertThat(exception.getErrorCode())
                    .isEqualTo(ResultCode.Error.SYS_INTERNAL_ERROR);
                assertThat(exception.getAdditionalMessage()).isNull();
            });
    }

    @Test
    @DisplayName("정상 row와 invalid 월 합산 날짜가 섞여도 invalid row를 skip하지 않는다")
    void rejectMixedInvalidAggregationDate() {
        assertThatThrownBy(() -> UsExportChartMaker.convertToDashBoardData(
            List.of(
                marketRow("2025-05-01T00:00", "1.0"),
                marketRow("invalid", "2.0")),
            "date",
            "value"))
            .isInstanceOfSatisfying(SystemErrorException.class,
                exception -> assertThat(exception.getAdditionalMessage()).isNull());
    }

    @Test
    @DisplayName("정상 period 계산 결과를 유지한다")
    void keepNormalPeriodCalculation() {
        UsExportChartMaker chartMaker = chartMaker();
        List<Map<String, Object>> rawData = List.of(
            marketRow("2025-05-15T00:00", "5.0"),
            marketRow("2025-06-15T00:00", "10.0"));

        Map<CalculatePeriodChangeType, CalculatePeriodChangeValue> result =
            chartMaker.calculatePeriodChange(
                mock(GoodsDataSourceDetail.class),
                rawData,
                "date",
                "value",
                MarketDataChartRawData.of("2025-06-15", 10.0));

        assertThat(result).containsExactly(
            Map.entry(
                CalculatePeriodChangeType.MOM,
                CalculatePeriodChangeValue.of(50.0, 5.0)),
            Map.entry(
                CalculatePeriodChangeType.YOY,
                CalculatePeriodChangeValue.of(100.0, 10.0)));
    }

    @ParameterizedTest(name = "[{index}] requiredDate={0}")
    @NullSource
    @ValueSource(strings = {"", "invalid", "2025-06-15"})
    @DisplayName("invalid 마지막 period 날짜는 이전 row fallback 없이 내부 오류로 처리한다")
    void rejectInvalidLastPeriodDate(String requiredDate) {
        UsExportChartMaker chartMaker = chartMaker();
        List<Map<String, Object>> rawData = List.of(
            marketRow("2025-05-15T00:00", "5.0"),
            marketRow(requiredDate, "10.0"));

        assertThatThrownBy(() -> chartMaker.calculatePeriodChange(
            mock(GoodsDataSourceDetail.class),
            rawData,
            "date",
            "value",
            MarketDataChartRawData.of("2025-06-15", 10.0)))
            .isInstanceOfSatisfying(SystemErrorException.class, exception -> {
                assertThat(exception.getErrorCode())
                    .isEqualTo(ResultCode.Error.SYS_INTERNAL_ERROR);
                assertThat(exception.getAdditionalMessage()).isNull();
            });
    }

    @Test
    @DisplayName("중간 period 날짜가 invalid여도 해당 row를 skip하지 않는다")
    void rejectInvalidIntermediatePeriodDate() {
        UsExportChartMaker chartMaker = chartMaker();
        List<Map<String, Object>> rawData = List.of(
            marketRow("invalid", "5.0"),
            marketRow("2025-06-15T00:00", "10.0"));

        assertThatThrownBy(() -> chartMaker.calculatePeriodChange(
            mock(GoodsDataSourceDetail.class),
            rawData,
            "date",
            "value",
            MarketDataChartRawData.of("2025-06-15", 10.0)))
            .isInstanceOfSatisfying(SystemErrorException.class,
                exception -> assertThat(exception.getAdditionalMessage()).isNull());
    }

    private UsExportChartMaker chartMaker() {
        return new UsExportChartMaker(
            mock(GoodsDataSourceValidator.class),
            mock(AuthorizationService.class),
            mock(StringRedisTemplate.class),
            mock(ObjectMapper.class),
            mock(MiMarketDynamicQueryService.class),
            mock(UnitConversionService.class));
    }

    private Map<String, Object> marketRow(Object date, Object value) {
        Map<String, Object> row = new HashMap<>();
        row.put("date", date);
        row.put("value", value);
        return row;
    }
}
