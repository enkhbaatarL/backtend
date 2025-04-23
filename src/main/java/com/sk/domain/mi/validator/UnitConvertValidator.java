package com.sk.domain.mi.validator;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.ValidationResult;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.repository.master.tmd.UnitConvertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UnitConvertValidator {

    private final UnitConvertRepository unitConvertRepository;

    public ValidationResult<UnitConvert> ensureExists(Integer unitConversionNo) {
        Optional<UnitConvert> entity = unitConvertRepository.findByUnitConvNoAndIsDeletedFalse(unitConversionNo);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                    "It doesn't exist in the [currentUnitCd-conversion]. unitConversionNo: %s".formatted(unitConversionNo)));
        }
    }

}
