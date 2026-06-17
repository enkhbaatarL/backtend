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
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.repository.master.tmd.projection.QScenarioPriceRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseDetailRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseListRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioPriceRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioRunStatusRow;
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
                scenarioCase.caseId.eq(latestCompletedCaseId()),
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
    public List<ScenarioRunStatusRow> findLatestRunStatusByScenarioIds(List<Long> scenarioIds) {
        if (scenarioIds == null || scenarioIds.isEmpty()) {
            return List.of();
        }
        // 시나리오별 최신 케이스 → last_run_id → 실행 상태(exec_stat_cd)
        return queryFactory
            .select(Projections.constructor(
                ScenarioRunStatusRow.class,
                scenarioCase.scenarioId,
                scenarioCaseRunHistory.status
            ))
            .from(scenarioCase)
            .innerJoin(scenarioCaseRunHistory).on(scenarioCaseRunHistory.runId.eq(scenarioCase.lastRunId))
            .where(
                scenarioCase.scenarioId.in(scenarioIds),
                scenarioCase.isDeleted.isFalse(),
                scenarioCase.caseId.eq(latestCaseId()),
                scenarioCaseRunHistory.isDeleted.isFalse()
            )
            .fetch();
    }

    /** 시나리오의 최신 케이스 ID (재실행 포함 가장 최근 케이스, 삭제 제외) */
    private JPQLSubQuery<Long> latestCaseId() {
        QScenarioCase latestCase = new QScenarioCase("latestCaseForStatus");
        return JPAExpressions
            .select(latestCase.caseId.max())
            .from(latestCase)
            .where(
                latestCase.scenarioId.eq(scenarioCase.scenarioId),
                latestCase.isDeleted.isFalse()
            );
    }

    /** 시나리오의 케이스 중 '케이스·마지막 실행이 모두 완료'된 최신 케이스 ID (재실행 실패/진행 중이면 직전 완료 케이스로 폴백) */
    private JPQLSubQuery<Long> latestCompletedCaseId() {
        QScenarioCase latestCase = new QScenarioCase("latestCase");
        QScenarioCaseRunHistory latestRun = new QScenarioCaseRunHistory("latestRun");

        return JPAExpressions
            .select(latestCase.caseId.max())
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
