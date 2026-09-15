package com.sk.domain.shared.repository.master.tmd.projection;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import java.time.LocalDateTime;

public record ScenarioCaseListRow(
    Long caseId,
    String title,
    FundamentalModelCaseStatus status,
    FundamentalModelExecType execType,
    String assigneeName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
