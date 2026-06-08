package com.sk.domain.shared.repository.master.tmd.custom;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseSearchType;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseDetailRow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseListRow;
import java.util.List;
import java.util.Optional;

public interface CustomScenarioCaseRepository {

    List<ScenarioCaseListRow> findListRows(FundamentalModelCaseSearchType searchType, String keyword);

    Optional<ScenarioCaseDetailRow> findDetailRow(Long caseId);
}
