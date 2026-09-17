package com.sk.domain.shared.repository.master.tmd.custom;

import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseSearchType;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.repository.master.tmd.projection.MiBasePriceRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseDetailRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseListRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioLatestRunRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioPriceRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioRunStateRow;
import java.util.List;
import java.util.Optional;

public interface CustomScenarioCaseRepository {

    List<ScenarioCaseListRow> findListRows(FundamentalModelCaseSearchType searchType, String keyword);

    Optional<ScenarioCaseListRow> findCurrentMiBaseListRow();

    Optional<ScenarioCaseListRow> findListRowByCaseId(Long caseId);

    Optional<ScenarioCaseDetailRow> findDetailRow(Long caseId);

    List<ScenarioPriceRow> findLatestCompletedPricesByScenarioIds(List<Long> scenarioIds, IndexName indexName, PeriodUnitSeparateCd periodUnit);

    List<ScenarioRunStateRow> findScenarioRunStates(List<Long> scenarioIds);

    /**
     * 시나리오별 최신 완료(케이스·실행 모두 COMPLETED) 실행의 run_id.
     * 가격전망 해석 재실행 stale 판정용 — 결과 scenarioId 집합 = 수급모델 실행 완료(executed) 시나리오.
     */
    List<ScenarioLatestRunRow> findLatestCompletedRunIdByScenarioIds(List<Long> scenarioIds);

    /** current MI Base 기준선 가격 — active generated marker가 Y인 단일 케이스의 가격 결과. */
    List<MiBasePriceRow> findCurrentMiBasePrices(IndexName indexName, PeriodUnitSeparateCd periodUnit);

}
