package com.sk.domain.shared.repository.master.tif.custom;

import static com.sk.domain.shared.entity.master.tif.QClientAccessInfo.clientAccessInfo;
import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.response.client.ClientAccessInfoRes;
import com.sk.domain.admin.dto.response.client.QClientAccessInfoRes;
import com.sk.domain.shared.entity.master.tmd.QCustomerCompany;
import com.sk.domain.shared.entity.master.tmd.QUser;
import com.sk.domain.shared.enums.UserRoleCd;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomClientAccessInfoRepositoryImpl implements CustomClientAccessInfoRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ClientAccessInfoRes> findClientInfoByConditions(int page, int size) {

        Pageable pageable = PageRequest.of(page - 1, size);

        // User 테이블 별칭
        QUser creator = new QUser("creator");
        QUser modifier = new QUser("modifier");

        // Company 테이블 별칭
        QCustomerCompany creatorCompany = new QCustomerCompany("creatorCompany");
        QCustomerCompany modifierCompany = new QCustomerCompany("modifierCompany");

        List<ClientAccessInfoRes> content = queryFactory.select(
                new QClientAccessInfoRes(
                    clientAccessInfo,
                    creator.userNm,
                    creator.departmentNm,
                    creatorCompany.customerCompanyNm,
                    modifier.userNm,
                    modifier.departmentNm,
                    modifierCompany.customerCompanyNm
                ))
            .from(clientAccessInfo)
            .leftJoin(clientAccessInfo.customerCompany, customerCompany).fetchJoin()
            .leftJoin(creator).on(clientAccessInfo.createUserId.eq(creator.userLoginId).and(creator.userRoleCd.in(UserRoleCd.ADMIN).and(creator.isDeleted.isFalse())))
            .leftJoin(creator.customerCompany, creatorCompany)
            .leftJoin(modifier).on(clientAccessInfo.modifyUserId.eq(modifier.userLoginId).and(modifier.userRoleCd.in(UserRoleCd.ADMIN).and(modifier.isDeleted.isFalse())))
            .leftJoin(modifier.customerCompany, modifierCompany)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .where(clientAccessInfo.isDeleted.eq(false))
            .orderBy(clientAccessInfo.createDt.desc())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(clientAccessInfo.count())
            .from(clientAccessInfo)
            .where(clientAccessInfo.isDeleted.eq(false));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

}
