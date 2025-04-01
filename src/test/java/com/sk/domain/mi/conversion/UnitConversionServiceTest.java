package com.sk.domain.mi.conversion;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.conversion.dto.ConversionResult;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.repository.master.tmd.UnitConvertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@DisplayName("단위 변환 서비스 테스트 클래스")
@ExtendWith(MockitoExtension.class)
class UnitConversionServiceTest {

    @Mock
    private UnitConversionContext unitConversionContext;

    @Mock
    private UnitConvertRepository unitConvertRepository;

    private UnitConversionService unitConversionService;

    @BeforeEach
    void setUp() {
        unitConversionService = new UnitConversionService(unitConversionContext, unitConvertRepository);
    }

    @Nested
    @DisplayName("1. 단일 값 변환 테스트")
    class ConvertSingleValueTests {

        @Test
        @DisplayName("1-1. 공식 서비스를 사용하여 단일 값을 성공적으로 변환해야 한다.")
        void shouldConvertSingleValueSuccessfullyWithFormulaService() {
            // [given]
            Integer unitConversionNo = 1;
            BigDecimal inputValue = new BigDecimal("10");
            BigDecimal expectedOutput = new BigDecimal("20");

            UnitConvert unitConvert = UnitConvert.builder()
                .calculateFormulaServiceNm("MOVING_AVERAGE")
                .isDeleted(Boolean.FALSE)
                .build();

            given(unitConvertRepository.findByUnitConvNoAndIsDeletedFalse(unitConversionNo)).willReturn(Optional.of(unitConvert));

            FormulaService formulaService = FormulaService.MOVING_AVERAGE;
            given(unitConversionContext.convert(formulaService, inputValue)).willReturn(expectedOutput);

            // [when]
            BigDecimal result = unitConversionService.convert(unitConversionNo, inputValue);

            // [then]
            assertThat(result).isEqualByComparingTo(expectedOutput);
            then(unitConversionContext).should().convert(formulaService, inputValue);
        }

        @Test
        @DisplayName("1-2. 변환 계수를 사용하여 단일 값을 성공적으로 변환해야 한다.")
        void shouldConvertSingleValueSuccessfullyWithCoefficient() {
            // [given]
            Integer unitConversionNo = 1;
            BigDecimal inputValue = new BigDecimal("10");
            BigDecimal coefficient = new BigDecimal("2");
            BigDecimal expectedOutput = new BigDecimal("20");

            UnitConvert unitConvert = UnitConvert.builder()
                .calculateFormulaServiceNm("")
                .convertCoef(coefficient)
                .isDeleted(Boolean.FALSE)
                .build();

            given(unitConvertRepository.findByUnitConvNoAndIsDeletedFalse(unitConversionNo))
                .willReturn(Optional.of(unitConvert));

            // [when]
            BigDecimal result = unitConversionService.convert(unitConversionNo, inputValue);

            // [then]
            assertThat(result).isEqualByComparingTo(expectedOutput);
        }

        @Test
        @DisplayName("1-3. 단위 변환이 존재하지 않을 때 예외가 발생해야 한다.")
        void shouldThrowExceptionWhenUnitConversionNotFound() {
            // [given]
            Integer unitConversionNo = 99; // 존재하지 않는 번호
            BigDecimal inputValue = new BigDecimal("10");

            given(unitConvertRepository.findByUnitConvNoAndIsDeletedFalse(unitConversionNo))
                .willReturn(Optional.empty());

            // [when & then]
            assertThatThrownBy(() -> unitConversionService.convert(unitConversionNo, inputValue))
                .isInstanceOf(AppErrorException.class)
                .matches(e -> ((AppErrorException) e).getErrorCode() == ResultCode.Error.RSC_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("2. 시계열 데이터 변환 테스트")
    class ConvertTimeSeriesTests {

        @Test
        @DisplayName("2-1. 공식 서비스를 사용하여 시계열 데이터를 성공적으로 변환해야 한다.")
        void shouldConvertTimeSeriesSuccessfullyWithFormulaService() {
            // [given]
            Integer unitConversionNo = 1;
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

            UnitConvert unitConvert = UnitConvert.builder()
                .calculateFormulaServiceNm("MOVING_AVERAGE")
                .isDeleted(Boolean.FALSE)
                .build();

            given(unitConvertRepository.findByUnitConvNoAndIsDeletedFalse(unitConversionNo))
                .willReturn(Optional.of(unitConvert));

            FormulaService formulaService = FormulaService.MOVING_AVERAGE;
            given(unitConversionContext.convertTimeSeries(formulaService, inputData, windowSize))
                .willReturn(expectedOutput);

            // [when]
            List<BigDecimal> result = unitConversionService.convertTimeSeries(unitConversionNo, inputData, windowSize);

            // [then]
            assertThat(result).isEqualTo(expectedOutput);
            then(unitConversionContext).should().convertTimeSeries(formulaService, inputData, windowSize);
        }

        @Test
        @DisplayName("2-2. 변환 계수를 사용하여 시계열 데이터를 성공적으로 변환해야 한다.")
        void shouldConvertTimeSeriesSuccessfullyWithCoefficient() {
            // [given]
            Integer unitConversionNo = 1;
            List<BigDecimal> inputData = Arrays.asList(
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("30")
            );
            int windowSize = 2;
            BigDecimal coefficient = new BigDecimal("2");

            List<BigDecimal> expectedOutput = Arrays.asList(
                new BigDecimal("20"),
                new BigDecimal("40"),
                new BigDecimal("60")
            );

            UnitConvert unitConvert = UnitConvert.builder()
                .calculateFormulaServiceNm("")
                .convertCoef(coefficient)
                .isDeleted(Boolean.FALSE)
                .build();

            given(unitConvertRepository.findByUnitConvNoAndIsDeletedFalse(unitConversionNo))
                .willReturn(Optional.of(unitConvert));

            // [when]
            List<BigDecimal> result = unitConversionService.convertTimeSeries(unitConversionNo, inputData, windowSize);

            // [then]
            assertThat(result).isEqualTo(expectedOutput);
        }

        @Test
        @DisplayName("2-3. 단위 변환이 존재하지 않을 때 예외가 발생해야 한다.")
        void shouldThrowExceptionWhenUnitConversionNotFound() {
            // [given]
            Integer unitConversionNo = 99; // 존재하지 않는 번호
            List<BigDecimal> inputData = Arrays.asList(
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("30")
            );
            int windowSize = 2;

            given(unitConvertRepository.findByUnitConvNoAndIsDeletedFalse(unitConversionNo))
                .willReturn(Optional.empty());

            // [when & then]
            assertThatThrownBy(() -> unitConversionService.convertTimeSeries(unitConversionNo, inputData, windowSize))
                .isInstanceOf(AppErrorException.class)
                .matches(e -> ((AppErrorException) e).getErrorCode() == ResultCode.Error.RSC_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("3. 시계열 데이터 결과 변환 테스트")
    class ConvertTimeSeriesResultTests {

        @Test
        @DisplayName("3-1. 복합 결과 타입을 반환하는 시계열 데이터 변환이 성공해야 한다.")
        void shouldConvertTimeSeriesResultSuccessfully() {
            // [given]
            Integer unitConversionNo = 1;
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

            List<ConversionResult> expectedOutput = Arrays.asList(
                null,
                result1,
                result2
            );

            UnitConvert unitConvert = UnitConvert.builder()
                .calculateFormulaServiceNm("BOLLINGER_BANDS")
                .isDeleted(Boolean.FALSE)
                .build();

            given(unitConvertRepository.findByUnitConvNoAndIsDeletedFalse(unitConversionNo))
                .willReturn(Optional.of(unitConvert));

            FormulaService formulaService = FormulaService.BOLLINGER_BANDS;
            given(unitConversionContext.convertTimeSeriesResult(formulaService, inputData, windowSize))
                .willReturn(expectedOutput);

            // [when]
            List<? extends ConversionResult> result = unitConversionService.convertTimeSeriesResult(unitConversionNo, inputData, windowSize);

            // [then]
            assertThat(result).isEqualTo(expectedOutput);
            then(unitConversionContext).should().convertTimeSeriesResult(formulaService, inputData, windowSize);
        }

        @Test
        @DisplayName("3-2. 단순 변환 계수를 사용할 때 예외가 발생해야 한다.")
        void shouldThrowExceptionWhenUsingCoefficient() {
            // [given]
            Integer unitConversionNo = 1;
            List<BigDecimal> inputData = Arrays.asList(
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("30")
            );
            int windowSize = 2;
            BigDecimal coefficient = new BigDecimal("2");

            UnitConvert unitConvert = UnitConvert.builder()
                .calculateFormulaServiceNm("")
                .convertCoef(coefficient)
                .isDeleted(Boolean.FALSE)
                .build();

            given(unitConvertRepository.findByUnitConvNoAndIsDeletedFalse(unitConversionNo))
                .willReturn(Optional.of(unitConvert));

            // [when & then]
            assertThatThrownBy(() -> unitConversionService.convertTimeSeriesResult(unitConversionNo, inputData, windowSize))
                .isInstanceOf(AppErrorException.class)
                .matches(e -> ((AppErrorException) e).getErrorCode() == ResultCode.Error.BIZ_RULE_VIOLATION);
        }

        @Test
        @DisplayName("3-3. 단위 변환이 존재하지 않을 때 예외가 발생해야 한다.")
        void shouldThrowExceptionWhenUnitConversionNotFound() {
            // [given]
            Integer unitConversionNo = 99; // 존재하지 않는 번호
            List<BigDecimal> inputData = Arrays.asList(
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("30")
            );
            int windowSize = 2;

            given(unitConvertRepository.findByUnitConvNoAndIsDeletedFalse(unitConversionNo))
                .willReturn(Optional.empty());

            // [when & then]
            assertThatThrownBy(() -> unitConversionService.convertTimeSeriesResult(unitConversionNo, inputData, windowSize))
                .isInstanceOf(AppErrorException.class)
                .matches(e -> ((AppErrorException) e).getErrorCode() == ResultCode.Error.RSC_NOT_FOUND);
        }
    }
}
