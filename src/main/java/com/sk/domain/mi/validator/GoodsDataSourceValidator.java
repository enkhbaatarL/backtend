package com.sk.domain.mi.validator;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.ValidationResult;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GoodsDataSourceValidator {

    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;

    public ValidationResult<GoodsDataSourceDetail> ensureExists(Integer goodsDataSourceNo) {
        Optional<GoodsDataSourceDetail> entity = goodsDataSourceDetailRepository.findGoodsDataSourceNoAndIsDeletedFalse(goodsDataSourceNo);
        if (entity.isPresent()) {
            return ValidationResult.success(entity.get());
        } else {
            return ValidationResult.failure(AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND,
                    "It doesn't exist in the [goods-datasource]. goodsDataSourceNo: %s".formatted(goodsDataSourceNo)));
        }
    }

}
