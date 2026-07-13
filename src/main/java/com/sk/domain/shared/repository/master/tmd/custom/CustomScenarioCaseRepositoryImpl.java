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
import com.sk.domain.shared.repository.master.tmd.projection.MiBasePriceRow;
import com.sk.domain.shared.repository.master.tmd.projection.QMiBasePriceRow;
import com.sk.domain.shared.repository.master.tmd.projection.QScenarioLatestRunRow;
import com.sk.domain.shared.repository.master.tmd.projection.QScenarioPriceRow;
import com.sk.domain.shared.repository.master.tmd.projection.QScenarioRunStateRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseDetailRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseListRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioLatestRunRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioPriceRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioRunStateRow;
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
                scenarioCaseRunPrice.priceRegionName.equalsIgnoreCase(indexName.getPriceRegionName()),
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
    public List<ScenarioRunStateRow> findScenarioRunStates(List<Long> scenarioIds) {

        if (scenarioIds == null || scenarioIds.isEmpty()) {
            return List.of();
        }
        return queryFactory
            .select(new QScenarioRunStateRow(
                scenarioCase.scenarioId,
                scenarioCase.status,
                scenarioCaseRunHistory.status
            ))
            .from(scenarioCase)
            .innerJoin(scenarioCaseRunHistory).on(scenarioCaseRunHistory.runId.eq(scenarioCase.lastRunId))
            .where(
                scenarioCase.scenarioId.in(scenarioIds),
                scenarioCase.isDeleted.isFalse(),
                scenarioCaseRunHistory.isDeleted.isFalse()
            )
            .fetch();
    }

    @Override
    public List<MiBasePriceRow> findLatestMiBasePrices(IndexName indexName, PeriodUnitSeparateCd periodUnit) {
        return queryFactory
            .select(new QMiBasePriceRow(
                scenarioCaseRunPrice.periodKey,
                scenarioCaseRunPrice.baseCasePrice,
                scenarioCaseRunPrice.userAdjustedPrice
            ))
            .from(scenarioCase)
            .innerJoin(scenarioCaseRunHistory).on(scenarioCaseRunHistory.runId.eq(scenarioCase.lastRunId))
            .innerJoin(scenarioCaseRunPrice).on(scenarioCaseRunPrice.runSeq.eq(scenarioCaseRunHistory.runSeq))
            .where(
                scenarioCase.caseId.eq(latestMiBaseCaseId()),
                scenarioCaseRunPrice.priceRegionName.equalsIgnoreCase(indexName.getPriceRegionName()),
                scenarioCaseRunPrice.periodUnit.eq(periodUnit),
                scenarioCaseRunPrice.isDeleted.isFalse()
            )
            .orderBy(scenarioCaseRunPrice.periodKey.asc())
            .fetch();
    }

    @Override
    public List<ScenarioLatestRunRow> findLatestCompletedRunIdByScenarioIds(List<Long> scenarioIds) {

        if (scenarioIds == null || scenarioIds.isEmpty()) {
            return List.of();
        }
        return queryFactory
            .select(new QScenarioLatestRunRow(
                scenarioCase.scenarioId,
                scenarioCaseRunHistory.runId
            ))
            .from(scenarioCase)
            .innerJoin(scenarioCaseRunHistory).on(scenarioCaseRunHistory.runId.eq(scenarioCase.lastRunId))
            .where(
                scenarioCase.scenarioId.in(scenarioIds),
                scenarioCase.isDeleted.isFalse(),
                scenarioCaseRunHistory.isDeleted.isFalse(),
                scenarioCaseRunHistory.runSeq.eq(latestCompletedRunSeq())
            )
            .fetch();
    }

    /**
     * exec_type_cd=MI_BASE 케이스 중 최신 1건의 case_id (case_id 최대).
     * <p>보통 1건이지만 새 baseline이 새 세트로 INSERT되므로 최신 케이스를 선택한다.
     */
    private JPQLSubQuery<Long> latestMiBaseCaseId() {
        QScenarioCase miBaseCase = new QScenarioCase("miBaseCase");
        return JPAExpressions
            .select(miBaseCase.caseId.max())
            .from(miBaseCase)
            .where(
                miBaseCase.execType.eq(FundamentalModelExecType.MI_BASE),
                miBaseCase.isDeleted.isFalse()
            );
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
