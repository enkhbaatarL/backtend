package com.sk.domain.mi.service.period;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange.CalculatePeriodChangeValue;
import com.sk.domain.shared.entity.master.tif.GieEu;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.repository.master.tif.GieEuRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("PeriodCalculator 테스트")
class PeriodCalculatorTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("calculators")
    @DisplayName(
        "raw data가 null 또는 empty이면 기존처럼 period key 생략용 null을 반환한다")
    void keepNullForMissingRawData(String name, PeriodCalculator calculator) {
        assertThat(calculator.calculatePeriodChange(
            gieDataSource(), null, "date", "value"))
            .isNull();
        assertThat(calculator.calculatePeriodChange(
            gieDataSource(), List.of(), "date", "value"))
            .isNull();
    }

    @ParameterizedTest(name = "[{index}] {0}, requiredDate={2}")
    @MethodSource("calculatorsAndInvalidDates")
    @DisplayName(
        "non-empty 마지막 raw row 날짜가 invalid이면 공개 detail 없는 내부 오류로 처리한다")
    void rejectInvalidLastRowDate(
        String name,
        PeriodCalculator calculator,
        Object requiredDate) {
        Map<String, Object> invalidLastRow = marketRow(requiredDate, 10.0);

        assertThatThrownBy(() -> calculator.calculatePeriodChange(
            gieDataSource(), List.of(invalidLastRow), "date", "value"))
            .isInstanceOfSatisfying(SystemErrorException.class, exception -> {
                assertThat(exception.getErrorCode())
                    .isEqualTo(ResultCode.Error.SYS_INTERNAL_ERROR);
                assertThat(exception.getAdditionalMessage()).isNull();
            });
    }

    @Test
    @DisplayName("DoD 정상 계산식과 값 부족 시 ofEmpty 결과를 유지한다")
    void keepDoDResults() {
        DoDPeriodCalculator calculator = new DoDPeriodCalculator();

        assertThat(calculator.calculatePeriodChange(
            null,
            List.of(marketRow("20250730", 10.0), marketRow("20250731", 20.0)),
            "date",
            "value"))
            .isEqualTo(CalculatePeriodChangeValue.of(100.0, 10.0));
        assertThat(calculator.calculatePeriodChange(
            null,
            List.of(marketRow("20250731", null)),
            "date",
            "value"))
            .isEqualTo(CalculatePeriodChangeValue.ofEmpty());
    }

    @Test
    @DisplayName("MoM 정상 계산식을 유지한다")
    void keepMoMResult() {
        MoMPeriodCalculator calculator = new MoMPeriodCalculator();

        assertThat(calculator.calculatePeriodChange(
            null,
            List.of(marketRow("20250615", 10.0), marketRow("20250715", 20.0)),
            "date",
            "value"))
            .isEqualTo(CalculatePeriodChangeValue.of(100.0, 10.0));
    }

    @Test
    @DisplayName("WoW 정상 계산식을 유지한다")
    void keepWoWResult() {
        WoWPeriodCalculator calculator = new WoWPeriodCalculator();

        assertThat(calculator.calculatePeriodChange(
            null,
            List.of(marketRow("20250720", 10.0), marketRow("20250727", 20.0)),
            "date",
            "value"))
            .isEqualTo(CalculatePeriodChangeValue.of(100.0, 10.0));
    }

    @Test
    @DisplayName("YoY 정상 계산식을 유지한다")
    void keepYoYResult() {
        YoYPeriodCalculator calculator = new YoYPeriodCalculator();

        assertThat(calculator.calculatePeriodChange(
            null,
            List.of(marketRow("20240615", 10.0), marketRow("20250615", 20.0)),
            "date",
            "value"))
            .isEqualTo(CalculatePeriodChangeValue.of(100.0, 10.0));
    }

    @Test
    @DisplayName("FiveYearAvg 정상 계산식과 nullable current value 결과를 유지한다")
    void keepFiveYearAvgResults() {
        GieEuRepository repository = mock(GieEuRepository.class);
        FiveYearAvgPeriodCalculator calculator =
            new FiveYearAvgPeriodCalculator(repository);
        LocalDate today = LocalDate.of(2025, 7, 31);
        given(repository.findByCodeAndDate(
            GieEu.GIE_EU_CODE_PAST_5Y_AVG, today.atStartOfDay()))
            .willReturn(Optional.of(GieEu.builder()
                .gasInStorageMt(BigDecimal.TEN)
                .build()));

        assertThat(calculator.calculatePeriodChange(
            gieDataSource(),
            List.of(marketRow("20250731", 20.0)),
            "date",
            "value"))
            .isEqualTo(CalculatePeriodChangeValue.of(100.0, 10.0));
        assertThat(calculator.calculatePeriodChange(
            gieDataSource(),
            List.of(marketRow("20250731", null)),
            "date",
            "value"))
            .isNull();
    }

    @Test
    @DisplayName(
        "FiveYearAvg 비대상 datasource는 마지막 날짜가 invalid여도 기존 null을 유지한다")
    void keepNullForUnsupportedFiveYearAvgDataSource() {
        FiveYearAvgPeriodCalculator calculator =
            new FiveYearAvgPeriodCalculator(mock(GieEuRepository.class));
        GoodsDataSourceDetail unsupportedDataSource = GoodsDataSourceDetail.builder()
            .targetTableNm("other_table")
            .build();

        assertThat(calculator.calculatePeriodChange(
            unsupportedDataSource,
            List.of(marketRow("invalid", 20.0)),
            "date",
            "value"))
            .isNull();
    }

    private static Stream<Arguments> calculators() {
        return allCalculators().stream()
            .map(testCase -> Arguments.of(testCase.name(), testCase.calculator()));
    }

    private static Stream<Arguments> calculatorsAndInvalidDates() {
        List<Object> invalidDates = java.util.Arrays.asList(
            null,
            "",
            "invalid",
            "20251301");
        return allCalculators().stream()
            .flatMap(testCase -> invalidDates.stream()
                .map(invalidDate -> Arguments.of(
                    testCase.name(), testCase.calculator(), invalidDate)));
    }

    private static List<CalculatorCase> allCalculators() {
        return List.of(
            new CalculatorCase("DoD", new DoDPeriodCalculator()),
            new CalculatorCase("MoM", new MoMPeriodCalculator()),
            new CalculatorCase("WoW", new WoWPeriodCalculator()),
            new CalculatorCase("YoY", new YoYPeriodCalculator()),
            new CalculatorCase(
                "FiveYearAvg",
                new FiveYearAvgPeriodCalculator(mock(GieEuRepository.class))));
    }

    private static GoodsDataSourceDetail gieDataSource() {
        return GoodsDataSourceDetail.builder()
            .targetTableNm(GieEu.GIE_EU_TABLE_NAME)
            .build();
    }

    private static Map<String, Object> marketRow(Object date, Object value) {
        Map<String, Object> row = new HashMap<>();
        row.put("date", date);
        row.put("value", value);
        return row;
    }

    private record CalculatorCase(String name, PeriodCalculator calculator) {
    }
}
