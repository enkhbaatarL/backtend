package com.sk.domain.mi.validator;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.ValidationResult;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GoodsDataValidator {

    private final GoodsDataRepository goodsDataRepository;

    public ValidationResult<GoodsData> ensureExists(Integer goodsDataNo) {
        Optional<GoodsData> entity = goodsDataRepository.findByGoodsDataNoAndIsDeletedFalse(goodsDataNo);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                    "It doesn't exist in the [GoodsData]. goodsDataNo: %s".formatted(goodsDataNo)));
        }
    }

    public ValidationResult<Void> ensureNotExists(Integer goodsDataNo) {
        Optional<GoodsData> entity = goodsDataRepository.findByGoodsDataNoAndIsDeletedFalse(goodsDataNo);
        if (entity.isPresent()) {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_ALREADY_EXISTS,
                    "goodsDataNo: %s".formatted(goodsDataNo)));
        } else {
            return ValidationResult.success(null);
        }
    }

}
