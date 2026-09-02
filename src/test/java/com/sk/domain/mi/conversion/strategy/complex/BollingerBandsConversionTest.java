package com.sk.domain.mi.conversion.strategy.complex;

import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.conversion.FormulaService;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.conversion.strategy.basic.FixedStandardDeviationConversion;
import com.sk.domain.mi.conversion.strategy.basic.MovingAverageConversion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@DisplayName("볼린저 밴드 전략 테스트 클래스")
@ExtendWith(MockitoExtension.class)
class BollingerBandsConversionTest {

    @Mock
    private MovingAverageConversion movingAverageConversion;

    @Mock
    private FixedStandardDeviationConversion standardDeviationConversion;

    @InjectMocks
    private BollingerBandsConversion bollingerBandsConversion;

    @Nested
    @DisplayName("1. 볼린저 밴드 계산 정확성 테스트")
    class CalculationTests {

        @Test
        @DisplayName("1.1 모킹된 의존성으로 볼린저 밴드가 올바르게 계산되어야 한다.")
        void shouldCalculateBollingerBandsWithMockedDependencies() {
            // [given]
            List<BigDecimal> data = Arrays.asList(
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("30"),
                new BigDecimal("40"),
                new BigDecimal("50")
            );
            int windowSize = 3;

            // 이동평균 모킹
            List<BigDecimal> maResults = Arrays.asList(
                null,
                null,
                new BigDecimal("20"),
                new BigDecimal("30"),
                new BigDecimal("40")
            );
            given(movingAverageConversion.convertTimeSeries(data, windowSize)).willReturn(maResults);

            // 표준편차 모킹
            List<BigDecimal> sdResults = Arrays.asList(
                null, null,
                new BigDecimal("8.16"),
                new BigDecimal("8.16"),
                new BigDecimal("8.16")
            );
            given(standardDeviationConversion.convertTimeSeries(data, windowSize)).willReturn(sdResults);

            // [when]
            List<BollingerBandResult> results = bollingerBandsConversion.convertTimeSeries(data, windowSize, BollingerBandResult.class);

            // [then]
            assertThat(results).hasSize(5);

            assertThat(results.get(0)).isEqualTo(BollingerBandResult.ofEmpty());
            assertThat(results.get(1)).isEqualTo(BollingerBandResult.ofEmpty());

            BollingerBandResult result2 = results.get(2);

            assertThat(result2).isNotNull();
            assertThat(result2.upper()).isEqualByComparingTo(new BigDecimal("36.32"));
            assertThat(result2.middle()).isEqualByComparingTo(new BigDecimal("20"));
            assertThat(result2.lower()).isEqualByComparingTo(new BigDecimal("3.68"));

            BollingerBandResult result3 = results.get(3);

            assertThat(result3).isNotNull();
            assertThat(result3.upper()).isEqualByComparingTo(new BigDecimal("46.32"));
            assertThat(result3.middle()).isEqualByComparingTo(new BigDecimal("30"));
            assertThat(result3.lower()).isEqualByComparingTo(new BigDecimal("13.68"));
        }
    }

    @Nested
    @DisplayName("2. 볼린저 밴드 계산 정확성 테스트 (실제 의존성 사용)")
    class IntegrationTests {

        @BeforeEach
        void setUpRealDependencies() {
            // 실제 의존성 사용
            movingAverageConversion = new MovingAverageConversion();
            standardDeviationConversion = new FixedStandardDeviationConversion();
            bollingerBandsConversion = new BollingerBandsConversion(movingAverageConversion, standardDeviationConversion);
        }

        @Test
        @DisplayName("2.1 실제 의존성으로 볼린저 밴드가 올바르게 계산되어야 한다.")
        void shouldCalculateBollingerBandsWithRealDependencies() {
            // [given]
            List<BigDecimal> data = Arrays.asList(
                new BigDecimal("100"),
                new BigDecimal("105"),
                new BigDecimal("110"),
                new BigDecimal("115"),
                new BigDecimal("120")
            );
            int windowSize = 3;

            // [when]
            List<BollingerBandResult> results = bollingerBandsConversion.convertTimeSeries(data, windowSize, BollingerBandResult.class);

            // [then]
            assertThat(results).hasSize(5);

            assertThat(results.get(0)).isEqualTo(BollingerBandResult.ofEmpty());
            assertThat(results.get(1)).isEqualTo(BollingerBandResult.ofEmpty());

            for (int i = windowSize - 1; i < results.size(); i++) {
                BollingerBandResult result = results.get(i);

                assertThat(result).isNotNull();

                // 기본 검증: middle < upper, middle > lower
                assertThat(result.middle().compareTo(result.upper())).isLessThan(0);
                assertThat(result.middle().compareTo(result.lower())).isGreaterThan(0);

                // 볼린저 밴드 속성 검증: upper - middle = middle - lower
                BigDecimal upperDiff = result.upper().subtract(result.middle());
                BigDecimal lowerDiff = result.middle().subtract(result.lower());

                assertThat(upperDiff.subtract(lowerDiff).abs().doubleValue()).isLessThan(0.01);
            }
        }

        @Test
        @DisplayName("2.2 시계열이 증가하는 데이터에 대한 볼린저 밴드 결과를 검증한다.")
        void shouldCalculateCorrectBollingerBandsForIncreasingData() {
            // [given]
            List<BigDecimal> data = Stream.iterate(new BigDecimal("100"), n -> n.add(new BigDecimal("10")))
                .limit(10)
                .toList();
            int windowSize = 5;

            // [when]
            List<BollingerBandResult> results = bollingerBandsConversion.convertTimeSeries(data, windowSize, BollingerBandResult.class);

            // [then]
            for (int i = windowSize - 1; i < results.size(); i++) {
                BollingerBandResult result = results.get(i);
                assertThat(result).isNotNull();

                // 증가하는 데이터에서는 볼린저 밴드의 middle 값도 증가
                if (i > windowSize - 1) {
                    BollingerBandResult prevResult = results.get(i - 1);
                    assertThat(result.middle().compareTo(prevResult.middle())).isGreaterThan(0);
                }
            }
        }
    }

    @Nested
    @DisplayName("3. 특수 케이스 및 예외 처리 테스트")
    class EdgeCasesAndExceptionTests {

        @Test
        @DisplayName("3-1. null 데이터셋으로 호출 시, SystemErrorException 발생해야 한다.")
        void shouldThrowExceptionForNullDataset() {
            // [when & then]
            assertThatThrownBy(() -> bollingerBandsConversion.convertTimeSeries(null, 3, BollingerBandResult.class))
                .isInstanceOf(SystemErrorException.class)
                .hasMessageContaining("The operation violates a business rule.");
        }

        @Test
        @DisplayName("3-2. 단일 값 변환 시도 시, SystemErrorException 발생해야 한다.")
        void shouldThrowExceptionForSingleValueConversion() {
            // [when & then]
            assertThatThrownBy(() -> bollingerBandsConversion.convert(BigDecimal.ONE))
                .isInstanceOf(SystemErrorException.class)
                .hasMessageContaining("The operation violates a business rule.");
        }

        @Test
        @DisplayName("3-3. 잘못된 결과 타입으로 호출 시, SystemErrorException 발생해야 한다")
        @SuppressWarnings("unchecked")
        void shouldThrowExceptionForInvalidResultType() {
            // [given]
            List<BigDecimal> data = Arrays.asList(new BigDecimal("10"), new BigDecimal("20"));

            // [when & then]
            assertThatThrownBy(() -> bollingerBandsConversion.convertTimeSeries(data, 2, (Class)String.class))
                .isInstanceOf(SystemErrorException.class)
                .hasMessageContaining("The operation violates a business rule.");
        }

        @Test
        @DisplayName("3-4. 빈 데이터셋으로 호출 시, 빈 결과를 반환해야 한다.")
        void shouldReturnEmptyResultForEmptyDataset() {
            // [given]
            List<BigDecimal> emptyData = Collections.emptyList();
            given(movingAverageConversion.convertTimeSeries(emptyData, 3)).willReturn(Collections.emptyList());
            given(standardDeviationConversion.convertTimeSeries(emptyData, 3)).willReturn(Collections.emptyList());

            // [when]
            List<BollingerBandResult> result = bollingerBandsConversion.convertTimeSeries(emptyData, 3, BollingerBandResult.class);

            // [then]
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("3-5. 이동평균이나 표준편차가 계산되지 않은 경우, 빈 결과를 반환해야 한다.")
        void shouldReturnNullWhenMAOrSDIsNull() {
            // [given]
            List<BigDecimal> data = Arrays.asList(
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("30")
            );
            int windowSize = 3;

            // 이동평균은 정상이지만 표준편차가 null인 경우 모킹
            List<BigDecimal> maResults = Arrays.asList(null, null, new BigDecimal("20"));
            given(movingAverageConversion.convertTimeSeries(data, windowSize)).willReturn(maResults);

            List<BigDecimal> sdResults = Arrays.asList(null, null, null); // 모든 표준편차가 null
            given(standardDeviationConversion.convertTimeSeries(data, windowSize)).willReturn(sdResults);

            // [when]
            List<BollingerBandResult> results = bollingerBandsConversion.convertTimeSeries(data, windowSize, BollingerBandResult.class);

            // [then]
            assertThat(results).hasSize(3);
            assertThat(results.get(0)).isEqualTo(BollingerBandResult.ofEmpty());
            assertThat(results.get(1)).isEqualTo(BollingerBandResult.ofEmpty());
            assertThat(results.get(2)).isEqualTo(BollingerBandResult.ofEmpty()); // MA는 있지만 SD가 null이므로 결과도 빈 값
        }
    }

    @Nested
    @DisplayName("4. 전략 패턴 관련 테스트")
    class StrategyPatternTests {

        @Test
        @DisplayName("4-1. 올바른 FormulaService 타입을 반환해야 한다.")
        void shouldReturnCorrectFormulaServiceType() {
            // [when]
            FormulaService formulaService = bollingerBandsConversion.getFormulaService();

            // [then]
            assertThat(formulaService).isEqualTo(FormulaService.BOLLINGER_BANDS);
        }

        @Test
        @DisplayName("4-2. 볼린저 밴드 공식 서비스를 지원한다.")
        void shouldSupportBollingerBandsFormulaService() {
            // [when & then]
            assertThat(bollingerBandsConversion.supports(FormulaService.BOLLINGER_BANDS)).isTrue();
        }

        @Test
        @DisplayName("4-3. 다른 공식 서비스는 지원하지 않는다.")
        void shouldNotSupportOtherFormulaServices() {
            // [when & then]
            assertThat(bollingerBandsConversion.supports(FormulaService.MOVING_AVERAGE)).isFalse();
            assertThat(bollingerBandsConversion.supports(FormulaService.STANDARD_DEVIATION)).isFalse();
        }

        @Test
        @DisplayName("4-4. 정확한 결과 타입을 반환해야 한다.")
        void shouldReturnCorrectResultType() {
            // [when]
            Class<?> resultType = bollingerBandsConversion.resultType();

            // [then]
            assertThat(resultType).isEqualTo(BollingerBandResult.class);
        }
    }

    @Nested
    @DisplayName("5. 성능 테스트")
    class PerformanceTests {

        @BeforeEach
        void setUpRealDependencies() {
            // 성능 테스트는 실제 구현체를 사용
            movingAverageConversion = new MovingAverageConversion();
            standardDeviationConversion = new FixedStandardDeviationConversion();
            bollingerBandsConversion = new BollingerBandsConversion(movingAverageConversion, standardDeviationConversion);
        }

        @Test
        @DisplayName("5-1. 대규모 데이터셋에서 1초 이내에 계산을 완료한다.")
        void shouldCalculateForLargeDatasetInReasonableTime() {
            // [given]
            int dataSize = 10_000;
            List<BigDecimal> largeData = Stream
                .iterate(BigDecimal.ONE, n -> n.add(BigDecimal.ONE))
                .limit(dataSize)
                .collect(Collectors.toList());
            int windowSize = 20;

            // [when]
            long startTime = System.currentTimeMillis();
            List<BollingerBandResult> results = bollingerBandsConversion.convertTimeSeries(largeData, windowSize, BollingerBandResult.class);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // [then]
            assertThat(results).hasSize(dataSize);
            assertThat(duration).isLessThan(1000); // 1초 이내

            // 윈도우 크기 이후의 결과는 null이 아니어야 함
            for (int i = windowSize - 1; i < results.size(); i++) {
                assertThat(results.get(i)).isNotNull();
            }
        }
    }
}
