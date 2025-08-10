package com.sk.domain.mi.conversion.strategy.basic;

import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.conversion.FormulaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("표준편차 계산 전략 테스트 클래스")
class StandardDeviationConversionTest {

    private final StandardDeviationConversion standardDeviationConversion = new StandardDeviationConversion();

    @Nested
    @DisplayName("1. 표준편차 계산 정확성 테스트")
    class CalculationTests {

        @Test
        @DisplayName("1-1. 윈도우 사이즈가 3일 때, 표준편차를 올바르게 계산한다.")
        void shouldCalculateCorrectStandardDeviationWithWindowSize3() {

            // [given]
            List<BigDecimal> data = Arrays.asList(
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("30"),
                new BigDecimal("40"),
                new BigDecimal("50")
            );
            int windowSize = 3;

            // [when]
            List<BigDecimal> result = standardDeviationConversion.convertTimeSeries(data, windowSize);

            // [then]
            assertThat(result).hasSize(5);
            assertThat(result.get(0)).isNull();
            assertThat(result.get(1)).isNull();
            assertThat(result.get(2).doubleValue()).isCloseTo(8.16, within(0.01));
            assertThat(result.get(3).doubleValue()).isCloseTo(8.16, within(0.01));
            assertThat(result.get(4).doubleValue()).isCloseTo(8.16, within(0.01));
        }

        @ParameterizedTest
        @MethodSource("provideTimeSeriesDataAndExpectedResults")
        @DisplayName("1-2. 다양한 데이터셋에서 표준편차를 올바르게 계산한다.")
        void shouldCalculateCorrectStandardDeviationForVariousDatasets(List<BigDecimal> data, int windowSize, List<Double> expected) {

            // [when]
            List<BigDecimal> result = standardDeviationConversion.convertTimeSeries(data, windowSize);

            // [then]
            assertThat(result).hasSize(expected.size());

            for (int i = 0; i < expected.size(); i++) {
                if (expected.get(i) == null) {
                    // [then]
                    assertThat(result.get(i)).isNull();
                } else {
                    // [then]
                    assertThat(result.get(i).doubleValue()).isCloseTo(expected.get(i), within(0.01));
                }
            }
        }

        private static Stream<Arguments> provideTimeSeriesDataAndExpectedResults() {
            return Stream.of(
                // 윈도우 크기가 데이터 크기와 같은 경우
                Arguments.of(
                    List.of(new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30")),
                    3,
                    Arrays.asList(null, null, 8.16)
                ),
                // 윈도우 크기가 1인 경우 (표준편차가 0이어야 함)
                Arguments.of(
                    List.of(new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30")),
                    1,
                    Arrays.asList(0.0, 0.0, 0.0)
                ),
                // 소수점이 있는 데이터셋
                Arguments.of(
                    List.of(new BigDecimal("1.5"), new BigDecimal("2.5"), new BigDecimal("3.5"), new BigDecimal("4.5")),
                    2,
                    Arrays.asList(null, 0.5, 0.5, 0.5)
                ),
                // 모든 값이 동일한 경우 (표준편차가 0이어야 함)
                Arguments.of(
                    List.of(new BigDecimal("5"), new BigDecimal("5"), new BigDecimal("5"), new BigDecimal("5")),
                    2,
                    Arrays.asList(null, 0.0, 0.0, 0.0)
                )
            );
        }
    }

    @Nested
    @DisplayName("2. 특수 케이스 및 예외 처리 테스트")
    class EdgeCasesAndExceptionTests {

        @Test
        @DisplayName("2-1. null 데이터셋으로 호출 시, 예외를 발생시킨다.")
        void shouldThrowExceptionForNullDataset() {
            // [when & then]
            assertThatThrownBy(() -> standardDeviationConversion.convertTimeSeries(null, 3))
                .isInstanceOf(SystemErrorException.class)
                .hasMessageContaining("The operation violates a business rule.");
        }

        @Test
        @DisplayName("2-2. 단일 값 변환 시도 시, SystemErrorException 발생해야 한다.")
        void shouldThrowExceptionForSingleValueConversion() {
            // [when & then]
            assertThatThrownBy(() -> standardDeviationConversion.convert(BigDecimal.ONE))
                .isInstanceOf(SystemErrorException.class)
                .hasMessageContaining("The operation violates a business rule.");
        }

        @Test
        @DisplayName("2-3. 빈 데이터셋으로 호출 시, 빈 결과를 반환해야 한다.")
        void shouldReturnEmptyResultForEmptyDataset() {
            // [given]
            List<BigDecimal> emptyData = Collections.emptyList();

            // [when]
            List<BigDecimal> result = standardDeviationConversion.convertTimeSeries(emptyData, 3);

            // [then]
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("2-4. 윈도우 크기보다 작은 데이터셋으로 호출 시, 모든 값이 null이어야 한다.")
        void shouldReturnAllNullsForDatasetSmallerThanWindow() {
            // [given]
            List<BigDecimal> data = List.of(new BigDecimal("10"), new BigDecimal("20"));
            int windowSize = 3;

            // [when]
            List<BigDecimal> result = standardDeviationConversion.convertTimeSeries(data, windowSize);

            // [then]
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(Objects::isNull);
        }
    }

    @Nested
    @DisplayName("3. 전략 패턴 관련 테스트")
    class StrategyPatternTests {

        @Test
        @DisplayName("3-1. 올바른 FormulaService 타입을 반환해야 한다.")
        void shouldReturnCorrectFormulaServiceType() {
            // [when]
            FormulaService formulaService = standardDeviationConversion.getFormulaService();

            // [then]
            assertThat(formulaService).isEqualTo(FormulaService.STANDARD_DEVIATION);
        }

        @Test
        @DisplayName("3-2. 표준편차 공식 서비스를 지원한다.")
        void shouldSupportStandardDeviationFormulaService() {
            // [when & then]
            assertThat(standardDeviationConversion.supports(FormulaService.STANDARD_DEVIATION)).isTrue();
        }

        @Test
        @DisplayName("3-3. 다른 공식 서비스는 지원하지 않는다.")
        void shouldNotSupportOtherFormulaServices() {
            // [when & then]
            assertThat(standardDeviationConversion.supports(FormulaService.MOVING_AVERAGE)).isFalse();
            assertThat(standardDeviationConversion.supports(FormulaService.BOLLINGER_BANDS)).isFalse();
        }
    }

    @Nested
    @DisplayName("4. 성능 테스트")
    class PerformanceTests {

        @Test
        @DisplayName("4-1. 대규모 데이터셋에서 1초 이내에 계산을 완료한다.")
        void shouldCalculateForLargeDatasetInReasonableTime() {
            // [given]
            int dataSize = 10_000;
            List<BigDecimal> largeData = Stream
                .iterate(BigDecimal.ONE, n -> n.add(BigDecimal.ONE))
                .limit(dataSize)
                .collect(Collectors.toList());
            int windowSize = 100;

            // [when]
            long startTime = System.currentTimeMillis();
            List<BigDecimal> result = standardDeviationConversion.convertTimeSeries(largeData, windowSize);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // [then]
            assertThat(result).hasSize(dataSize);
            assertThat(duration).isLessThan(1000); // 1초 이내

            BigDecimal lastValue = result.get(dataSize - 1);
            double expectedLastValue = Math.sqrt((Math.pow(windowSize, 2) - 1) / 12.0);

            assertThat(lastValue.doubleValue()).isCloseTo(expectedLastValue, within(0.1));
        }
    }

}
