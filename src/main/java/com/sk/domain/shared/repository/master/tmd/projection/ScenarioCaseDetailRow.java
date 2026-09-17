package com.sk.domain.shared.repository.master.tmd.projection;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import java.time.LocalDateTime;

public record ScenarioCaseDetailRow(
    Long caseId,
    String title,
    String description,
    FundamentalModelCaseStatus status,
    FundamentalModelExecType execType,
    String assigneeName,
    Integer assigneeUserNo,
    String lastRunId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
