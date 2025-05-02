package com.sk.domain.shared.service.permissions;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoodsNestedEntityResolver {

    private final GoodsDataRepository goodsDataRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;

    public Integer resolve(String paramName, Integer id) {

        return switch (paramName) {
            case "fileNo" -> {
                UploadedFile uploadedFile = uploadedFileRepository.findByFileNoAndIsDeletedFalse(id.longValue())
                    .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [uploaded-file]. fileNo: %s".formatted(id)));

                GoodsData goodsData = goodsDataRepository.findByGoodsDataNoAndIsDeletedFalse(uploadedFile.getFileRelationshipObjectDistinguishNo())
                    .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [goods-data]. goodsDataNo: %s".formatted(uploadedFile.getFileRelationshipObjectDistinguishNo())));

                yield goodsData.getGoods().getGoodsNo();
            }

            case "goodsDatasourceNo" -> {
                GoodsDataSourceDetail gds = goodsDataSourceDetailRepository.findByGoodsDataSourceNoAndIsDeletedFalse(id)
                    .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [goods-datasource]. goodsDatasourceNo: %s".formatted(id)));

                yield gds.getGoodsData().getGoods().getGoodsNo();
            }

            default -> throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "Unsupported resourceIdParam: " + paramName);
        };

    }
}

