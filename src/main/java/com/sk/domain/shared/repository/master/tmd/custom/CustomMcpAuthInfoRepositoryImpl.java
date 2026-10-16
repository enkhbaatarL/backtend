package com.sk.domain.shared.repository.master.tmd.custom;

import static com.sk.domain.shared.entity.master.tmd.QCustomerCompany.customerCompany;
import static com.sk.domain.shared.entity.master.tmd.QMcpAuthInfo.mcpAuthInfo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.admin.dto.response.mcp.McpAuthInfoRes;
import com.sk.domain.admin.dto.response.mcp.QMcpAuthInfoRes;
import com.sk.domain.shared.entity.master.tmd.QCustomerCompany;
import com.sk.domain.shared.entity.master.tmd.QUser;
import com.sk.domain.shared.enums.UserRoleCd;

import lombok.RequiredArgsConstructor;

/** [MCP 인증] MCP 인증 키 목록 조회. ClientAccessInfo 목록 조회와 같은 구조. */
@RequiredArgsConstructor
public class CustomMcpAuthInfoRepositoryImpl implements CustomMcpAuthInfoRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<McpAuthInfoRes> findMcpAuthInfoByConditions(int page, int size) {

        Pageable pageable = PageRequest.of(page - 1, size);

        // User 테이블 별칭
        QUser creator = new QUser("creator");
        QUser modifier = new QUser("modifier");

        // Company 테이블 별칭
        QCustomerCompany creatorCompany = new QCustomerCompany("creatorCompany");
        QCustomerCompany modifierCompany = new QCustomerCompany("modifierCompany");

        List<McpAuthInfoRes> content = queryFactory.select(
                new QMcpAuthInfoRes(
                    mcpAuthInfo,
                    creator.userNm,
                    creator.departmentNm,
                    creatorCompany.customerCompanyNm,
                    modifier.userNm,
                    modifier.departmentNm,
                    modifierCompany.customerCompanyNm
                ))
            .from(mcpAuthInfo)
            .leftJoin(mcpAuthInfo.customerCompany, customerCompany).fetchJoin()
            .leftJoin(creator).on(mcpAuthInfo.createUserId.eq(creator.userLoginId).and(creator.userRoleCd.in(UserRoleCd.ADMIN).and(creator.isDeleted.isFalse())))
            .leftJoin(creator.customerCompany, creatorCompany)
            .leftJoin(modifier).on(mcpAuthInfo.modifyUserId.eq(modifier.userLoginId).and(modifier.userRoleCd.in(UserRoleCd.ADMIN).and(modifier.isDeleted.isFalse())))
            .leftJoin(modifier.customerCompany, modifierCompany)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .where(mcpAuthInfo.isDeleted.eq(false))
            .orderBy(mcpAuthInfo.createDt.desc())
            .fetch();

        JPQLQuery<Long> countQuery = queryFactory
            .select(mcpAuthInfo.count())
            .from(mcpAuthInfo)
            .where(mcpAuthInfo.isDeleted.eq(false));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

}
