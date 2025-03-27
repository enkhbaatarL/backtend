package com.sk.domain.mi.service.conversion_initialize;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.service.conversion_initialize.strategy.ConversionStrategy;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.repository.master.tmd.UnitConvertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UnitConversionService {

    private final UnitConvertRepository unitConvertRepository;
    private final ConversionContext conversionContext;

    /**
     * 단위 변환
     *
     * @param unitConversionNo 단위변환번호
     * @param formulaService 단위변환 서비스 명
     * @param value 변환시킬 값 배열
     * @return BigDecimal
     */
    public BigDecimal convert(Integer unitConversionNo, FormulaService formulaService, BigDecimal... value) {

        // Complex strategy
        Optional<ConversionStrategy> complexStrategy = conversionContext.findComplexStrategy(formulaService);

        if (complexStrategy.isPresent()) {
            return complexStrategy.get().convert(value);
        }

        // Simple strategy
        UnitConvert unitConvert = unitConvertRepository.findByUnitConvNoAndIsDeletedFalse(unitConversionNo)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [unit-conversion]. unitConversionNo: %s".formatted(unitConversionNo)));

        return value[0].multiply(unitConvert.getConvertCoef());
    }

}
