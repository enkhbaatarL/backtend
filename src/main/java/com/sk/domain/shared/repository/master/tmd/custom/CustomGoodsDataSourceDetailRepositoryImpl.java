package com.sk.domain.shared.repository.master.tmd.custom;

import static com.sk.domain.shared.entity.master.tcm.QComponentInfo.componentInfo;
import static com.sk.domain.shared.entity.master.tcm.QUploadedFile.uploadedFile;
import static com.sk.domain.shared.entity.master.tmd.QGoodsDataSourceDetail.goodsDataSourceDetail;

import java.util.List;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.response.provider.StructuredDataRes;
import com.sk.domain.shared.entity.master.tmd.GoodsData;

import com.sk.domain.admin.dto.response.provider.QStructuredDataRes;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomGoodsDataSourceDetailRepositoryImpl implements CustomGoodsDataSourceDetailRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<StructuredDataRes> findGoodsDataSourceStructuredDatasByConditions(GoodsData goodsDataNo) {
        return queryFactory
                .select(new QStructuredDataRes(
                    componentInfo.componentTypeNm,
                    uploadedFile.createDt
                ))
                .from(goodsDataSourceDetail)
                .leftJoin(uploadedFile)
                    .on(uploadedFile.fileRelationshipObjectDistinguishNo.eq(goodsDataSourceDetail.goodsDataSourceNo)
                        .and(uploadedFile.fileRelationshipObjectSeparateCd.eq("GDATS"))
                        .and(uploadedFile.isDeleted.isFalse()))
                .leftJoin(componentInfo)
                    .on(goodsDataSourceDetail.componentInfo.componentTypeId.eq(componentInfo.componentTypeId))
                .where(goodsDataSourceDetail.goodsData.eq(goodsDataNo)
                        .and(goodsDataSourceDetail.isDeleted.isFalse()))
                .fetch();
    }
}
