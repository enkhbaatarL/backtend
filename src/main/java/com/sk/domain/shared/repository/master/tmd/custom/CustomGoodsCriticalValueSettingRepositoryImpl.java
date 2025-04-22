package com.sk.domain.shared.repository.master.tmd.custom;

import static com.sk.domain.shared.entity.master.tmd.QGoodsCriticalValueSetting.goodsCriticalValueSetting;
import static com.sk.domain.shared.entity.master.tmd.QGoodsData.goodsData;
import static com.sk.domain.shared.entity.master.tmd.QGoodsDataSourceDetail.goodsDataSourceDetail;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.mi.dto.response.notification.MiNotificationSettingGoodsDataSourceRes;
import com.sk.domain.mi.dto.response.notification.MiNotificationSettingRes;
import com.sk.domain.mi.dto.response.notification.QMiNotificationSettingGoodsDataSourceRes;
import com.sk.domain.mi.dto.response.notification.QMiNotificationSettingRes;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomGoodsCriticalValueSettingRepositoryImpl implements CustomGoodsCriticalValueSettingRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<MiNotificationSettingRes> findNotificationSettingsByConditions(GoodsDataSourceDetail goodsDataSourceDetail, User user, int page,
            int size) {

        Pageable pageable = PageRequest.of(page - 1, size);

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(goodsCriticalValueSetting.goodsDataSourceDetail.eq(goodsDataSourceDetail))
                .and(goodsCriticalValueSetting.user.eq(user))
                .and(goodsCriticalValueSetting.isDeleted.isFalse());

        List<MiNotificationSettingRes> content = queryFactory.select(
                new QMiNotificationSettingRes(
                        goodsCriticalValueSetting.userGoodsNotificationSettingNo,
                        goodsCriticalValueSetting.user.userNo,
                        goodsCriticalValueSetting.goodsDataSourceDetail.goodsDataSourceNo,
                        goodsCriticalValueSetting.goodsDataSourceDetail.goodsDataSourceNm,
                        goodsCriticalValueSetting.notificationCriticalValueSeparateCd,
                        goodsCriticalValueSetting.criticalValueSettingVol,
                        goodsCriticalValueSetting.criticalValueSettingUnitNm))
                .from(goodsCriticalValueSetting)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(goodsCriticalValueSetting.userGoodsNotificationSettingNo.desc())
                .fetch();

        JPQLQuery<Long> countQuery = queryFactory
                .select(goodsCriticalValueSetting.count())
                .from(goodsCriticalValueSetting)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<MiNotificationSettingGoodsDataSourceRes> findNotificationSettingGoodsDataSourceByConditions(int page, int size, String search, User user) {
        Pageable pageable = PageRequest.of(page - 1, size);

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(goodsDataSourceDetail.isDeleted.isFalse())
                .and(goodsCriticalValueSetting.isDeleted.isFalse())
                .and(goodsData.isDeleted.isFalse())
                .and(goodsCriticalValueSetting.user.eq(user));

        if (search != null && !search.isEmpty()) {
            builder.and(goodsData.goodsDataNm.containsIgnoreCase(search.trim()));
        }

        List<MiNotificationSettingGoodsDataSourceRes> content = queryFactory
                .select(new QMiNotificationSettingGoodsDataSourceRes(
                        goodsData.goodsDataNo,
                        goodsData.goodsDataNm,
                        goodsDataSourceDetail.goodsDataSourceNo,
                        goodsDataSourceDetail.goodsDataSourceNm,
                        goodsCriticalValueSetting.count().intValue()))
                .from(goodsDataSourceDetail)
                .join(goodsDataSourceDetail.goodsData, goodsData).fetchJoin()
                .leftJoin(goodsCriticalValueSetting)
                .on(goodsCriticalValueSetting.goodsDataSourceDetail.eq(goodsDataSourceDetail)
                        .and(goodsCriticalValueSetting.isDeleted.isFalse())
                        .and(goodsCriticalValueSetting.user.eq(user)))
                .where(builder)
                .groupBy(goodsDataSourceDetail.goodsDataSourceNo, goodsData.goodsDataNo, goodsData.goodsDataNm)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(goodsDataSourceDetail.goodsDataSourceNo.desc())
                .fetch();

        JPQLQuery<Long> countQuery = queryFactory
                .select(goodsDataSourceDetail.countDistinct())
                .from(goodsDataSourceDetail)
                .join(goodsDataSourceDetail.goodsData, goodsData)
                .leftJoin(goodsCriticalValueSetting)
                .on(goodsCriticalValueSetting.goodsDataSourceDetail.eq(goodsDataSourceDetail)
                        .and(goodsCriticalValueSetting.isDeleted.isFalse())
                        .and(goodsCriticalValueSetting.user.eq(user)))
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
