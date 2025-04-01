package com.sk.domain.mi.conversion;

import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.conversion.dto.ConversionResult;
import com.sk.domain.mi.conversion.strategy.ConversionStrategy;
import com.sk.domain.mi.conversion.strategy.basic.MovingAverageConversion;
import com.sk.domain.mi.conversion.strategy.basic.StandardDeviationConversion;
import com.sk.domain.mi.conversion.strategy.complex.BollingerBandsConversion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@DisplayName("단위 변환 컨텍스트 테스트 클래스")
@ExtendWith(MockitoExtension.class)
class UnitConversionContextTest {

    @Mock
    private MovingAverageConversion movingAverageConversion;

    @Mock
    private StandardDeviationConversion standardDeviationConversion;

    @Mock
    private BollingerBandsConversion bollingerBandsConversion;

    private UnitConversionContext unitConversionContext;

    @BeforeEach
    void setUp() {
        // 각 모의 객체의 기본 동작 설정
        given(movingAverageConversion.getFormulaService()).willReturn(FormulaService.MOVING_AVERAGE);
        given(standardDeviationConversion.getFormulaService()).willReturn(FormulaService.STANDARD_DEVIATION);
        given(bollingerBandsConversion.getFormulaService()).willReturn(FormulaService.BOLLINGER_BANDS);
        Mockito.lenient().when(bollingerBandsConversion.resultType()).thenReturn(BollingerBandResult.class);
        // 테스트 대상 초기화
        unitConversionContext = new UnitConversionContext(
            Arrays.asList(movingAverageConversion, standardDeviationConversion, bollingerBandsConversion)
        );
        unitConversionContext.init(); // PostConstruct 메서드 수동 호출
    }

    @Nested
    @DisplayName("1. 전략 찾기 테스트")
    class FindStrategyTests {

        @Test
        @DisplayName("1-1. 존재하는 공식 서비스에 대한 전략을 찾아야 한다.")
        void shouldFindStrategyForExistingFormulaService() {
            // [when]
            Optional<ConversionStrategy> strategy = unitConversionContext.findStrategy(FormulaService.MOVING_AVERAGE);

            // [then]
            assertThat(strategy).isPresent();
            assertThat(strategy.get()).isEqualTo(movingAverageConversion);
        }

        @Test
        @DisplayName("1-2. 존재하지 않는 공식 서비스에 대해 빈 Optional을 반환해야 한다.")
        void shouldReturnEmptyOptionalForNonExistingFormulaService() {
            // [given] (가상의 존재하지 않는 FormulaService 정의 필요)
            FormulaService nonExistingService = null;

            // [when & then]
            assertThat(unitConversionContext.findStrategy(nonExistingService)).isEmpty();
        }
    }

    @Nested
    @DisplayName("2. 단일 값 변환 테스트")
    class ConvertSingleValueTests {

        @Test
        @DisplayName("2-1. 단일 값을 성공적으로 변환해야 한다.")
        void shouldConvertSingleValueSuccessfully() {
            // [given]
            BigDecimal inputValue = new BigDecimal("10");
            BigDecimal expectedOutput = new BigDecimal("20");

            given(movingAverageConversion.convert(inputValue)).willReturn(expectedOutput);

            // [when]
            BigDecimal result = unitConversionContext.convert(FormulaService.MOVING_AVERAGE, inputValue);

            // [then]
            assertThat(result).isEqualByComparingTo(expectedOutput);
            then(movingAverageConversion).should().convert(inputValue);
        }

    }

    @Nested
    @DisplayName("3. 시계열 데이터 변환 테스트")
    class ConvertTimeSeriesTests {

        @Test
        @DisplayName("3-1. 시계열 데이터를 성공적으로 변환해야 한다.")
        void shouldConvertTimeSeriesSuccessfully() {
            // [given]
            List<BigDecimal> inputData = Arrays.asList(
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("30")
            );
            int windowSize = 2;

            List<BigDecimal> expectedOutput = Arrays.asList(
                null,
                new BigDecimal("15"),
                new BigDecimal("25")
            );

            given(movingAverageConversion.convertTimeSeries(inputData, windowSize)).willReturn(expectedOutput);

            // [when]
            List<BigDecimal> result = unitConversionContext.convertTimeSeries(FormulaService.MOVING_AVERAGE, inputData, windowSize);

            // [then]
            assertThat(result).isEqualTo(expectedOutput);
            then(movingAverageConversion).should().convertTimeSeries(inputData, windowSize);
        }

    }

    @Nested
    @DisplayName("4. 시계열 데이터 결과 변환 테스트")
    class ConvertTimeSeriesResultTests {

        @Test
        @DisplayName("4-1. 복합 결과 타입을 반환하는 시계열 데이터 변환이 성공해야 한다")
        void shouldConvertTimeSeriesResultSuccessfully() {
            // [given]
            List<BigDecimal> inputData = Arrays.asList(
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("30")
            );
            int windowSize = 2;

            BollingerBandResult result1 = BollingerBandResult.of(
                new BigDecimal("15"),
                new BigDecimal("25"),
                new BigDecimal("5")
            );

            BollingerBandResult result2 = BollingerBandResult.of(
                new BigDecimal("25"),
                new BigDecimal("35"),
                new BigDecimal("15")
            );

            List<BollingerBandResult> expectedOutput = Arrays.asList(
                null,
                result1,
                result2
            );

            given(bollingerBandsConversion.convertTimeSeries(inputData, windowSize, BollingerBandResult.class))
                .willReturn(expectedOutput);

            // [when]
            List<? extends ConversionResult> result = unitConversionContext.convertTimeSeriesResult(FormulaService.BOLLINGER_BANDS, inputData, windowSize);

            // [then]
            assertThat(result).isEqualTo(expectedOutput);
            then(bollingerBandsConversion).should().convertTimeSeries(inputData, windowSize, BollingerBandResult.class);
        }

        @Test
        @DisplayName("4-2. 복합 결과를 지원하지 않는 전략으로 변환 시도 시, 예외가 발생해야 한다.")
        void shouldThrowExceptionWhenConvertingWithNonResultWrapperStrategy() {
            // [given]
            List<BigDecimal> inputData = Arrays.asList(
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("30")
            );
            int windowSize = 2;

            // [when & then]
            assertThatThrownBy(() -> unitConversionContext.convertTimeSeriesResult(FormulaService.MOVING_AVERAGE, inputData, windowSize))
                .isInstanceOf(SystemErrorException.class)
                .hasMessageContaining("The operation violates a business rule.");
        }

    }
}
