package com.sk.domain.mi.service.period;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange.CalculatePeriodChangeValue;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.marketData.CalculatePeriodChangeType;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AbstractPeriodCalculator 테스트")
class AbstractPeriodCalculatorTest {

    private final TestPeriodCalculator calculator = new TestPeriodCalculator();

    @Test
    @DisplayName("정상 current/previous 평균의 기존 변화율과 변화값을 유지한다")
    void keepNormalCalculation() {
        CalculatePeriodChangeValue result = calculate(List.of(
            marketRow("20250515", 10.0),
            marketRow("20250615", 20.0)));

        assertThat(result).isEqualTo(CalculatePeriodChangeValue.of(100.0, 10.0));
    }

    @Test
    @DisplayName("빈 raw data는 기존 ofEmpty 결과를 유지한다")
    void keepEmptyRawDataResult() {
        assertThat(calculate(List.of()))
            .isEqualTo(CalculatePeriodChangeValue.ofEmpty());
    }

    @Test
    @DisplayName("비교 기간 값이 부족하면 기존 ofEmpty 결과를 유지한다")
    void keepInsufficientValueResult() {
        assertThat(calculate(List.of(marketRow("20250615", 20.0))))
            .isEqualTo(CalculatePeriodChangeValue.ofEmpty());
    }

    @Test
    @DisplayName("null 또는 숫자로 변환할 수 없는 value는 날짜 검증 전에 기존처럼 건너뛴다")
    void keepNullableAndInvalidNumberSkip() {
        CalculatePeriodChangeValue result = calculate(List.of(
            marketRow("invalid-null-value-date", null),
            marketRow("invalid-number-date", "not-number"),
            marketRow("20250515", 10.0),
            marketRow("20250615", 20.0)));

        assertThat(result).isEqualTo(CalculatePeriodChangeValue.of(100.0, 10.0));
    }

    @ParameterizedTest(name = "[{index}] requiredDate={0}")
    @NullSource
    @ValueSource(strings = {"", "invalid", "20251301"})
    @DisplayName("계산 참여 row의 invalid 날짜는 공개 detail 없는 내부 오류로 처리한다")
    void rejectInvalidRequiredDate(String requiredDate) {
        assertThatThrownBy(() -> calculate(List.of(marketRow(requiredDate, 10.0))))
            .isInstanceOfSatisfying(SystemErrorException.class, exception -> {
                assertThat(exception.getErrorCode())
                    .isEqualTo(ResultCode.Error.SYS_INTERNAL_ERROR);
                assertThat(exception.getAdditionalMessage()).isNull();
            });
    }

    @Test
    @DisplayName("정상 row와 invalid 날짜가 섞여도 계산 참여 row를 skip하지 않는다")
    void rejectMixedInvalidRequiredDate() {
        assertThatThrownBy(() -> calculate(List.of(
            marketRow("20250515", 10.0),
            marketRow("invalid", 20.0))))
            .isInstanceOfSatisfying(SystemErrorException.class,
                exception -> assertThat(exception.getAdditionalMessage()).isNull());
    }

    private CalculatePeriodChangeValue calculate(List<Map<String, Object>> rawData) {
        return calculator.calculatePeriodChange(null, rawData, "date", "value");
    }

    private Map<String, Object> marketRow(Object date, Object value) {
        Map<String, Object> row = new HashMap<>();
        row.put("date", date);
        row.put("value", value);
        return row;
    }

    private static class TestPeriodCalculator extends AbstractPeriodCalculator {

        @Override
        public CalculatePeriodChangeType getCalculatePeriodChangeType() {
            return CalculatePeriodChangeType.MOM;
        }

        @Override
        public CalculatePeriodChangeValue calculatePeriodChange(
            GoodsDataSourceDetail mainGoodsDataSource,
            List<Map<String, Object>> rawData,
            String dateColumnName,
            String defaultDataColumnName) {
            return calculatePeriodChangeValue(
                rawData,
                dateColumnName,
                defaultDataColumnName,
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 6, 30),
                LocalDate.of(2025, 5, 1),
                LocalDate.of(2025, 5, 31));
        }
    }
}
