package com.sk.domain.shared.repository.master.tmd.custom;

import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseSearchType;
import com.sk.domain.shared.enums.nextgen.IndexName;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseDetailRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseListRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioPriceRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioRunStatusRow;
import java.util.List;
import java.util.Optional;

public interface CustomScenarioCaseRepository {

    List<ScenarioCaseListRow> findListRows(FundamentalModelCaseSearchType searchType, String keyword);

    Optional<ScenarioCaseDetailRow> findDetailRow(Long caseId);

    List<ScenarioPriceRow> findLatestCompletedPricesByScenarioIds(List<Long> scenarioIds, IndexName indexName, PeriodUnitSeparateCd periodUnit);

    /**
     * 시나리오별 최신 케이스의 최신 실행 상태 (last_run_id → tmd_mdl_scnrcaseh.exec_stat_cd).
     * 그래프 executed(=COMPLETED)/modelRunning(=RUNNING) 판정용. 케이스/실행 없으면 해당 시나리오는 결과에서 빠진다.
     */
    List<ScenarioRunStatusRow> findLatestRunStatusByScenarioIds(List<Long> scenarioIds);

}
