package com.sk.domain.shared.repository.master.tmd.custom;

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
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.dataResult.QUserWithCompany;
import com.sk.domain.admin.dto.dataResult.UserWithCompany;
import com.sk.domain.admin.dto.response.QUserWithCreatorRes;
import com.sk.domain.admin.dto.response.UserWithCreatorRes;
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
    public List<UserWithCompany> findAllUserByInfoIds(List<String> imCds) {

        return queryFactory.select(new QUserWithCompany(
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
                .on(userInfo.companyCd.eq(customerCompany.customerCompanyCd))
                .where(userInfo.imCd.in(imCds)
                        .and(userInfo.isDeleted.isFalse()))
                .fetch();
    }

    @Override
    public Optional<UserWithCompany> findUserByInfoId(String imCd) {
        return Optional.ofNullable(queryFactory.select(new QUserWithCompany(
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
                .on(userInfo.companyCd.eq(customerCompany.customerCompanyCd))
                .where(userInfo.imCd.eq(imCd)
                        .and(userInfo.isDeleted.isFalse()))
                .fetchOne());
    }

    @Override
    public Page<UserWithCreatorRes> findUsersByConditions(int page, int size, UserRoleCd userRoleCd,
            Integer customerCompanyNo, String departmentNm,
            String userNm, String emailAddress) {

        QUser user = QUser.user;
        QUser creator = new QUser("creator");

        QCustomerCompany customerCompany = QCustomerCompany.customerCompany;
        QCustomerCompany creatorCompany = new QCustomerCompany("creatorCompany");

        Pageable pageable = PageRequest.of(page - 1, size);

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(user.isDeleted.isFalse())
                .and(user.userRoleCd.eq(userRoleCd));

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

        List<UserWithCreatorRes> content = queryFactory
                .select(new QUserWithCreatorRes(
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
                        creator.userNm,
                        creator.departmentNm,
                        creator.onDutyNm,
                        creator.customerCompany.customerCompanyNo,
                        creator.customerCompany.customerCompanyNm,
                        user.createUserId,
                        user.createDt,
                        user.modifyUserId,
                        user.modifyDt))
                .from(user)
                .leftJoin(user.customerCompany, customerCompany)
                .leftJoin(creator).on(user.createUserId.eq(creator.userLoginId))
                .leftJoin(creator.customerCompany, creatorCompany)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy()
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(user.count())
                .from(user)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
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
}
