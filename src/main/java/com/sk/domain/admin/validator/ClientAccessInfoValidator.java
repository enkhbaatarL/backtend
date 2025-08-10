package com.sk.domain.admin.validator;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.ValidationResult;
import com.sk.domain.shared.entity.master.tif.ClientAccessInfo;
import com.sk.domain.shared.repository.master.tif.ClientAccessInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClientAccessInfoValidator {

    private final ClientAccessInfoRepository clientAccessInfoRepository;

    public ValidationResult<ClientAccessInfo> ensureExists(Integer clientAccessRegNo) {
        Optional<ClientAccessInfo> entity = clientAccessInfoRepository.findByClientAccessRegNoAndIsDeletedFalse(clientAccessRegNo);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                    "It doesn't exist in the [ClientAccessInfo]. clientAccessRegNo: %s".formatted(clientAccessRegNo)));
        }
    }

    public ValidationResult<Void> ensureNotExists(Integer clientAccessRegNo) {
        Optional<ClientAccessInfo> entity = clientAccessInfoRepository.findByClientAccessRegNoAndIsDeletedFalse(clientAccessRegNo);
        if (entity.isPresent()) {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS,
                    "clientAccessRegNo: %s".formatted(clientAccessRegNo)));
        } else {
            return ValidationResult.success(null);
        }
    }

}
