package com.sk.domain.shared.repository.master.tmd.custom;

import static com.sk.domain.shared.entity.master.tmd.QScenarioCase.scenarioCase;
import static com.sk.domain.shared.entity.master.tmd.QUser.user;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseSearchType;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseDetailRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseListRow;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomScenarioCaseRepositoryImpl implements CustomScenarioCaseRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ScenarioCaseListRow> findListRows(FundamentalModelCaseSearchType searchType, String keyword) {
        BooleanBuilder builder = new BooleanBuilder()
            .and(scenarioCase.isDeleted.isFalse());

        if (keyword != null && searchType != null) {
            String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
            if (searchType == FundamentalModelCaseSearchType.TITLE) {
                builder.and(scenarioCase.title.lower().contains(normalizedKeyword));
            }
            if (searchType == FundamentalModelCaseSearchType.ASSIGNEE_NAME) {
                builder.and(user.userNm.lower().contains(normalizedKeyword));
            }
        }

        return queryFactory
            .select(Projections.constructor(
                ScenarioCaseListRow.class,
                scenarioCase.caseId,
                scenarioCase.title,
                scenarioCase.status,
                user.userNm,
                scenarioCase.createDt,
                scenarioCase.modifyDt
            ))
            .from(scenarioCase)
            .join(user).on(scenarioCase.userNo.eq(user.userNo))
            .where(builder)
            .orderBy(scenarioCase.createDt.desc(), scenarioCase.caseId.desc())
            .fetch();
    }

    @Override
    public Optional<ScenarioCaseDetailRow> findDetailRow(Long caseId) {
        return Optional.ofNullable(queryFactory
            .select(Projections.constructor(
                ScenarioCaseDetailRow.class,
                scenarioCase.caseId,
                scenarioCase.title,
                scenarioCase.description,
                scenarioCase.status,
                user.userNm,
                scenarioCase.userNo,
                scenarioCase.lastRunId,
                scenarioCase.createDt,
                scenarioCase.modifyDt
            ))
            .from(scenarioCase)
            .join(user).on(scenarioCase.userNo.eq(user.userNo))
            .where(
                scenarioCase.caseId.eq(caseId),
                scenarioCase.isDeleted.isFalse()
            )
            .fetchOne());
    }
}
