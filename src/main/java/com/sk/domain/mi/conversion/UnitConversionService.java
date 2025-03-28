package com.sk.domain.mi.conversion;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.conversion.strategy.ConversionStrategy;
import com.sk.domain.mi.conversion_on_demand.FormulaService;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.repository.master.tmd.UnitConvertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 단위 변환 서비스 클래스
 */
@Service
@RequiredArgsConstructor
public class UnitConversionService {

    private final ConversionContext conversionContext;
    private final UnitConvertRepository unitConvertRepository;

    /**
     * 단위 변환
     *
     * @param unitConversionNo 단위변환번호
     * @param formulaService 단위변환 서비스 명
     * @param values 변환시킬 값 배열
     * @return BigDecimal
     */
    public BigDecimal convert(Integer unitConversionNo, FormulaService formulaService, BigDecimal... values) {
        // 전략 패턴 사용 - 복잡한 공식이 필요한 경우
        Optional<ConversionStrategy> strategyOpt = conversionContext.findStrategy(formulaService);

        if (strategyOpt.isPresent()) {
            return strategyOpt.get().convert(values);
        }

        // 단순 변환 계수 사용 - DB에 저장된 단위 변환
        UnitConvert unitConvert = unitConvertRepository.findByUnitConvNoAndIsDeletedFalse(unitConversionNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                "It doesn't exist in the [unit-conversion]. unitConversionNo: %s".formatted(unitConversionNo)));

        return values[0].multiply(unitConvert.getConvertCoef());
    }

    /**
     * 시계열 데이터 변환
     */
    public List<BigDecimal> convertTimeSeries(FormulaService formulaService, List<BigDecimal> timeSeries, int windowSize) {
        return conversionContext.convertTimeSeries(formulaService, timeSeries, windowSize);
    }

    /**
     * 특정 결과 타입을 반환하는 시계열 데이터 변환
     */
    public <T> List<T> convertTimeSeries(FormulaService formulaService, List<BigDecimal> timeSeries, int windowSize, Class<T> resultType) {
        return conversionContext.convertTimeSeries(formulaService, timeSeries, windowSize, resultType);
    }

    /**
     * 두 개의 전략을 체인으로 연결
     */
    public ConversionStrategy chainStrategies(FormulaService first, FormulaService second, FormulaService target) {
        Optional<ConversionStrategy> firstStrategy = conversionContext.findStrategy(first);
        Optional<ConversionStrategy> secondStrategy = conversionContext.findStrategy(second);

        if (firstStrategy.isPresent() && secondStrategy.isPresent()) {
            return conversionContext.createChainedStrategy(
                List.of(firstStrategy.get(), secondStrategy.get()), target);
        }

        throw new IllegalArgumentException("One or both strategies not found");
    }

    /**
     * 여러 전략을 위임 패턴으로 조합
     */
    public ConversionStrategy delegateStrategies(List<FormulaService> services, FormulaService target) {
        List<ConversionStrategy> strategies = services.stream()
            .map(service -> conversionContext.findStrategy(service)
                .orElseThrow(() -> new IllegalArgumentException("Strategy not found: " + service)))
            .toList();

        return conversionContext.createDelegatingStrategy(strategies, target);
    }

}
