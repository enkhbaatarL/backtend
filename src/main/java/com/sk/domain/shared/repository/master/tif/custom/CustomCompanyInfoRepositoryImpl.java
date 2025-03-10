package com.sk.domain.shared.repository.master.tif.custom;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.response.CompanyInfoRes;
import com.sk.domain.admin.dto.response.QCompanyInfoRes;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.sk.domain.shared.entity.master.tif.QCompanyInfo.companyInfo;

@RequiredArgsConstructor
public class CustomCompanyInfoRepositoryImpl implements CustomCompanyInfoRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CompanyInfoRes> findCompaniesInfoByConditions(int page, int size) {

        Pageable pageable = PageRequest.of(page - 1, size);

        List<CompanyInfoRes> content = queryFactory
            .select(new QCompanyInfoRes(
                companyInfo.companyNm,
                companyInfo.companyEngNm,
                companyInfo.createUserId,
                companyInfo.createDt,
                companyInfo.modifyUserId,
                companyInfo.modifyDt
            ))
            .from(companyInfo)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy()
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(companyInfo.count())
            .from(companyInfo);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

    }

}
