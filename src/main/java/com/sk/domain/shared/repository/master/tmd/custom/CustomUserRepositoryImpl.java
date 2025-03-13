package com.sk.domain.shared.repository.master.tmd.custom;

import static com.sk.domain.shared.entity.master.tif.QUserInfo.userInfo;
import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.dataResult.QUserWithCompany;
import com.sk.domain.admin.dto.dataResult.UserWithCompany;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CustomUserRepositoryImpl implements CustomUserRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<UserWithCompany> findAllUserByInfoIds(List<String> imCodes) {
        return queryFactory.select(new QUserWithCompany(
                userInfo.userName,
                userInfo.loginId,
                userInfo.onDutyName,
                userInfo.departmentName,
                customerCompany))
                .from(userInfo)
                .leftJoin(customerCompany)
                .on(userInfo.companyCode.eq(customerCompany.customerCompanyCd))
                .fetchJoin()
                .where(userInfo.imCode.in(imCodes)
                        .and(userInfo.isDeleted.eq(false)))
                .fetch();
    }
}
