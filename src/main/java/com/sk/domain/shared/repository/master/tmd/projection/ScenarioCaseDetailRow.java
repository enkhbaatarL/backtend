package com.sk.domain.shared.repository.master.tmd.projection;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import java.time.LocalDateTime;

public record ScenarioCaseDetailRow(
    Long caseId,
    String title,
    String description,
    FundamentalModelCaseStatus status,
    String assigneeName,
    Integer assigneeUserNo,
    String lastRunId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
