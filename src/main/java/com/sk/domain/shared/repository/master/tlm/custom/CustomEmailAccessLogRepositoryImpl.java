package com.sk.domain.shared.repository.master.tlm.custom;

import static com.sk.domain.shared.entity.master.tlm.QEmailAccessLog.emailAccessLog;
import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;
import static com.sk.domain.shared.entity.master.tmd.QGoodsCriticalValueSetting.goodsCriticalValueSetting;
import static com.sk.domain.shared.entity.master.tmd.QGoodsDataSourceDetail.goodsDataSourceDetail;
import static com.sk.domain.shared.entity.master.tmd.QGoodsData.goodsData;
import static com.sk.domain.shared.entity.master.tmd.QGoods.goods;
import static com.sk.domain.shared.entity.master.tmd.QUser.user;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.response.notification.AdminNotificationCountRes;
import com.sk.domain.admin.dto.response.notification.AdminNotificationRes;
import com.sk.domain.admin.dto.response.notification.QAdminNotificationCountRes;
import com.sk.domain.admin.dto.response.notification.QAdminNotificationRes;
import com.sk.domain.mi.dto.response.notification.MiNotificationRes;
import com.sk.domain.mi.dto.response.notification.QMiNotificationRes;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CustomEmailAccessLogRepositoryImpl implements CustomEmailAccessLogRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<AdminNotificationRes> findNotificationsByConditions(int page, int size, Boolean isSendSuccess, String startDate, String endDate) {
		Pageable pageable = PageRequest.of(page - 1, size);

		BooleanBuilder builder = new BooleanBuilder();
		builder.and(emailAccessLog.isDeleted.isFalse());

		if (isSendSuccess != null) {
			builder.and(emailAccessLog.isSendSuccess.eq(isSendSuccess));
		}

		if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
			builder.and(
					emailAccessLog.emailSendDtm.between(DateUtil.fromYmd(startDate).atStartOfDay(), DateUtil.fromYmd(endDate).atTime(LocalTime.MAX)));
		}

		List<AdminNotificationRes> content = queryFactory.select(new QAdminNotificationRes(
				emailAccessLog.emailSendSn,
				emailAccessLog.emailReceiveUserLoginId,
				emailAccessLog.emailTitleNm,
				emailAccessLog.emailCntt,
				emailAccessLog.emailSendStatusNm,
				emailAccessLog.emailSendErrCntt,
				emailAccessLog.emailSendDtm,
				emailAccessLog.isSendSuccess,
				user.userNm,
				user.userRoleCd,
				user.positionNm,
				user.jobGradeNm,
				user.onDutyNm,
				user.departmentNm,
				customerCompany.customerCompanyNo,
				customerCompany.customerCompanyNm,
				goodsDataSourceDetail.goodsDataSourceNo,
				goodsDataSourceDetail.goodsDataSourceNm,
				emailAccessLog.createUserId,
				emailAccessLog.createDt,
				emailAccessLog.modifyUserId,
				emailAccessLog.modifyDt))
				.from(emailAccessLog)
				.leftJoin(user)
				.on(emailAccessLog.emailReceiveUserLoginId.eq(user.userLoginId).and(user.isDeleted.isFalse())
						.and(user.userRoleCd.eq(UserRoleCd.USER))) // 현재 정책으로 알람 받는 이용자는 USER만 가능
				.leftJoin(user.customerCompany, customerCompany)
				.leftJoin(emailAccessLog.goodsCriticalValueSetting, goodsCriticalValueSetting)
				.leftJoin(goodsCriticalValueSetting.goodsDataSourceDetail, goodsDataSourceDetail)
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.where(builder)
				.orderBy(emailAccessLog.emailSendSn.desc())
				.fetch();

		JPQLQuery<Long> countQuery = queryFactory.select(emailAccessLog.count())
				.from(emailAccessLog)
				.where(builder);

		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}

	@Override
	public Page<MiNotificationRes> findNotificationsWithinSevenDays(int page, int size, User user) {
		Pageable pageable = PageRequest.of(page - 1, size);

		BooleanBuilder builder = new BooleanBuilder();
		builder.and(emailAccessLog.isDeleted.isFalse())
				.and(emailAccessLog.emailSendDtm.goe(LocalDateTime.now().minusDays(7)))
				.and(emailAccessLog.emailReceiveUserLoginId.eq(user.getUserLoginId()));

		List<MiNotificationRes> content = queryFactory.select(new QMiNotificationRes(
				emailAccessLog.emailSendSn,
				emailAccessLog.emailTitleNm,
				emailAccessLog.emailCntt,
				emailAccessLog.emailSendDtm,
				emailAccessLog.isEmailInquiry,
				goodsCriticalValueSetting.notificationCriticalValueSeparateCd,
				goodsCriticalValueSetting.criticalValueSettingVol,
				goodsDataSourceDetail.goodsDataSourceUnitNm,
				goodsDataSourceDetail.goodsDataSourceNm,
				goodsDataSourceDetail.goodsDataSourceNo,
				goodsData.goodsDataNo,
				goods.goodsNo))
				.from(emailAccessLog)
				.leftJoin(emailAccessLog.goodsCriticalValueSetting, goodsCriticalValueSetting)
				.leftJoin(goodsCriticalValueSetting.goodsDataSourceDetail, goodsDataSourceDetail)
				.leftJoin(goodsDataSourceDetail.goodsData, goodsData)
				.leftJoin(goodsData.goods, goods)
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.where(builder)
				.orderBy(emailAccessLog.emailSendSn.desc(), emailAccessLog.emailSendDtm.desc())
				.fetch();

		JPQLQuery<Long> countQuery = queryFactory.select(emailAccessLog.count())
				.from(emailAccessLog)
				.leftJoin(emailAccessLog.goodsCriticalValueSetting, goodsCriticalValueSetting)
				.leftJoin(goodsCriticalValueSetting.goodsDataSourceDetail, goodsDataSourceDetail)
				.leftJoin(goodsDataSourceDetail.goodsData, goodsData)
				.leftJoin(goodsData.goods, goods)
				.where(builder);

		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}

	@Override
	public AdminNotificationCountRes findNotificationCounts(String startDate, String endDate) {
		BooleanBuilder builder = new BooleanBuilder();
		builder.and(emailAccessLog.isDeleted.isFalse());

		if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
			builder.and(
					emailAccessLog.emailSendDtm.between(DateUtil.fromYmd(startDate).atStartOfDay(), DateUtil.fromYmd(endDate).atTime(LocalTime.MAX)));
		}

		NumberTemplate<Integer> successCount = Expressions.numberTemplate(
				Integer.class,
				"sum(case when {0} = true then 1 else 0 end)",
				emailAccessLog.isSendSuccess);

		NumberTemplate<Integer> failCount = Expressions.numberTemplate(
				Integer.class,
				"sum(case when {0} = false then 1 else 0 end)",
				emailAccessLog.isSendSuccess);

		return queryFactory
				.select(new QAdminNotificationCountRes(
						emailAccessLog.count().intValue(),
						successCount,
						failCount,
						emailAccessLog.modifyDt.max()))
				.from(emailAccessLog)
				.where(builder)
				.fetchOne();
	}
}
