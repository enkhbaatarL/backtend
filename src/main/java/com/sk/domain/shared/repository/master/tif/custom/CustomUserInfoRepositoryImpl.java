package com.sk.domain.shared.repository.master.tif.custom;

import static com.sk.domain.shared.entity.master.tif.QUserInfo.userInfo;
import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;
import static com.sk.domain.shared.entity.master.tmd.QUser.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.internal.QUserInfoWithCompany;
import com.sk.domain.admin.dto.internal.UserInfoWithCompany;
import com.sk.domain.admin.dto.response.user.QUserInfoRes;
import com.sk.domain.admin.dto.response.user.UserInfoRes;
import com.sk.domain.shared.enums.UserRoleCd;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CustomUserInfoRepositoryImpl implements CustomUserInfoRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<UserInfoRes> findUserInfosByConditions(int page, int size, List<UserRoleCd> userRoleCds, String companyCd,
			String userNm,
			String emailAddress) {

		Pageable pageable = PageRequest.of(page - 1, size);


		// //TODO: 호영: => 조회 API 하나로 => ROLECD로 고객사냐 관리사냐 구분해서 JOIN 조건 추가


		BooleanBuilder builder = new BooleanBuilder();
		builder.and(userInfo.isDeleted.isFalse())
				.and(userInfo.companyCd.eq(companyCd))
				.and(queryFactory.selectFrom(customerCompany)
						.where(customerCompany.customerCompanyCd.eq(companyCd)
								.and(customerCompany.isUsed.isTrue())
								.and(customerCompany.isDeleted.isFalse()))
						.exists());

		if (userNm != null) {
			builder.and(userInfo.userNm.lower().contains(userNm.toLowerCase()));
		}

		if (emailAddress != null) {
			builder.and(userInfo.emailAddress.lower().contains(emailAddress.toLowerCase())); // 대소문자 구분 무시
		}

		BooleanExpression isRegistered = user.userLoginId.isNotNull().and(user.isDeleted.isFalse());

		List<UserInfoRes> content = queryFactory.select(new QUserInfoRes(
				userInfo.imCd,
				userInfo.userNm,
				userInfo.userEnglishNm,
				userInfo.loginId,
				userInfo.emailAddress,
				userInfo.companyCd,
				userInfo.companyNm,
				userInfo.employeeNo,
				userInfo.departmentCd,
				userInfo.departmentNm,
				userInfo.positionCd,
				userInfo.positionNm,
				userInfo.jobGradeCd,
				userInfo.jobGradeNm,
				userInfo.onDutyCd,
				userInfo.onDutyNm,
				isRegistered))
				.from(userInfo)
				.leftJoin(user).on(userInfo.loginId.eq(user.userLoginId).and(user.userRoleCd.in(userRoleCds)))
				.where(builder)
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.orderBy(userInfo.userNm.asc())
				.fetch();

		JPQLQuery<Long> countQuery = queryFactory
				.select(userInfo.count())
				.from(userInfo)
				.where(builder);

		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}

	@Override
	public List<UserInfoWithCompany> findAllUserByInfoIds(List<String> imCds) {
		return queryFactory.select(new QUserInfoWithCompany(
				userInfo.userNm,
				userInfo.loginId,
				userInfo.positionNm,
				userInfo.jobGradeNm,
				userInfo.onDutyNm,
				userInfo.departmentNm,
				customerCompany,
				customerCompany.customerCompanyNm))
				.from(userInfo)
				.leftJoin(customerCompany)
				.on(userInfo.companyCd.eq(customerCompany.customerCompanyCd)
						.and(customerCompany.isUsed.isTrue())
						.and(customerCompany.isDeleted.isFalse()))

						//TODO: 호영: 고객사 OR 관리사 조건 추가

				.where(userInfo.imCd.in(imCds)
						.and(userInfo.isDeleted.isFalse()))
				.fetch();
	}

	@Override
	public Optional<UserInfoWithCompany> findUserByInfoId(String imCd) {
		return Optional.ofNullable(queryFactory.select(new QUserInfoWithCompany(
				userInfo.userNm,
				userInfo.loginId,
				userInfo.positionNm,
				userInfo.jobGradeNm,
				userInfo.onDutyNm,
				userInfo.departmentNm,
				customerCompany,
				customerCompany.customerCompanyNm))
				.from(userInfo)
				.leftJoin(customerCompany)
				.on(userInfo.companyCd.eq(customerCompany.customerCompanyCd)
						.and(customerCompany.isUsed.isTrue())
						.and(customerCompany.isDeleted.isFalse()))
				.where(userInfo.imCd.eq(imCd)
						.and(userInfo.isDeleted.isFalse()))
				.fetchOne());
	}

}
