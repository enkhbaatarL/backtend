package com.sk.domain.shared.repository.master.tmd.custom;

import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;
import static com.sk.domain.shared.entity.master.tmd.QUser.user;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.response.user.QUserDetailRes;
// import com.sk.domain.admin.dto.response.QUserDetailRes;
import com.sk.domain.admin.dto.response.user.UserDetailRes;
import com.sk.domain.shared.entity.master.tif.UserInfo;
import com.sk.domain.shared.entity.master.tmd.QCustomerCompany;
import com.sk.domain.shared.entity.master.tmd.QUser;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CustomUserRepositoryImpl implements CustomUserRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<UserDetailRes> findUsersByConditions(int page, int size, List<UserRoleCd> userRoleCds,
			Integer customerCompanyNo, String departmentNm,
			String userNm, String emailAddress) {

		/*
		 * 현재 정책은 creator, modifier 는 관리자로 관리사를 Join 하여 데이터 조회
		 */
		QUser user = QUser.user;
		QUser creator = new QUser("creator"); 
		QUser modifier = new QUser("modifier");

		QCustomerCompany customerCompany = QCustomerCompany.customerCompany;
		QCustomerCompany creatorCompany = new QCustomerCompany("creatorCompany"); 
		QCustomerCompany modifierCompany = new QCustomerCompany("modifierCompany");

		Pageable pageable = PageRequest.of(page - 1, size);

		BooleanBuilder builder = new BooleanBuilder();
		builder.and(user.isDeleted.isFalse())
				.and(user.userRoleCd.in(userRoleCds));

		if (customerCompanyNo != null) {
			builder.and(user.customerCompany.customerCompanyNo.eq(customerCompanyNo));
		}

		if (departmentNm != null) {
			builder.and(user.departmentNm.eq(departmentNm));
		}

		if (userNm != null) {
			builder.and(user.userNm.lower().contains(userNm.toLowerCase()));
		}

		if (emailAddress != null) {
			builder.and(user.userLoginId.lower().contains(emailAddress.toLowerCase())); // 대소문자 구분 무시
		}

		List<UserDetailRes> content = queryFactory
				.select(new QUserDetailRes(
						user.userNo,
						user.userNm,
						user.userLoginId,
						user.userRoleCd,
						user.positionNm,
						user.jobGradeNm,
						user.onDutyNm,
						user.departmentNm,
						user.isOtherServiceAccessPossible,
						user.isIndividualInfoAgreement,
						user.individualInfoAgreementDtm,
						user.customerCompany.customerCompanyNo,
						user.customerCompany.customerCompanyNm,
						creator.userLoginId,
						user.createDt,
						modifier.userLoginId,
						user.modifyDt,
						creator.userNm,
						creator.departmentNm,
						creatorCompany.customerCompanyNm,
						modifier.userNm,
						modifier.departmentNm,
						modifierCompany.customerCompanyNm))
				.distinct()
				.from(user)
				.leftJoin(user.customerCompany, customerCompany)
				.leftJoin(creator).on(user.createUserId.eq(creator.userLoginId).and(creator.userRoleCd.eq(UserRoleCd.ADMIN).and(creator.isDeleted.isFalse())))
				.leftJoin(creator.customerCompany, creatorCompany)
				.leftJoin(modifier).on(user.modifyUserId.eq(modifier.userLoginId).and(modifier.userRoleCd.eq(UserRoleCd.ADMIN).and(modifier.isDeleted.isFalse())))
				.leftJoin(modifier.customerCompany, modifierCompany)
				.where(builder)
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.orderBy(user.userNm.asc())
				.fetch();

		JPAQuery<Long> countQuery = queryFactory
				.select(user.count())
				.from(user)
				.where(builder);

		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}

	@Override
	public Optional<UserDetailRes> findUserByConditions(int userNo) {
		/*
		 * 현재 정책은 creator, modifier 는 관리자로 관리사를 Join 하여 데이터 조회
		 */
		QUser user = QUser.user;
		QUser creator = new QUser("creator"); 
		QUser modifier = new QUser("modifier");

		QCustomerCompany customerCompany = QCustomerCompany.customerCompany;
		QCustomerCompany creatorCompany = new QCustomerCompany("creatorCompany"); 
		QCustomerCompany modifierCompany = new QCustomerCompany("modifierCompany");

		return Optional.ofNullable(queryFactory
				.select(new QUserDetailRes(
					user.userNo,
					user.userNm,
					user.userLoginId,
					user.userRoleCd,
					user.positionNm,
					user.jobGradeNm,
					user.onDutyNm,
					user.departmentNm,
					user.isOtherServiceAccessPossible,
					user.isIndividualInfoAgreement,
					user.individualInfoAgreementDtm,
					user.customerCompany.customerCompanyNo,
					user.customerCompany.customerCompanyNm,
					creator.userLoginId,
					user.createDt,
					modifier.userLoginId,
					user.modifyDt,
					creator.userNm,
					creator.departmentNm,
					creatorCompany.customerCompanyNm,
					modifier.userNm,
					modifier.departmentNm,
					modifierCompany.customerCompanyNm))
				.from(user)
				.leftJoin(user.customerCompany, customerCompany)
				.leftJoin(creator).on(user.createUserId.eq(creator.userLoginId).and(creator.userRoleCd.eq(UserRoleCd.ADMIN).and(creator.isDeleted.isFalse())))
				.leftJoin(creator.customerCompany, creatorCompany)
				.leftJoin(modifier).on(user.modifyUserId.eq(modifier.userLoginId).and(modifier.userRoleCd.eq(UserRoleCd.ADMIN).and(modifier.isDeleted.isFalse())))
				.leftJoin(modifier.customerCompany, modifierCompany)
				.where(
						user.userNo.eq(userNo),
						user.isDeleted.isFalse()
				)
				.fetchOne());
	}

	@Override
	public List<User> findByUserLoginIdsAndRoleAndNotDeleted(List<String> userLoginIds, UserRoleCd role) {
		return queryFactory
				.selectFrom(user)
				.where(
						user.userLoginId.in(userLoginIds),
						user.userRoleCd.eq(role),
						user.isDeleted.eq(false))
				.fetch();
	}

	@Override
	public List<User> findByUserInfosAndIsDeletedFalse(List<UserInfo> userInfos) {
		if (userInfos == null || userInfos.isEmpty()) {
			return Collections.emptyList();
		}

		BooleanBuilder builder = new BooleanBuilder();
		for (UserInfo userInfo : userInfos) {
			String loginId = userInfo.getLoginId();
			String companyCd = userInfo.getCompanyCd();
			if (loginId != null && companyCd != null) {
				builder.or(
						user.userLoginId.eq(loginId)
								.and(user.customerCompany.customerCompanyCd.eq(companyCd)));
			}
		}

		if (builder.getValue() == null) {
			return Collections.emptyList();
		}

		return queryFactory
				.selectFrom(user)
				.innerJoin(user.customerCompany, customerCompany)
				.where(builder
						.and(user.isDeleted.isFalse()))
				.fetch();
	}

	@Override
	public Optional<User> findByUserNoAndUserRoleCdList(Integer userNo, List<UserRoleCd> userRoleCds) {
		return Optional.ofNullable(queryFactory
				.selectFrom(user)
				.where(user.userNo.eq(userNo)
						.and(user.userRoleCd.in(userRoleCds))
						.and(user.isDeleted.isFalse()))
				.fetchOne());
	}
}
