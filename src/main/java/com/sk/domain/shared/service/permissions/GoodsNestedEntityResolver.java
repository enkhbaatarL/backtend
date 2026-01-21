package com.sk.domain.shared.service.permissions;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.NewsSummary;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsDataSourceDetailRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsRepository;
import com.sk.domain.shared.repository.master.tmd.NewsSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoodsNestedEntityResolver {

    private final GoodsDataRepository goodsDataRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final GoodsDataSourceDetailRepository goodsDataSourceDetailRepository;
    private final NewsSummaryRepository newsSummaryRepository;
    private final GoodsRepository goodsRepository;

    public Integer resolve(String paramName, Object id) {

        return switch (paramName) {
            case "fileNo" -> {
                Long fileNo = toLong(id);
                UploadedFile uploadedFile = uploadedFileRepository.findByFileNoAndIsDeletedFalse(fileNo)
                    .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [uploaded-file]. fileNo: %s".formatted(fileNo)));

                GoodsData goodsData = goodsDataRepository.findByGoodsDataNoAndIsDeletedFalse(uploadedFile.getFileRelationshipObjectDistinguishNo())
                    .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [goods-data]. goodsDataNo: %s".formatted(uploadedFile.getFileRelationshipObjectDistinguishNo())));

                yield goodsData.getGoods().getGoodsNo();
            }

            case "goodsDataNo" -> {
                Integer goodsDataNo = toInt(id);
                yield goodsDataRepository.queryGoodsNoByGoodsDataNo(goodsDataNo)
                    .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [goods-data]. goodsDataNo: %s".formatted(goodsDataNo)));
            }

            case "goodsDataSourceNo" -> {
                Integer goodsDataSourceNo = toInt(id);
                yield goodsDataSourceDetailRepository.queryGoodsNoByGoodsDataSourceNo(goodsDataSourceNo)
                    .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [goods-datasource]. goodsDatasourceNo: %s".formatted(goodsDataSourceNo)));
            }

            case "newsSummaryId" -> {
                Long newsSummaryId = toLong(id);
                NewsSummary newsSummary = newsSummaryRepository.findById(newsSummaryId)
                    .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "It doesn't exist in the [news-summary]. newsSummaryId: %s".formatted(newsSummaryId)));
                yield goodsRepository.queryGoodsNoByGoodsId(newsSummary.getProviderId())
                    .orElseThrow(() -> CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "You do not have permission to 'VIEW' the goods(goodsId: %s)".formatted(newsSummary.getProviderId())));
            }

            default -> throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "Unsupported resourceIdParam: " + paramName);
        };

    }

    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer intValue) {
            return intValue;
        }
        if (value instanceof Number numberValue) {
            return Math.toIntExact(numberValue.longValue());
        }
        if (value instanceof String stringValue) {
            return Integer.valueOf(stringValue);
        }
        throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "Unsupported resourceIdParam type: " + value.getClass().getSimpleName());
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        if (value instanceof String stringValue) {
            return Long.valueOf(stringValue);
        }
        throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "Unsupported resourceIdParam type: " + value.getClass().getSimpleName());
    }
}

