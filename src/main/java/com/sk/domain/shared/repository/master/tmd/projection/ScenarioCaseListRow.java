package com.sk.domain.shared.repository.master.tmd.projection;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import java.time.LocalDateTime;

public record ScenarioCaseListRow(
    Long caseId,
    String title,
    FundamentalModelCaseStatus status,
    String assigneeName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
