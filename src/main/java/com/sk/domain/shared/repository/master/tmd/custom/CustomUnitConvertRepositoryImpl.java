package com.sk.domain.shared.repository.master.tmd.custom;

import static com.sk.domain.shared.entity.master.tmd.QUnitConvert.unitConvert;

import java.util.List;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomUnitConvertRepositoryImpl implements CustomUnitConvertRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<UnitConvert> findUnitConvertsByConditions(GoodsDataSourceDetail goodsDataSourceDetail) {

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(unitConvert.isDeleted.isFalse())
                .and(unitConvert.convertPreviousUnitNm.eq(goodsDataSourceDetail.getGoodsDataSourceUnitNm()));

        if (goodsDataSourceDetail.getConvertTargetDetailTypeNm() != null) {
            builder.and(unitConvert.convertTargetDetailTypeNm.eq(goodsDataSourceDetail.getConvertTargetDetailTypeNm())
                    .or(unitConvert.convertTargetDetailTypeNm.isNull()));
        }

        return queryFactory.selectFrom(unitConvert)
                .where(builder)
                .fetch();
    }

}
