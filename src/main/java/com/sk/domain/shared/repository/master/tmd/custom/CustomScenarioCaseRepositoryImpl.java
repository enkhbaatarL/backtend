package com.sk.domain.shared.repository.master.tmd.custom;

import static com.sk.domain.shared.entity.master.tmd.QScenarioCase.scenarioCase;
import static com.sk.domain.shared.entity.master.tmd.QScenarioCaseRunHistory.scenarioCaseRunHistory;
import static com.sk.domain.shared.entity.master.tmd.QScenarioCaseRunPrice.scenarioCaseRunPrice;
import static com.sk.domain.shared.entity.master.tmd.QUser.user;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLSubQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.shared.entity.master.tmd.QScenarioCase;
import com.sk.domain.shared.entity.master.tmd.QScenarioCaseRunHistory;
import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseSearchType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.repository.master.tmd.projection.QScenarioPriceRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseDetailRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseListRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioPriceRow;
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
            .and(scenarioCase.isDeleted.isFalse())
            .and(scenarioCase.execType.eq(FundamentalModelExecType.INDEPENDENT));

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
                scenarioCase.isDeleted.isFalse(),
                scenarioCase.execType.eq(FundamentalModelExecType.INDEPENDENT)
            )
            .fetchOne());
    }

    @Override
    public List<ScenarioPriceRow> findLatestCompletedPricesByScenarioIds(List<Long> scenarioIds, IndexName indexName, PeriodUnitSeparateCd periodUnit) {

        if (scenarioIds == null || scenarioIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
            .select(new QScenarioPriceRow(
                scenarioCase.scenarioId,
                scenarioCaseRunPrice.periodKey,
                scenarioCaseRunPrice.baseCasePrice,
                scenarioCaseRunPrice.userAdjustedPrice
            ))
            .from(scenarioCase)
            .innerJoin(scenarioCaseRunHistory).on(scenarioCaseRunHistory.runId.eq(scenarioCase.lastRunId))
            .innerJoin(scenarioCaseRunPrice).on(scenarioCaseRunPrice.runSeq.eq(scenarioCaseRunHistory.runSeq))
            .where(
                scenarioCase.scenarioId.in(scenarioIds),
                scenarioCaseRunHistory.runSeq.eq(latestCompletedRunSeq()),
                scenarioCaseRunPrice.priceRegionName.equalsIgnoreCase(indexName.getCode()),
                scenarioCaseRunPrice.periodUnit.eq(periodUnit),
                scenarioCaseRunPrice.isDeleted.isFalse()
            )
            .orderBy(
                scenarioCase.scenarioId.asc(),
                scenarioCaseRunPrice.periodKey.asc()
            )
            .fetch();
    }

    @Override
    public List<Long> findRunningScenarioIds(List<Long> scenarioIds) {
        if (scenarioIds == null || scenarioIds.isEmpty()) {
            return List.of();
        }
        return queryFactory
            .select(scenarioCase.scenarioId)
            .distinct()
            .from(scenarioCase)
            .innerJoin(scenarioCaseRunHistory).on(scenarioCaseRunHistory.runId.eq(scenarioCase.lastRunId))
            .where(
                scenarioCase.scenarioId.in(scenarioIds),
                scenarioCase.isDeleted.isFalse(),
                scenarioCaseRunHistory.isDeleted.isFalse(),
                scenarioCaseRunHistory.status.eq(FundamentalModelRunStatus.RUNNING)
            )
            .fetch();
    }

    /**
     * 시나리오의 '케이스·마지막 실행이 모두 완료'된 실행 중 최신(run_seq 최대) 실행순번.
     * <p>"최신"은 전역 증가값인 run_seq DESC 기준 (재실행 실패/진행 중이면 직전 완료 실행으로 폴백).
     */
    private JPQLSubQuery<Long> latestCompletedRunSeq() {
        QScenarioCase latestCase = new QScenarioCase("latestCase");
        QScenarioCaseRunHistory latestRun = new QScenarioCaseRunHistory("latestRun");

        return JPAExpressions
            .select(latestRun.runSeq.max())
            .from(latestCase)
            .innerJoin(latestRun).on(latestRun.runId.eq(latestCase.lastRunId))
            .where(
                latestCase.scenarioId.eq(scenarioCase.scenarioId),
                latestCase.isDeleted.isFalse(),
                latestCase.status.eq(FundamentalModelCaseStatus.COMPLETED),
                latestRun.status.eq(FundamentalModelRunStatus.COMPLETED),
                latestRun.isDeleted.isFalse()
            );
    }
}
