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
    public Page<UserInfoRes> findUserInfosByConditions(int page, int size, String companyCd, String userNm,
            String emailAddress) {

        Pageable pageable = PageRequest.of(page - 1, size);

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(userInfo.isDeleted.eq(false))
                .and(userInfo.companyCd.eq(companyCd));

        if (userNm != null) {
            builder.and(userInfo.userNm.lower().contains(userNm.toLowerCase()));
        }

        if (emailAddress != null) {
            builder.and(userInfo.emailAddress.lower().contains(emailAddress.toLowerCase())); // 대소문자 구분 무시
        }

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
                userInfo.onDutyNm))
                .from(userInfo)
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
}
