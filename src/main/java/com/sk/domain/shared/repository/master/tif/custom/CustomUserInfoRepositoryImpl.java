package com.sk.domain.shared.repository.master.tif.custom;

import static com.sk.domain.shared.entity.master.tif.QUserInfo.userInfo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.response.QUserInfoRes;
import com.sk.domain.admin.dto.response.UserInfoRes;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CustomUserInfoRepositoryImpl implements CustomUserInfoRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<UserInfoRes> findUserInfosByConditions(int page, int size, String companyCode, String userName,
            String emailAddress) {
        Pageable pageable = PageRequest.of(page - 1, size);

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(userInfo.isDeleted.eq(false))
                .and(userInfo.companyCode.eq(companyCode));

        if (userName != null) {
            builder.and(userInfo.userName.contains(userName));
        }

        if (emailAddress != null) {
            builder.and(userInfo.emailAddress.contains(emailAddress));
        }

        List<UserInfoRes> content = queryFactory.select(new QUserInfoRes(
                userInfo.imCode,
                userInfo.userName,
                userInfo.userEnglishName,
                userInfo.loginId,
                userInfo.emailAddress,
                userInfo.companyCode,
                userInfo.companyName,
                userInfo.employeeNumber,
                userInfo.departmentCode,
                userInfo.departmentName,
                userInfo.positionCode,
                userInfo.positionName,
                userInfo.jobGradeCode,
                userInfo.jobGradeName,
                userInfo.onDutyCode,
                userInfo.onDutyName))
                .from(userInfo)
                .where(userInfo.isDeleted.eq(false))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .where(builder)
                .fetch();

        JPQLQuery<Long> countQuery = queryFactory
                .select(userInfo.count())
                .from(userInfo)
                .where(userInfo.isDeleted.eq(false));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
