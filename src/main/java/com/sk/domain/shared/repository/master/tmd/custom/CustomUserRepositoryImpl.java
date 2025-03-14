package com.sk.domain.shared.repository.master.tmd.custom;

import static com.sk.domain.shared.entity.master.tif.QUserInfo.userInfo;
import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;

import java.util.List;

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
                userInfo.onDutyNm,
                userInfo.departmentNm,
                customerCompany,
                customerCompany.customerCompanyNm))
                .from(userInfo)
                .leftJoin(customerCompany)
                .on(userInfo.companyCd.eq(customerCompany.customerCompanyCd))
                .fetchJoin()
                .where(userInfo.imCd.in(imCds)
                        .and(userInfo.isDeleted.eq(false)))
                .fetch();
    }

    @Override
    public Page<UserWithCreatorRes> findUsersByConditions(int page, int size, Integer customerCompanyNo, String departmentNm,
            String userName, String emailAddress) {

        QUser user = QUser.user;
        QUser creator = new QUser("creator");

        QCustomerCompany customerCompany = QCustomerCompany.customerCompany;
        QCustomerCompany creatorCompany = new QCustomerCompany("creatorCompany");

        Pageable pageable = PageRequest.of(page - 1, size);

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(user.isDeleted.eq(false));
        
        if (customerCompanyNo != null) {
            builder.and(user.customerCompany.customerCompanyNo.eq(customerCompanyNo));
        }

        if (departmentNm != null) {
            builder.and(user.departmentNm.eq(departmentNm));
        }

        if (userName != null) {
            builder.and(user.userNm.contains(userName));
        }

        if (emailAddress != null) {
            builder.and(user.userLoginId.contains(emailAddress));
        }

        List<UserWithCreatorRes> content = queryFactory
                .select(new QUserWithCreatorRes(
                        user.userNo,
                        user.userNm,
                        user.userLoginId,
                        user.userRoleCd,
                        user.onDutyNm,
                        user.departmentNm,
                        user.isOtherServiceAccessPossible,
                        user.isIndividualInfoAgreement,
                        user.individualInfoAgreementDtm,
                        user.customerCompany,
                        customerCompany.customerCompanyNm,
                        creator.userNm,
                        creator.departmentNm,
                        creator.onDutyNm,
                        creator.customerCompany,
                        creatorCompany.customerCompanyNm,
                        user.createUserId,
                        user.createDt,
                        user.modifyUserId,
                        user.modifyDt
                ))
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
}
