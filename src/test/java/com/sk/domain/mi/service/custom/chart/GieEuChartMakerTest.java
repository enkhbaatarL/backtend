package com.sk.domain.mi.service.custom.chart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.dto.response.marketData.charts.graph.MarketDataChartRawData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("GieEuChartMaker 테스트")
class GieEuChartMakerTest {

    @Test
    @DisplayName("빈 raw data는 기존 empty chart 데이터를 유지한다")
    void keepEmptyChartData() {
        assertThat(GieEuChartMaker.convertToDashBoardData(
            List.of(),
            "date",
            "value",
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31)))
            .isEmpty();
    }

    @Test
    @DisplayName("정상 날짜와 nullable value 및 기간 필터 결과를 유지한다")
    void keepNormalAndNullableChartData() {
        List<Map<String, Object>> rawData = List.of(
            marketRow(LocalDateTime.of(2025, 6, 1, 0, 0), "1.25"),
            marketRow("20250602", null),
            marketRow("20240601", "99"));

        List<MarketDataChartRawData> result = GieEuChartMaker.convertToDashBoardData(
            rawData,
            "date",
            "value",
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31));

        assertThat(result).containsExactly(
            MarketDataChartRawData.of("2025-06-01", 1.25),
            MarketDataChartRawData.of("2025-06-02", null));
    }

    @ParameterizedTest(name = "[{index}] requiredDate={0}")
    @NullSource
    @ValueSource(strings = {"", "invalid", "20251301"})
    @DisplayName("null 또는 invalid 날짜는 공개 detail 없는 내부 오류로 처리한다")
    void rejectInvalidRequiredDate(String requiredDate) {
        Map<String, Object> invalidRow = marketRow(requiredDate, "1.0");

        assertThatThrownBy(() -> GieEuChartMaker.convertToDashBoardData(
            List.of(invalidRow),
            "date",
            "value",
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31)))
            .isInstanceOfSatisfying(SystemErrorException.class, exception -> {
                assertThat(exception.getErrorCode())
                    .isEqualTo(ResultCode.Error.SYS_INTERNAL_ERROR);
                assertThat(exception.getAdditionalMessage()).isNull();
            });
    }

    @Test
    @DisplayName("정상 row와 invalid 날짜가 섞여도 invalid row를 skip하지 않는다")
    void rejectMixedInvalidRequiredDate() {
        assertThatThrownBy(() -> GieEuChartMaker.convertToDashBoardData(
            List.of(marketRow("20250601", "1.0"), marketRow("invalid", "2.0")),
            "date",
            "value",
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31)))
            .isInstanceOfSatisfying(SystemErrorException.class,
                exception -> assertThat(exception.getAdditionalMessage()).isNull());
    }

    @Test
    @DisplayName("기간 밖 invalid number도 기존처럼 실패한다")
    void rejectOutOfRangeInvalidNumberBeforeFilter() {
        assertThatThrownBy(() -> GieEuChartMaker.convertToDashBoardData(
            List.of(marketRow("20250601", "1.0"), marketRow("20240601", "invalid")),
            "date",
            "value",
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31)))
            .isInstanceOf(NumberFormatException.class);
    }

    private Map<String, Object> marketRow(Object date, Object value) {
        Map<String, Object> row = new HashMap<>();
        row.put("date", date);
        row.put("value", value);
        return row;
    }
}
